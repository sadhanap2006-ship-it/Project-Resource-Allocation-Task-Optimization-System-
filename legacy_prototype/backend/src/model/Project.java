package model;

import java.util.ArrayList;
import java.util.List;

public class Project {
    private int id;
    private String name;
    private String description;
    private String startDate;
    private String endDate;
    private double totalBudget;
    private double currentSpend;
    private String status; // PLANNED, ACTIVE, AT_RISK, COMPLETED, ON_HOLD
    private int milestoneCount;
    private int budgetUtilization;
    private int conflictCount;
    private List<Milestone> milestones = new ArrayList<>();

    public Project() {}

    public Project(int id, String name, String description, String startDate, String endDate, double totalBudget, double currentSpend, String status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalBudget = totalBudget;
        this.currentSpend = currentSpend;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public double getTotalBudget() { return totalBudget; }
    public void setTotalBudget(double totalBudget) { this.totalBudget = totalBudget; }

    public double getCurrentSpend() { return currentSpend; }
    public void setCurrentSpend(double currentSpend) { this.currentSpend = currentSpend; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getMilestoneCount() { return milestoneCount; }
    public void setMilestoneCount(int milestoneCount) { this.milestoneCount = milestoneCount; }

    public int getBudgetUtilization() { return budgetUtilization; }
    public void setBudgetUtilization(int budgetUtilization) { this.budgetUtilization = budgetUtilization; }

    public int getConflictCount() { return conflictCount; }
    public void setConflictCount(int conflictCount) { this.conflictCount = conflictCount; }

    public List<Milestone> getMilestones() { return milestones; }
    public void setMilestones(List<Milestone> milestones) { this.milestones = milestones; }
}
