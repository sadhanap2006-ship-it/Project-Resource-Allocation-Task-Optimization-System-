package com.sist.rms.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Body of POST/PUT /api/milestones. */
public class MilestoneRequest {

    @NotNull(message = "Project id is required")
    private Long projectId;

    @NotBlank(message = "Milestone name is required")
    @Size(max = 150, message = "Milestone name cannot be longer than 150 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot be longer than 1000 characters")
    private String description;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Min(value = 0, message = "Progress percentage cannot be below 0")
    @Max(value = 100, message = "Progress percentage cannot be above 100")
    private Integer progressPercentage = 0;

    /** PLANNED, IN_PROGRESS, COMPLETED, DELAYED or AT_RISK. */
    private String status;

    @NotNull(message = "Estimated effort in developer-days is required")
    @Min(value = 1, message = "Estimated effort must be at least 1 developer-day")
    private Integer estimatedEffortDays;

    /** Skill names this milestone requires, e.g. ["Java", "Spring Boot"]. */
    private List<String> requiredSkills = new ArrayList<>();

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

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
}
