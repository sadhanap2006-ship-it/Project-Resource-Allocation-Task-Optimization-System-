package com.sist.rms.service;

import com.sist.rms.dto.UtilizationResponse;
import com.sist.rms.entity.Developer;
import com.sist.rms.entity.DeveloperLeave;
import com.sist.rms.entity.ResourceAllocation;
import com.sist.rms.repository.DeveloperLeaveRepository;
import com.sist.rms.repository.DeveloperRepository;
import com.sist.rms.repository.ResourceAllocationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * RESOURCE UTILIZATION TABLE
 *
 *   Developer | Allocation | Availability | Status
 *   Arun      |    60 %    |    100 %     | Available
 *   Priya     |    80 %    |    100 %     | High Load
 *   Rahul     |   110 %    |    100 %     | Conflict
 *
 * "Allocation" is the PEAK simultaneous allocation, so a developer who is
 * free today but double-booked next month still shows up as a conflict.
 *
 * Status rule:
 *   peak >  availability        -> "Conflict"
 *   peak == availability        -> "Fully Allocated"
 *   peak >= 80 % of availability-> "High Load"
 *   otherwise                   -> "Available"
 */
@Service
public class UtilizationService {

    /** At or above this share of their availability a developer is "High Load". */
    public static final double HIGH_LOAD_RATIO = 0.8;

    private final DeveloperRepository developerRepository;
    private final ResourceAllocationRepository allocationRepository;
    private final DeveloperLeaveRepository leaveRepository;
    private final ConflictService conflictService;

    public UtilizationService(DeveloperRepository developerRepository,
                              ResourceAllocationRepository allocationRepository,
                              DeveloperLeaveRepository leaveRepository,
                              ConflictService conflictService) {
        this.developerRepository = developerRepository;
        this.allocationRepository = allocationRepository;
        this.leaveRepository = leaveRepository;
        this.conflictService = conflictService;
    }

    public List<UtilizationResponse> getUtilization() {
        LocalDate today = LocalDate.now();
        List<UtilizationResponse> rows = new ArrayList<>();
        for (Developer developer : developerRepository.findAllByOrderByNameAsc()) {
            rows.add(getUtilizationFor(developer, today));
        }
        return rows;
    }

    public UtilizationResponse getUtilizationFor(Developer developer, LocalDate today) {
        List<ResourceAllocation> allocations = allocationRepository.findByDeveloperId(developer.getId());
        List<DeveloperLeave> leaves = leaveRepository.findByDeveloperId(developer.getId());

        int availability = developer.getAvailabilityPercentage() == null
                ? 100 : developer.getAvailabilityPercentage();
        int peak = conflictService.peakAllocation(allocations);
        int current = conflictService.totalAllocationOn(today, allocations);

        UtilizationResponse row = new UtilizationResponse();
        row.setDeveloperId(developer.getId());
        row.setDeveloperName(developer.getName());
        row.setAvailability(availability);
        row.setPeakAllocation(peak);
        row.setCurrentAllocation(current);
        row.setRemainingCapacity(Math.max(0, availability - peak));
        row.setOnLeaveToday(conflictService.isOnLeave(today, leaves));
        row.setStatus(decideStatus(peak, availability));
        return row;
    }

    /** The single place that decides the colour shown in the table. */
    public String decideStatus(int peakAllocation, int availability) {
        if (peakAllocation > availability) {
            return "Conflict";
        }
        if (peakAllocation == availability && availability > 0) {
            return "Fully Allocated";
        }
        if (availability > 0 && peakAllocation >= availability * HIGH_LOAD_RATIO) {
            return "High Load";
        }
        return "Available";
    }
}
