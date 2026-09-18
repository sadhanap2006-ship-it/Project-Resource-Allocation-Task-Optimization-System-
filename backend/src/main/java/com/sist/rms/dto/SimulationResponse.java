package com.sist.rms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The result of a what-if resource swap.
 *
 * When this comes back from POST /api/simulations/run, id is null and
 * NOTHING has been written to the database. It only gets an id after the
 * manager explicitly saves the scenario.
 */
public class SimulationResponse {
    private Long id;                    // null until saved
    private boolean saved;

    private String scenarioName;
    private Long projectId;
    private String projectName;
    private Long milestoneId;
    private String milestoneName;

    private String currentDeveloperName;
    private String newDeveloperName;

    private BigDecimal currentHourlyCost;
    private BigDecimal newHourlyCost;

    private Integer currentAllocationPercentage;
    private Integer simulatedAllocationPercentage;

    private BigDecimal currentCost;
    private BigDecimal simulatedCost;
    private BigDecimal budgetImpact;          // simulated - current

    private Integer currentSkillMatch;        // 0-100 %
    private Integer simulatedSkillMatch;
    private List<String> currentMissingSkills = new ArrayList<>();
    private List<String> simulatedMissingSkills = new ArrayList<>();

    private Integer currentConflicts;
    private Integer simulatedConflicts;
    private List<String> simulatedConflictDetails = new ArrayList<>();

    private LocalDate currentDeliveryDate;
    private LocalDate simulatedDeliveryDate;
    private Long deliveryImpactDays;          // + = later, - = earlier

    private String deliveryImpactMessage;
    private String recommendation;

    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public boolean isSaved() { return saved; }
    public void setSaved(boolean saved) { this.saved = saved; }
    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public Long getMilestoneId() { return milestoneId; }
    public void setMilestoneId(Long milestoneId) { this.milestoneId = milestoneId; }
    public String getMilestoneName() { return milestoneName; }
    public void setMilestoneName(String milestoneName) { this.milestoneName = milestoneName; }
    public String getCurrentDeveloperName() { return currentDeveloperName; }
    public void setCurrentDeveloperName(String currentDeveloperName) { this.currentDeveloperName = currentDeveloperName; }
    public String getNewDeveloperName() { return newDeveloperName; }
    public void setNewDeveloperName(String newDeveloperName) { this.newDeveloperName = newDeveloperName; }
    public BigDecimal getCurrentHourlyCost() { return currentHourlyCost; }
    public void setCurrentHourlyCost(BigDecimal currentHourlyCost) { this.currentHourlyCost = currentHourlyCost; }
    public BigDecimal getNewHourlyCost() { return newHourlyCost; }
    public void setNewHourlyCost(BigDecimal newHourlyCost) { this.newHourlyCost = newHourlyCost; }
    public Integer getCurrentAllocationPercentage() { return currentAllocationPercentage; }
    public void setCurrentAllocationPercentage(Integer currentAllocationPercentage) { this.currentAllocationPercentage = currentAllocationPercentage; }
    public Integer getSimulatedAllocationPercentage() { return simulatedAllocationPercentage; }
    public void setSimulatedAllocationPercentage(Integer simulatedAllocationPercentage) { this.simulatedAllocationPercentage = simulatedAllocationPercentage; }
    public BigDecimal getCurrentCost() { return currentCost; }
    public void setCurrentCost(BigDecimal currentCost) { this.currentCost = currentCost; }
    public BigDecimal getSimulatedCost() { return simulatedCost; }
    public void setSimulatedCost(BigDecimal simulatedCost) { this.simulatedCost = simulatedCost; }
    public BigDecimal getBudgetImpact() { return budgetImpact; }
    public void setBudgetImpact(BigDecimal budgetImpact) { this.budgetImpact = budgetImpact; }
    public Integer getCurrentSkillMatch() { return currentSkillMatch; }
    public void setCurrentSkillMatch(Integer currentSkillMatch) { this.currentSkillMatch = currentSkillMatch; }
    public Integer getSimulatedSkillMatch() { return simulatedSkillMatch; }
    public void setSimulatedSkillMatch(Integer simulatedSkillMatch) { this.simulatedSkillMatch = simulatedSkillMatch; }
    public List<String> getCurrentMissingSkills() { return currentMissingSkills; }
    public void setCurrentMissingSkills(List<String> currentMissingSkills) { this.currentMissingSkills = currentMissingSkills; }
    public List<String> getSimulatedMissingSkills() { return simulatedMissingSkills; }
    public void setSimulatedMissingSkills(List<String> simulatedMissingSkills) { this.simulatedMissingSkills = simulatedMissingSkills; }
    public Integer getCurrentConflicts() { return currentConflicts; }
    public void setCurrentConflicts(Integer currentConflicts) { this.currentConflicts = currentConflicts; }
    public Integer getSimulatedConflicts() { return simulatedConflicts; }
    public void setSimulatedConflicts(Integer simulatedConflicts) { this.simulatedConflicts = simulatedConflicts; }
    public List<String> getSimulatedConflictDetails() { return simulatedConflictDetails; }
    public void setSimulatedConflictDetails(List<String> simulatedConflictDetails) { this.simulatedConflictDetails = simulatedConflictDetails; }
    public LocalDate getCurrentDeliveryDate() { return currentDeliveryDate; }
    public void setCurrentDeliveryDate(LocalDate currentDeliveryDate) { this.currentDeliveryDate = currentDeliveryDate; }
    public LocalDate getSimulatedDeliveryDate() { return simulatedDeliveryDate; }
    public void setSimulatedDeliveryDate(LocalDate simulatedDeliveryDate) { this.simulatedDeliveryDate = simulatedDeliveryDate; }
    public Long getDeliveryImpactDays() { return deliveryImpactDays; }
    public void setDeliveryImpactDays(Long deliveryImpactDays) { this.deliveryImpactDays = deliveryImpactDays; }
    public String getDeliveryImpactMessage() { return deliveryImpactMessage; }
    public void setDeliveryImpactMessage(String deliveryImpactMessage) { this.deliveryImpactMessage = deliveryImpactMessage; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
