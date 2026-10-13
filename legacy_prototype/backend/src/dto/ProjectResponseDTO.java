package dto;

import java.util.ArrayList;
import java.util.List;

public class ProjectResponseDTO {
    private int id;
    private String name;
    private String description;
    private String startDate;
    private String endDate;
    private double totalBudget;
    private double currentSpend;
    private double remainingBudget;
    private String status;
    private List<String> requiredSkills = new ArrayList<>();
    private List<Integer> requiredSkillIds = new ArrayList<>();
    private int milestoneCount;
    private double progressPercentage;

    public ProjectResponseDTO() {}

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

    public double getRemainingBudget() { return remainingBudget; }
    public void setRemainingBudget(double remainingBudget) { this.remainingBudget = remainingBudget; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(List<String> requiredSkills) { this.requiredSkills = requiredSkills; }

    public List<Integer> getRequiredSkillIds() { return requiredSkillIds; }
    public void setRequiredSkillIds(List<Integer> requiredSkillIds) { this.requiredSkillIds = requiredSkillIds; }

    public int getMilestoneCount() { return milestoneCount; }
    public void setMilestoneCount(int milestoneCount) { this.milestoneCount = milestoneCount; }

    public double getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(double progressPercentage) { this.progressPercentage = progressPercentage; }
}
