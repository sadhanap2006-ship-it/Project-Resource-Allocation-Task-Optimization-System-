package model;

import java.util.ArrayList;
import java.util.List;

public class Milestone {
    private int id;
    private int projectId;
    private String projectName;
    private String name;
    private String description;
    private String startDate;
    private String endDate;
    private double estimatedEffortHours;
    private int progressPercentage;
    private String status; // NOT_STARTED, IN_PROGRESS, AT_RISK, COMPLETED
    private List<String> requiredSkillNames = new ArrayList<>();
    private List<Integer> requiredSkillIds = new ArrayList<>();
    private List<Allocation> allocations = new ArrayList<>();

    public Milestone() {}

    public Milestone(int id, int projectId, String name, String startDate, String endDate, double estimatedEffortHours, int progressPercentage, String status) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.estimatedEffortHours = estimatedEffortHours;
        this.progressPercentage = progressPercentage;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

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

    public List<String> getRequiredSkillNames() { return requiredSkillNames; }
    public void setRequiredSkillNames(List<String> requiredSkillNames) { this.requiredSkillNames = requiredSkillNames; }

    public List<Integer> getRequiredSkillIds() { return requiredSkillIds; }
    public void setRequiredSkillIds(List<Integer> requiredSkillIds) { this.requiredSkillIds = requiredSkillIds; }

    public List<Allocation> getAllocations() { return allocations; }
    public void setAllocations(List<Allocation> allocations) { this.allocations = allocations; }
}
