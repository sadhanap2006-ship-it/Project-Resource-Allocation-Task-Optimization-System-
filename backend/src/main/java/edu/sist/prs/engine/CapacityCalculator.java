package edu.sist.prs.engine;

import edu.sist.prs.engine.model.AllocationSnapshot;
import edu.sist.prs.engine.model.OverAllocationPeriod;
import edu.sist.prs.engine.model.ResourceSnapshot;
import edu.sist.prs.engine.model.UnavailabilitySnapshot;
import edu.sist.prs.entity.ConflictSeverity;
import edu.sist.prs.entity.MilestoneStatus;
import edu.sist.prs.entity.ProjectStatus;
import edu.sist.prs.entity.ResourceStatus;
import edu.sist.prs.entity.ResourceType;
import edu.sist.prs.entity.UnavailabilityStatus;
import edu.sist.prs.util.WorkingDays;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Pure calculation engine for capacity, load, remaining availability,
 * and over-allocation period detection across all resource types.
 * Adheres to Rules R3, R4, R9 and Section 9.1.
 */
public final class CapacityCalculator {

    private CapacityCalculator() {}

    /**
     * Rule R3: capacity(r, d)
     * = 0 if r is INACTIVE, or d is inside an APPROVED unavailability of r,
     * or (for DEVELOPER only) d is Saturday or Sunday.
     * Otherwise capacity(r, d) = r.capacity_pct.
     * HARDWARE and CLOUD use calendar days (no weekend rule).
     */
    public static int calculateCapacity(
            ResourceSnapshot resource,
            LocalDate date,
            Collection<UnavailabilitySnapshot> unavailabilities
    ) {
        if (resource.status() == ResourceStatus.INACTIVE) {
            return 0;
        }

        if (isInsideApprovedUnavailability(resource.id(), date, unavailabilities)) {
            return 0;
        }

        if (resource.type() == ResourceType.DEVELOPER && WorkingDays.isWeekend(date)) {
            return 0;
        }

        return resource.capacityPct();
    }

    /**
     * Rule R9: Completed work.
     * Allocations whose milestone status is COMPLETED, or whose project status is COMPLETED
     * or ARCHIVED, are ignored by load, conflicts and forecasts.
     */
    public static boolean isExcludedByRuleR9(AllocationSnapshot allocation) {
        if (allocation.milestoneStatus() == MilestoneStatus.COMPLETED) {
            return true;
        }
        return allocation.projectStatus() == ProjectStatus.COMPLETED || allocation.projectStatus() == ProjectStatus.ARCHIVED;
    }

    /**
     * Section 9.1: load(r, d)
     * = sum of allocation_pct of allocations a of r where a.start <= d <= a.end,
     * excluding Rule R9 allocations.
     */
    public static int calculateLoad(
            Long resourceId,
            LocalDate date,
            Collection<AllocationSnapshot> allocations
    ) {
        int totalLoad = 0;
        for (AllocationSnapshot a : allocations) {
            if (!a.resourceId().equals(resourceId)) {
                continue;
            }
            if (isExcludedByRuleR9(a)) {
                continue;
            }
            if (!date.isBefore(a.startDate()) && !date.isAfter(a.endDate())) {
                totalLoad += a.allocationPct();
            }
        }
        return totalLoad;
    }

    /**
     * remaining(r, d) = capacity(r, d) - load(r, d) (may be negative).
     */
    public static int calculateRemaining(
            ResourceSnapshot resource,
            LocalDate date,
            Collection<AllocationSnapshot> allocations,
            Collection<UnavailabilitySnapshot> unavailabilities
    ) {
        int cap = calculateCapacity(resource, date, unavailabilities);
        int load = calculateLoad(resource.id(), date, allocations);
        return cap - load;
    }

    /**
     * Peak load over a given window [startDate, endDate] inclusive.
     */
    public static int calculatePeakLoad(
            Long resourceId,
            LocalDate startDate,
            LocalDate endDate,
            Collection<AllocationSnapshot> allocations
    ) {
        int peak = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            int load = calculateLoad(resourceId, current, allocations);
            if (load > peak) {
                peak = load;
            }
            current = current.plusDays(1);
        }
        return peak;
    }

    /**
     * Peak load over next 30 days [today, today + 29].
     */
    public static int calculatePeakLoad30Days(
            Long resourceId,
            LocalDate today,
            Collection<AllocationSnapshot> allocations
    ) {
        return calculatePeakLoad(resourceId, today, today.plusDays(29), allocations);
    }

    /**
     * Rule R4: Evaluated only on "available days": days where r is ACTIVE,
     * not inside APPROVED unavailability, and (for DEVELOPER) not a weekend.
     */
    public static boolean isAvailableDay(
            ResourceSnapshot resource,
            LocalDate date,
            Collection<UnavailabilitySnapshot> unavailabilities
    ) {
        if (resource.status() != ResourceStatus.ACTIVE) {
            return false;
        }
        if (isInsideApprovedUnavailability(resource.id(), date, unavailabilities)) {
            return false;
        }
        if (resource.type() == ResourceType.DEVELOPER && WorkingDays.isWeekend(date)) {
            return false;
        }
        return true;
    }

    /**
     * Checks if date is inside an APPROVED unavailability for resource.
     */
    public static boolean isInsideApprovedUnavailability(
            Long resourceId,
            LocalDate date,
            Collection<UnavailabilitySnapshot> unavailabilities
    ) {
        if (unavailabilities == null) {
            return false;
        }
        for (UnavailabilitySnapshot u : unavailabilities) {
            if (u.resourceId().equals(resourceId) && u.status() == UnavailabilityStatus.APPROVED) {
                if (!date.isBefore(u.startDate()) && !date.isAfter(u.endDate())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Rule R4 & Section 9.1:
     * Over-allocated available days are merged into one period when no available,
     * non-over-allocated day lies between them (weekends and leave days do not break a period,
     * but they are not counted in it).
     * Period start = first over-allocated day, end = last, peak_value = max load in the period.
     * Severity: CRITICAL if peak > 100, WARNING if capacity < peak <= 100.
     * Note: load == capacity == 100 is NOT a conflict.
     */
    public static List<OverAllocationPeriod> findOverAllocationPeriods(
            ResourceSnapshot resource,
            LocalDate windowStart,
            LocalDate windowEnd,
            Collection<AllocationSnapshot> allocations,
            Collection<UnavailabilitySnapshot> unavailabilities
    ) {
        List<OverAllocationPeriod> periods = new ArrayList<>();
        if (resource.status() == ResourceStatus.INACTIVE) {
            // Rule R4: Load on an INACTIVE resource is reported ONLY as INACTIVE_RESOURCE.
            return periods;
        }

        LocalDate currentPeriodStart = null;
        LocalDate currentPeriodEnd = null;
        int currentPeak = 0;

        LocalDate current = windowStart;
        while (!current.isAfter(windowEnd)) {
            boolean available = isAvailableDay(resource, current, unavailabilities);

            if (available) {
                int load = calculateLoad(resource.id(), current, allocations);
                int cap = resource.capacityPct(); // On available days, capacity is resource.capacityPct

                if (load > cap) {
                    if (currentPeriodStart == null) {
                        currentPeriodStart = current;
                        currentPeriodEnd = current;
                        currentPeak = load;
                    } else {
                        currentPeriodEnd = current;
                        if (load > currentPeak) {
                            currentPeak = load;
                        }
                    }
                } else {
                    // Available day with load <= cap breaks the over-allocation streak
                    if (currentPeriodStart != null) {
                        ConflictSeverity severity = currentPeak > 100
                                ? ConflictSeverity.CRITICAL
                                : ConflictSeverity.WARNING;
                        periods.add(new OverAllocationPeriod(
                                resource.id(),
                                currentPeriodStart,
                                currentPeriodEnd,
                                currentPeak,
                                severity
                        ));
                        currentPeriodStart = null;
                        currentPeriodEnd = null;
                        currentPeak = 0;
                    }
                }
            }
            // Unavailable days (weekends, approved leave) are skipped without breaking or extending the period

            current = current.plusDays(1);
        }

        // Close any trailing period
        if (currentPeriodStart != null) {
            ConflictSeverity severity = currentPeak > 100
                    ? ConflictSeverity.CRITICAL
                    : ConflictSeverity.WARNING;
            periods.add(new OverAllocationPeriod(
                    resource.id(),
                    currentPeriodStart,
                    currentPeriodEnd,
                    currentPeak,
                    severity
            ));
        }

        return periods;
    }
}
