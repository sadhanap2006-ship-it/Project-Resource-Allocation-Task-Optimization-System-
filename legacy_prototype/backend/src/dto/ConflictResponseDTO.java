package dto;

import java.util.ArrayList;
import java.util.List;

public class ConflictResponseDTO {
    private String id;
    private String type; // OVER_ALLOCATION, LEAVE_CONFLICT
    private String severity; // CRITICAL, WARNING
    private int developerId;
    private String developerName;
    private String title;
    private String description;
    private String recommendation;
    private String conflictWindowStart;
    private String conflictWindowEnd;
    private int peakAllocationPercentage;
    private List<String> conflictingProjects = new ArrayList<>();
    private Integer milestoneId;
    private String milestoneName;
    private String projectName;

    public ConflictResponseDTO() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getConflictType() { return type; }
    public void setConflictType(String conflictType) { this.type = conflictType; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public int getDeveloperId() { return developerId; }
    public void setDeveloperId(int developerId) { this.developerId = developerId; }

    public String getDeveloperName() { return developerName; }
    public void setDeveloperName(String developerName) { this.developerName = developerName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public String getConflictWindowStart() { return conflictWindowStart; }
    public void setConflictWindowStart(String conflictWindowStart) { this.conflictWindowStart = conflictWindowStart; }

    public String getConflictWindowEnd() { return conflictWindowEnd; }
    public void setConflictWindowEnd(String conflictWindowEnd) { this.conflictWindowEnd = conflictWindowEnd; }

    public int getPeakAllocationPercentage() { return peakAllocationPercentage; }
    public void setPeakAllocationPercentage(int peakAllocationPercentage) { this.peakAllocationPercentage = peakAllocationPercentage; }

    public List<String> getConflictingProjects() { return conflictingProjects; }
    public void setConflictingProjects(List<String> conflictingProjects) { this.conflictingProjects = conflictingProjects; }

    public Integer getMilestoneId() { return milestoneId; }
    public void setMilestoneId(Integer milestoneId) { this.milestoneId = milestoneId; }

    public String getMilestoneName() { return milestoneName; }
    public void setMilestoneName(String milestoneName) { this.milestoneName = milestoneName; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
}
