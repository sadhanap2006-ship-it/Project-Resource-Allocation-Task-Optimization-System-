package model;

import java.util.ArrayList;
import java.util.List;

public class WhatIfScenario {
    private int id;
    private int milestoneId;
    private String milestoneName;
    private int projectId;
    private String projectName;
    private int currentDeveloperId;
    private String currentDeveloperName;
    private double currentCostPerHour;
    private double currentMilestoneCost;
    private String currentDeliveryDate;
    private int currentSkillMatchPercentage;
    private List<String> currentConflicts = new ArrayList<>();

    private int proposedDeveloperId;
    private String proposedDeveloperName;
    private double simulatedCostPerHour;
    private double simulatedMilestoneCost;
    private String simulatedDeliveryDate;
    private int simulatedSkillMatchPercentage;
    private List<String> simulatedConflicts = new ArrayList<>();

    private int allocationPercentage;
    private double costDifferencePerHour;
    private double milestoneCostDifference;
    private int deliveryDaysDelta;
    private String scenarioVerdict; // OPTIMAL, ACCEPTABLE, SUBOPTIMAL, CRITICAL_RISK
    private String recommendation;
    private String notes;

    public WhatIfScenario() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMilestoneId() { return milestoneId; }
    public void setMilestoneId(int milestoneId) { this.milestoneId = milestoneId; }

    public String getMilestoneName() { return milestoneName; }
    public void setMilestoneName(String milestoneName) { this.milestoneName = milestoneName; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public int getCurrentDeveloperId() { return currentDeveloperId; }
    public void setCurrentDeveloperId(int currentDeveloperId) { this.currentDeveloperId = currentDeveloperId; }

    public String getCurrentDeveloperName() { return currentDeveloperName; }
    public void setCurrentDeveloperName(String currentDeveloperName) { this.currentDeveloperName = currentDeveloperName; }

    public double getCurrentCostPerHour() { return currentCostPerHour; }
    public void setCurrentCostPerHour(double currentCostPerHour) { this.currentCostPerHour = currentCostPerHour; }

    public double getCurrentMilestoneCost() { return currentMilestoneCost; }
    public void setCurrentMilestoneCost(double currentMilestoneCost) { this.currentMilestoneCost = currentMilestoneCost; }

    public String getCurrentDeliveryDate() { return currentDeliveryDate; }
    public void setCurrentDeliveryDate(String currentDeliveryDate) { this.currentDeliveryDate = currentDeliveryDate; }

    public int getCurrentSkillMatchPercentage() { return currentSkillMatchPercentage; }
    public void setCurrentSkillMatchPercentage(int currentSkillMatchPercentage) { this.currentSkillMatchPercentage = currentSkillMatchPercentage; }

    public List<String> getCurrentConflicts() { return currentConflicts; }
    public void setCurrentConflicts(List<String> currentConflicts) { this.currentConflicts = currentConflicts; }

    public int getProposedDeveloperId() { return proposedDeveloperId; }
    public void setProposedDeveloperId(int proposedDeveloperId) { this.proposedDeveloperId = proposedDeveloperId; }

    public String getProposedDeveloperName() { return proposedDeveloperName; }
    public void setProposedDeveloperName(String proposedDeveloperName) { this.proposedDeveloperName = proposedDeveloperName; }

    public double getSimulatedCostPerHour() { return simulatedCostPerHour; }
    public void setSimulatedCostPerHour(double simulatedCostPerHour) { this.simulatedCostPerHour = simulatedCostPerHour; }
    public double getProposedCostPerHour() { return simulatedCostPerHour; }
    public void setProposedCostPerHour(double proposedCostPerHour) { this.simulatedCostPerHour = proposedCostPerHour; }

    public double getSimulatedMilestoneCost() { return simulatedMilestoneCost; }
    public void setSimulatedMilestoneCost(double simulatedMilestoneCost) { this.simulatedMilestoneCost = simulatedMilestoneCost; }

    public String getSimulatedDeliveryDate() { return simulatedDeliveryDate; }
    public void setSimulatedDeliveryDate(String simulatedDeliveryDate) { this.simulatedDeliveryDate = simulatedDeliveryDate; }

    public int getSimulatedSkillMatchPercentage() { return simulatedSkillMatchPercentage; }
    public void setSimulatedSkillMatchPercentage(int simulatedSkillMatchPercentage) { this.simulatedSkillMatchPercentage = simulatedSkillMatchPercentage; }

    public List<String> getSimulatedConflicts() { return simulatedConflicts; }
    public void setSimulatedConflicts(List<String> simulatedConflicts) { this.simulatedConflicts = simulatedConflicts; }

    public int getAllocationPercentage() { return allocationPercentage; }
    public void setAllocationPercentage(int allocationPercentage) { this.allocationPercentage = allocationPercentage; }

    public double getCostDifferencePerHour() { return costDifferencePerHour; }
    public void setCostDifferencePerHour(double costDifferencePerHour) { this.costDifferencePerHour = costDifferencePerHour; }

    public double getMilestoneCostDifference() { return milestoneCostDifference; }
    public void setMilestoneCostDifference(double milestoneCostDifference) { this.milestoneCostDifference = milestoneCostDifference; }

    public int getDeliveryDaysDelta() { return deliveryDaysDelta; }
    public void setDeliveryDaysDelta(int deliveryDaysDelta) { this.deliveryDaysDelta = deliveryDaysDelta; }

    public String getScenarioVerdict() { return scenarioVerdict; }
    public void setScenarioVerdict(String scenarioVerdict) { this.scenarioVerdict = scenarioVerdict; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
