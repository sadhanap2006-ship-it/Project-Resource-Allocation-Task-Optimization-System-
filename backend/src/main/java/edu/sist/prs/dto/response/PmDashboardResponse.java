package edu.sist.prs.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record PmDashboardResponse(
        int totalProjects,
        int activeProjects,
        int atRiskProjects,
        Map<String, Long> openConflictsBySeverity,
        int skillGapsCount,
        double averageUtilisation,
        List<PmProjectRow> projects,
        List<UpcomingDeadlineItem> upcomingDeadlines,
        List<NotificationResponse> latestNotifications
) {
    public record PmProjectRow(
            Long id,
            String code,
            String name,
            String health,
            double progressPct,
            BigDecimal totalBudget,
            BigDecimal actualSpend,
            double budgetUtilisationPct,
            LocalDate runOutDate
    ) {}

    public record UpcomingDeadlineItem(
            Long milestoneId,
            String milestoneName,
            Long projectId,
            String projectName,
            LocalDate endDate,
            double progressPct,
            int delayDays
    ) {}

    public record NotificationResponse(
            Long id,
            String title,
            String message,
            String type,
            boolean isRead,
            String createdAt
    ) {}
}
