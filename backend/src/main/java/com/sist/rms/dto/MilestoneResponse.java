package com.sist.rms.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MilestoneResponse {
    private Long id;
    private Long projectId;
    private String projectName;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer progressPercentage;
    private String status;
    private Integer estimatedEffortDays;
    private List<String> requiredSkills = new ArrayList<>();
    private List<String> assignedDevelopers = new ArrayList<>();

    /** Total allocation percentage of everybody working on this milestone. */
    private int totalAllocationPercentage;

    /** Delivery date estimated from the effort and the capacity (see docs). */
    private LocalDate estimatedDeliveryDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Integer getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(Integer progressPercentage) { this.progressPercentage = progressPercentage; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getEstimatedEffortDays() { return estimatedEffortDays; }
    public void setEstimatedEffortDays(Integer estimatedEffortDays) { this.estimatedEffortDays = estimatedEffortDays; }
    public List<String> getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(List<String> requiredSkills) { this.requiredSkills = requiredSkills; }
    public List<String> getAssignedDevelopers() { return assignedDevelopers; }
    public void setAssignedDevelopers(List<String> assignedDevelopers) { this.assignedDevelopers = assignedDevelopers; }
    public int getTotalAllocationPercentage() { return totalAllocationPercentage; }
    public void setTotalAllocationPercentage(int totalAllocationPercentage) { this.totalAllocationPercentage = totalAllocationPercentage; }
    public LocalDate getEstimatedDeliveryDate() { return estimatedDeliveryDate; }
    public void setEstimatedDeliveryDate(LocalDate estimatedDeliveryDate) { this.estimatedDeliveryDate = estimatedDeliveryDate; }
}
