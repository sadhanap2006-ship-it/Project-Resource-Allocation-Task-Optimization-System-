package edu.sist.prs.dto.response;

import java.time.LocalDate;
import java.util.List;

public record DeveloperDashboardResponse(
        Long resourceId,
        String resourceName,
        double currentLoadPct,
        double peakLoad30DaysPct,
        double remainingCapacityPct,
        List<DevAllocationItem> allocations,
        List<DevMilestoneItem> milestones,
        List<DevWeeklyLoadItem> weeklyLoad8Weeks,
        List<DevLeaveItem> leaveRequests,
        List<DevConflictItem> openConflicts,
        long unreadNotificationsCount
) {
    public record DevAllocationItem(
            Long id,
            Long projectId,
            String projectName,
            Long milestoneId,
            String milestoneName,
            int allocationPct,
            LocalDate startDate,
            LocalDate endDate
    ) {}

    public record DevMilestoneItem(
            Long id,
            Long projectId,
            String projectName,
            String name,
            LocalDate endDate,
            double progressPct,
            String status
    ) {}

    public record DevWeeklyLoadItem(
            String weekLabel,
            LocalDate startDate,
            LocalDate endDate,
            double loadPct
    ) {}

    public record DevLeaveItem(
            Long id,
            LocalDate startDate,
            LocalDate endDate,
            String reason,
            String status
    ) {}

    public record DevConflictItem(
            Long id,
            String type,
            String severity,
            String description,
            LocalDate startDate,
            LocalDate endDate
    ) {}
}
