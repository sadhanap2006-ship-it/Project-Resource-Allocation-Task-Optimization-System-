package com.sist.rms.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/** Body of POST/PUT /api/allocations. */
public class AllocationRequest {

    @NotNull(message = "Developer id is required")
    private Long developerId;

    @NotNull(message = "Project id is required")
    private Long projectId;

    /** Optional - an allocation can be to the project as a whole. */
    private Long milestoneId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Allocation percentage is required")
    @Min(value = 1, message = "Allocation percentage must be between 1 and 100")
    @Max(value = 100, message = "Allocation percentage must be between 1 and 100")
    private Integer allocationPercentage;

    public Long getDeveloperId() { return developerId; }
    public void setDeveloperId(Long developerId) { this.developerId = developerId; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getMilestoneId() { return milestoneId; }
    public void setMilestoneId(Long milestoneId) { this.milestoneId = milestoneId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Integer getAllocationPercentage() { return allocationPercentage; }
    public void setAllocationPercentage(Integer allocationPercentage) { this.allocationPercentage = allocationPercentage; }
}
