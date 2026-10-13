package dto;

import java.util.ArrayList;
import java.util.List;

public class DashboardManagerResponseDTO {
    private int activeProjects;
    private int totalProjects;
    private int atRiskProjects;
    private int totalDevelopers;
    private int averageDeveloperUtilization;
    private double totalSpend;
    private double totalBudget;
    private double totalRemainingBudget;
    private int criticalConflictCount;
    private int warningConflictCount;
    private int skillGapCount;
    private List<ConflictResponseDTO> recentConflicts = new ArrayList<>();
    private List<ProjectResponseDTO> projectSummaries = new ArrayList<>();

    public DashboardManagerResponseDTO() {}

    public int getActiveProjects() { return activeProjects; }
    public void setActiveProjects(int activeProjects) { this.activeProjects = activeProjects; }

    public int getTotalProjects() { return totalProjects; }
    public void setTotalProjects(int totalProjects) { this.totalProjects = totalProjects; }

    public int getAtRiskProjects() { return atRiskProjects; }
    public void setAtRiskProjects(int atRiskProjects) { this.atRiskProjects = atRiskProjects; }

    public int getTotalDevelopers() { return totalDevelopers; }
    public void setTotalDevelopers(int totalDevelopers) { this.totalDevelopers = totalDevelopers; }

    public int getAverageDeveloperUtilization() { return averageDeveloperUtilization; }
    public void setAverageDeveloperUtilization(int averageDeveloperUtilization) { this.averageDeveloperUtilization = averageDeveloperUtilization; }

    public double getTotalSpend() { return totalSpend; }
    public void setTotalSpend(double totalSpend) { this.totalSpend = totalSpend; }

    public double getTotalBudget() { return totalBudget; }
    public void setTotalBudget(double totalBudget) { this.totalBudget = totalBudget; }

    public double getTotalRemainingBudget() { return totalRemainingBudget; }
    public void setTotalRemainingBudget(double totalRemainingBudget) { this.totalRemainingBudget = totalRemainingBudget; }

    public int getCriticalConflictCount() { return criticalConflictCount; }
    public void setCriticalConflictCount(int criticalConflictCount) { this.criticalConflictCount = criticalConflictCount; }

    public int getWarningConflictCount() { return warningConflictCount; }
    public void setWarningConflictCount(int warningConflictCount) { this.warningConflictCount = warningConflictCount; }

    public int getSkillGapCount() { return skillGapCount; }
    public void setSkillGapCount(int skillGapCount) { this.skillGapCount = skillGapCount; }

    public List<ConflictResponseDTO> getRecentConflicts() { return recentConflicts; }
    public void setRecentConflicts(List<ConflictResponseDTO> recentConflicts) { this.recentConflicts = recentConflicts; }

    public List<ProjectResponseDTO> getProjectSummaries() { return projectSummaries; }
    public void setProjectSummaries(List<ProjectResponseDTO> projectSummaries) { this.projectSummaries = projectSummaries; }
}
