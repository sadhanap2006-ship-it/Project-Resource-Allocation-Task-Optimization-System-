package com.sist.rms.dto;

import jakarta.validation.constraints.*;

/**
 * Body of POST /api/simulations/run and POST /api/simulations.
 *
 * "Replace currentDeveloperId with newDeveloperId on this milestone,
 *  at newAllocationPercentage - what would happen?"
 */
public class SimulationRequest {

    /** Only needed when SAVING a scenario. */
    private String scenarioName;

    @NotNull(message = "Milestone id is required")
    private Long milestoneId;

    @NotNull(message = "Current developer id is required")
    private Long currentDeveloperId;

    @NotNull(message = "New developer id is required")
    private Long newDeveloperId;

    /** If left empty, the current developer's allocation percentage is reused. */
    @Min(value = 1, message = "Allocation percentage must be between 1 and 100")
    @Max(value = 100, message = "Allocation percentage must be between 1 and 100")
    private Integer newAllocationPercentage;

    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }

    public Long getMilestoneId() { return milestoneId; }
    public void setMilestoneId(Long milestoneId) { this.milestoneId = milestoneId; }

    public Long getCurrentDeveloperId() { return currentDeveloperId; }
    public void setCurrentDeveloperId(Long currentDeveloperId) { this.currentDeveloperId = currentDeveloperId; }

    public Long getNewDeveloperId() { return newDeveloperId; }
    public void setNewDeveloperId(Long newDeveloperId) { this.newDeveloperId = newDeveloperId; }

    public Integer getNewAllocationPercentage() { return newAllocationPercentage; }
    public void setNewAllocationPercentage(Integer newAllocationPercentage) { this.newAllocationPercentage = newAllocationPercentage; }
}
