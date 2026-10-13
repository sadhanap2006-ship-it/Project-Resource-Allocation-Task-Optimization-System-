package edu.sist.prs.dto.response;

import java.time.LocalDate;
import java.util.List;

public record ProjectTimelineResponse(
        Long projectId,
        String projectName,
        List<TimelineMilestoneItem> milestones
) {
    public record TimelineMilestoneItem(
            Long id,
            String name,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            double progressPct,
            LocalDate forecastFinishDate,
            int delayDays,
            List<String> assignedResources
    ) {}
}
