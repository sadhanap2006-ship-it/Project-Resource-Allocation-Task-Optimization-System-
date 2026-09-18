package com.sist.rms.service;

import com.sist.rms.dto.ConflictResponse;
import com.sist.rms.entity.Developer;
import com.sist.rms.entity.DeveloperLeave;
import com.sist.rms.entity.LeaveStatus;
import com.sist.rms.entity.ResourceAllocation;
import com.sist.rms.repository.DeveloperLeaveRepository;
import com.sist.rms.repository.DeveloperRepository;
import com.sist.rms.repository.ResourceAllocationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 * CONFLICT DETECTION
 *
 * 1) OVER-ALLOCATION
 *    Simply adding up all of a developer's allocations would be wrong,
 *    because work done in January does not compete with work done in March.
 *    So we use a "timeline sweep":
 *
 *      step 1  collect every boundary date: each start date, and each
 *              end date + 1 day
 *      step 2  sort them. Between two consecutive boundaries the set of
 *              running allocations never changes - we call that a SEGMENT
 *      step 3  for each segment, add up the allocations that are running
 *      step 4  if the total is greater than the developer's availability,
 *              that segment is an OVER-ALLOCATION CONFLICT
 *      step 5  merge touching segments that have the same total, so the
 *              message reads as one date range
 *
 * 2) LEAVE CONFLICT
 *    During approved leave the effective availability is 0 %. So any
 *    allocation whose dates overlap a leave period is a LEAVE CONFLICT.
 */
@Service
public class ConflictService {

    private static final DateTimeFormatter NICE_DATE = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    private final DeveloperRepository developerRepository;
    private final ResourceAllocationRepository allocationRepository;
    private final DeveloperLeaveRepository leaveRepository;

    public ConflictService(DeveloperRepository developerRepository,
                           ResourceAllocationRepository allocationRepository,
                           DeveloperLeaveRepository leaveRepository) {
        this.developerRepository = developerRepository;
        this.allocationRepository = allocationRepository;
        this.leaveRepository = leaveRepository;
    }

    // -----------------------------------------------------------------
    //  Public entry points
    // -----------------------------------------------------------------

    /** Every conflict of every developer. */
    public List<ConflictResponse> findAllConflicts() {
        List<ConflictResponse> conflicts = new ArrayList<>();
        for (Developer developer : developerRepository.findAllByOrderByNameAsc()) {
            conflicts.addAll(findConflictsForDeveloper(developer.getId()));
        }
        return conflicts;
    }

    /** All conflicts of one developer (over-allocation first, then leave). */
    public List<ConflictResponse> findConflictsForDeveloper(Long developerId) {
        Developer developer = developerRepository.findById(developerId).orElse(null);
        if (developer == null) {
            return new ArrayList<>();
        }
        List<ResourceAllocation> allocations = allocationRepository.findByDeveloperId(developerId);
        List<DeveloperLeave> leaves = leaveRepository.findByDeveloperId(developerId);

        List<ConflictResponse> conflicts = new ArrayList<>();
        conflicts.addAll(detectOverAllocation(developer, allocations));
        conflicts.addAll(detectLeaveConflicts(developer, allocations, leaves));
        return conflicts;
    }

    // -----------------------------------------------------------------
    //  1) OVER-ALLOCATION  (the timeline sweep)
    // -----------------------------------------------------------------

    /**
     * Pure calculation - it takes plain lists and touches no database,
     * which is exactly why it is easy to unit test.
     */
    public List<ConflictResponse> detectOverAllocation(Developer developer, List<ResourceAllocation> allocations) {
        List<ConflictResponse> conflicts = new ArrayList<>();
        if (developer == null || allocations == null || allocations.isEmpty()) {
            return conflicts;
        }

        int availability = developer.getAvailabilityPercentage() == null
                ? 100 : developer.getAvailabilityPercentage();

        // step 1 + 2 : every boundary date, sorted and without duplicates
        TreeSet<LocalDate> boundaries = new TreeSet<>();
        for (ResourceAllocation allocation : allocations) {
            boundaries.add(allocation.getStartDate());
            boundaries.add(allocation.getEndDate().plusDays(1));
        }
        List<LocalDate> points = new ArrayList<>(boundaries);

        // step 3 + 4 : build the segments and add up what is running in each
        List<Segment> overloaded = new ArrayList<>();
        for (int i = 0; i < points.size() - 1; i++) {
            LocalDate segmentStart = points.get(i);
            LocalDate segmentEnd = points.get(i + 1).minusDays(1);
            if (segmentEnd.isBefore(segmentStart)) {
                continue;
            }

            int total = 0;
            List<ResourceAllocation> running = new ArrayList<>();
            for (ResourceAllocation allocation : allocations) {
                if (allocation.overlaps(segmentStart, segmentEnd)) {
                    total += allocation.getAllocationPercentage();
                    running.add(allocation);
                }
            }

            if (total > availability) {
                overloaded.add(new Segment(segmentStart, segmentEnd, total, running));
            }
        }

        // step 5 : merge touching segments that carry the same total
        for (Segment segment : mergeSegments(overloaded)) {
            conflicts.add(buildOverAllocationConflict(developer, availability, segment));
        }
        return conflicts;
    }

    /** Joins segments that touch each other and have the same total percentage. */
    private List<Segment> mergeSegments(List<Segment> segments) {
        List<Segment> merged = new ArrayList<>();
        segments.sort(Comparator.comparing(s -> s.start));
        for (Segment segment : segments) {
            if (!merged.isEmpty()) {
                Segment previous = merged.get(merged.size() - 1);
                boolean touching = previous.end.plusDays(1).equals(segment.start);
                if (touching && previous.total == segment.total) {
                    previous.end = segment.end;
                    continue;
                }
            }
            merged.add(new Segment(segment.start, segment.end, segment.total, segment.running));
        }
        return merged;
    }

    private ConflictResponse buildOverAllocationConflict(Developer developer, int availability, Segment segment) {
        ConflictResponse conflict = new ConflictResponse();
        conflict.setType("OVER_ALLOCATION");
        conflict.setSeverity("CRITICAL");
        conflict.setDeveloperId(developer.getId());
        conflict.setDeveloperName(developer.getName());
        conflict.setStartDate(segment.start);
        conflict.setEndDate(segment.end);
        conflict.setTotalAllocation(segment.total);
        conflict.setAvailability(availability);

        List<String> involved = new ArrayList<>();
        for (ResourceAllocation allocation : segment.running) {
            String milestone = allocation.getMilestone() == null
                    ? "whole project" : allocation.getMilestone().getName();
            involved.add(allocation.getProject().getName() + " / " + milestone
                    + " (" + allocation.getAllocationPercentage() + "%)");
        }
        conflict.setInvolvedAllocations(involved);

        conflict.setMessage("CRITICAL: " + developer.getName() + " is allocated at "
                + segment.total + "% between " + segment.start.format(NICE_DATE)
                + " and " + segment.end.format(NICE_DATE)
                + ", but is only " + availability + "% available.");
        return conflict;
    }

    // -----------------------------------------------------------------
    //  2) LEAVE CONFLICT
    // -----------------------------------------------------------------

    /** Pure calculation, same as above - takes lists, touches no database. */
    public List<ConflictResponse> detectLeaveConflicts(Developer developer,
                                                       List<ResourceAllocation> allocations,
                                                       List<DeveloperLeave> leaves) {
        List<ConflictResponse> conflicts = new ArrayList<>();
        if (developer == null || allocations == null || leaves == null) {
            return conflicts;
        }

        for (DeveloperLeave leave : leaves) {
            if (leave.getStatus() == LeaveStatus.CANCELLED) {
                continue;   // cancelled leave does not block anything
            }
            for (ResourceAllocation allocation : allocations) {
                if (!allocation.overlaps(leave.getStartDate(), leave.getEndDate())) {
                    continue;
                }
                // the conflict lasts only for the days the two ranges share
                LocalDate from = allocation.getStartDate().isAfter(leave.getStartDate())
                        ? allocation.getStartDate() : leave.getStartDate();
                LocalDate to = allocation.getEndDate().isBefore(leave.getEndDate())
                        ? allocation.getEndDate() : leave.getEndDate();

                ConflictResponse conflict = new ConflictResponse();
                conflict.setType("LEAVE_CONFLICT");
                conflict.setSeverity("WARNING");
                conflict.setDeveloperId(developer.getId());
                conflict.setDeveloperName(developer.getName());
                conflict.setStartDate(from);
                conflict.setEndDate(to);
                conflict.setTotalAllocation(allocation.getAllocationPercentage());
                conflict.setAvailability(0);
                conflict.getInvolvedAllocations().add(allocation.getProject().getName()
                        + " (" + allocation.getAllocationPercentage() + "%)");
                conflict.setMessage("LEAVE CONFLICT: " + developer.getName() + " is allocated "
                        + allocation.getAllocationPercentage() + "% to " + allocation.getProject().getName()
                        + " between " + from.format(NICE_DATE) + " and " + to.format(NICE_DATE)
                        + ", but is on leave during those dates (effective availability 0%).");
                conflicts.add(conflict);
            }
        }
        return conflicts;
    }

    // -----------------------------------------------------------------
    //  Small helpers reused by the utilization table and the simulator
    // -----------------------------------------------------------------

    /** Total allocation percentage running on one particular day. */
    public int totalAllocationOn(LocalDate date, List<ResourceAllocation> allocations) {
        int total = 0;
        for (ResourceAllocation allocation : allocations) {
            if (allocation.overlaps(date, date)) {
                total += allocation.getAllocationPercentage();
            }
        }
        return total;
    }

    /** The highest simultaneous allocation the developer ever reaches. */
    public int peakAllocation(List<ResourceAllocation> allocations) {
        if (allocations == null || allocations.isEmpty()) {
            return 0;
        }
        TreeSet<LocalDate> boundaries = new TreeSet<>();
        for (ResourceAllocation allocation : allocations) {
            boundaries.add(allocation.getStartDate());
            boundaries.add(allocation.getEndDate().plusDays(1));
        }
        int peak = 0;
        List<LocalDate> points = new ArrayList<>(boundaries);
        for (int i = 0; i < points.size() - 1; i++) {
            LocalDate segmentStart = points.get(i);
            LocalDate segmentEnd = points.get(i + 1).minusDays(1);
            if (segmentEnd.isBefore(segmentStart)) {
                continue;
            }
            int total = 0;
            for (ResourceAllocation allocation : allocations) {
                if (allocation.overlaps(segmentStart, segmentEnd)) {
                    total += allocation.getAllocationPercentage();
                }
            }
            peak = Math.max(peak, total);
        }
        return peak;
    }

    /** True when the developer has non-cancelled leave covering the date. */
    public boolean isOnLeave(LocalDate date, List<DeveloperLeave> leaves) {
        if (leaves == null) {
            return false;
        }
        for (DeveloperLeave leave : leaves) {
            if (leave.getStatus() != LeaveStatus.CANCELLED && leave.overlaps(date, date)) {
                return true;
            }
        }
        return false;
    }

    /** One stretch of time in which the running allocations do not change. */
    private static class Segment {
        private final LocalDate start;
        private LocalDate end;
        private final int total;
        private final List<ResourceAllocation> running;

        Segment(LocalDate start, LocalDate end, int total, List<ResourceAllocation> running) {
            this.start = start;
            this.end = end;
            this.total = total;
            this.running = running;
        }
    }
}
