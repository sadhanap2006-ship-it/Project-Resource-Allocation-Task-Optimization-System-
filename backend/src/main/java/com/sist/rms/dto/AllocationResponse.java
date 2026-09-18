package com.sist.rms.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AllocationResponse {
    private Long id;
    private Long developerId;
    private String developerName;
    private Long projectId;
    private String projectName;
    private Long milestoneId;
    private String milestoneName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer allocationPercentage;

    /**
     * Warnings raised while saving, e.g. "Arun does not have the required
     * skill MySQL". The allocation is still saved, but it is never silent.
     */
    private List<String> warnings = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDeveloperId() { return developerId; }
    public void setDeveloperId(Long developerId) { this.developerId = developerId; }
    public String getDeveloperName() { return developerName; }
    public void setDeveloperName(String developerName) { this.developerName = developerName; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public Long getMilestoneId() { return milestoneId; }
    public void setMilestoneId(Long milestoneId) { this.milestoneId = milestoneId; }
    public String getMilestoneName() { return milestoneName; }
    public void setMilestoneName(String milestoneName) { this.milestoneName = milestoneName; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Integer getAllocationPercentage() { return allocationPercentage; }
    public void setAllocationPercentage(Integer allocationPercentage) { this.allocationPercentage = allocationPercentage; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
}
