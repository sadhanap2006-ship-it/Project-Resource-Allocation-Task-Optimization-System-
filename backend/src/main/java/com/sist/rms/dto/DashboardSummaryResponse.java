package com.sist.rms.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Everything the Project Manager dashboard needs, in one request. */
public class DashboardSummaryResponse {
    private long totalProjects;
    private long activeProjects;
    private long atRiskProjects;
    private long completedProjects;
    private long totalDevelopers;

    /** Average of every developer's current allocation, 0-100+. */
    private double averageUtilization;

    private long overAllocationConflicts;
    private long leaveConflicts;
    private long skillGaps;

    private BigDecimal totalBudget = BigDecimal.ZERO;
    private BigDecimal totalSpending = BigDecimal.ZERO;
    private Double overallBudgetUtilization;
    private String budgetMessage;

    private long unreadNotifications;

    private List<UtilizationResponse> utilization = new ArrayList<>();
    private List<ConflictResponse> conflicts = new ArrayList<>();
    private List<SkillGapResponse> skillGapDetails = new ArrayList<>();
    private List<MilestoneResponse> upcomingMilestones = new ArrayList<>();
    private List<BudgetResponse> budgetWarnings = new ArrayList<>();
    private List<NotificationResponse> notifications = new ArrayList<>();

    public long getTotalProjects() { return totalProjects; }
    public void setTotalProjects(long totalProjects) { this.totalProjects = totalProjects; }
    public long getActiveProjects() { return activeProjects; }
    public void setActiveProjects(long activeProjects) { this.activeProjects = activeProjects; }
    public long getAtRiskProjects() { return atRiskProjects; }
    public void setAtRiskProjects(long atRiskProjects) { this.atRiskProjects = atRiskProjects; }
    public long getCompletedProjects() { return completedProjects; }
    public void setCompletedProjects(long completedProjects) { this.completedProjects = completedProjects; }
    public long getTotalDevelopers() { return totalDevelopers; }
    public void setTotalDevelopers(long totalDevelopers) { this.totalDevelopers = totalDevelopers; }
    public double getAverageUtilization() { return averageUtilization; }
    public void setAverageUtilization(double averageUtilization) { this.averageUtilization = averageUtilization; }
    public long getOverAllocationConflicts() { return overAllocationConflicts; }
    public void setOverAllocationConflicts(long overAllocationConflicts) { this.overAllocationConflicts = overAllocationConflicts; }
    public long getLeaveConflicts() { return leaveConflicts; }
    public void setLeaveConflicts(long leaveConflicts) { this.leaveConflicts = leaveConflicts; }
    public long getSkillGaps() { return skillGaps; }
    public void setSkillGaps(long skillGaps) { this.skillGaps = skillGaps; }
    public BigDecimal getTotalBudget() { return totalBudget; }
    public void setTotalBudget(BigDecimal totalBudget) { this.totalBudget = totalBudget; }
    public BigDecimal getTotalSpending() { return totalSpending; }
    public void setTotalSpending(BigDecimal totalSpending) { this.totalSpending = totalSpending; }
    public Double getOverallBudgetUtilization() { return overallBudgetUtilization; }
    public void setOverallBudgetUtilization(Double overallBudgetUtilization) { this.overallBudgetUtilization = overallBudgetUtilization; }
    public String getBudgetMessage() { return budgetMessage; }
    public void setBudgetMessage(String budgetMessage) { this.budgetMessage = budgetMessage; }
    public long getUnreadNotifications() { return unreadNotifications; }
    public void setUnreadNotifications(long unreadNotifications) { this.unreadNotifications = unreadNotifications; }
    public List<UtilizationResponse> getUtilization() { return utilization; }
    public void setUtilization(List<UtilizationResponse> utilization) { this.utilization = utilization; }
    public List<ConflictResponse> getConflicts() { return conflicts; }
    public void setConflicts(List<ConflictResponse> conflicts) { this.conflicts = conflicts; }
    public List<SkillGapResponse> getSkillGapDetails() { return skillGapDetails; }
    public void setSkillGapDetails(List<SkillGapResponse> skillGapDetails) { this.skillGapDetails = skillGapDetails; }
    public List<MilestoneResponse> getUpcomingMilestones() { return upcomingMilestones; }
    public void setUpcomingMilestones(List<MilestoneResponse> upcomingMilestones) { this.upcomingMilestones = upcomingMilestones; }
    public List<BudgetResponse> getBudgetWarnings() { return budgetWarnings; }
    public void setBudgetWarnings(List<BudgetResponse> budgetWarnings) { this.budgetWarnings = budgetWarnings; }
    public List<NotificationResponse> getNotifications() { return notifications; }
    public void setNotifications(List<NotificationResponse> notifications) { this.notifications = notifications; }
}
