package edu.sist.prs.dto.response;

import java.time.LocalDate;
import java.util.List;

public record ResourceTimelineResponse(
        List<ResourceTimelineLane> resources
) {
    public record ResourceTimelineLane(
            Long resourceId,
            String resourceName,
            List<TimelineAllocationBar> allocations,
            List<TimelineConflictBand> conflictBands
    ) {}

    public record TimelineAllocationBar(
            Long allocationId,
            Long projectId,
            String projectName,
            Long milestoneId,
            String milestoneName,
            int allocationPct,
            LocalDate startDate,
            LocalDate endDate
    ) {}

    public record TimelineConflictBand(
            Long conflictId,
            String type,
            String severity,
            String description,
            LocalDate startDate,
            LocalDate endDate
    ) {}
}
