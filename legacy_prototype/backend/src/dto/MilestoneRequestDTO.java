package dto;

import java.util.ArrayList;
import java.util.List;

public class MilestoneRequestDTO {
    private int projectId;
    private String name;
    private String description;
    private String startDate;
    private String endDate;
    private double estimatedEffortHours = 40.0;
    private int progressPercentage = 0;
    private String status = "NOT_STARTED";
    private List<Integer> requiredSkillIds = new ArrayList<>();

    public MilestoneRequestDTO() {}

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public double getEstimatedEffortHours() { return estimatedEffortHours; }
    public void setEstimatedEffortHours(double estimatedEffortHours) { this.estimatedEffortHours = estimatedEffortHours; }

    public int getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(int progressPercentage) { this.progressPercentage = progressPercentage; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<Integer> getRequiredSkillIds() { return requiredSkillIds; }
    public void setRequiredSkillIds(List<Integer> requiredSkillIds) { this.requiredSkillIds = requiredSkillIds; }
}
