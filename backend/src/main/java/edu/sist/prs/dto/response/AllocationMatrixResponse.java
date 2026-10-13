package edu.sist.prs.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AllocationMatrixResponse(
        Long projectId,
        String projectName,
        List<MatrixMilestoneHeader> milestones,
        List<MatrixResourceRow> resources
) {
    public record MatrixMilestoneHeader(
            Long id,
            String name,
            String status,
            LocalDate startDate,
            LocalDate endDate
    ) {}

    public record MatrixResourceRow(
            Long resourceId,
            String resourceName,
            String roleType,
            Map<Long, Integer> allocations, // milestoneId -> pct
            int totalAllocatedPct,
            int peakLoadPct,
            String band // LOW, HEALTHY, HIGH, OVER
    ) {}
}
