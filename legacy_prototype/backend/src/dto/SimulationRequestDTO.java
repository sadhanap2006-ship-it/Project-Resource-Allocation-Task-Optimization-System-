package dto;

public class SimulationRequestDTO {
    private int milestoneId;
    private int currentDeveloperId;
    private int proposedDeveloperId;
    private int allocationPercentage = 60;

    public SimulationRequestDTO() {}

    public int getMilestoneId() { return milestoneId; }
    public void setMilestoneId(int milestoneId) { this.milestoneId = milestoneId; }

    public int getCurrentDeveloperId() { return currentDeveloperId; }
    public void setCurrentDeveloperId(int currentDeveloperId) { this.currentDeveloperId = currentDeveloperId; }

    public int getProposedDeveloperId() { return proposedDeveloperId; }
    public void setProposedDeveloperId(int proposedDeveloperId) { this.proposedDeveloperId = proposedDeveloperId; }

    public int getAllocationPercentage() { return allocationPercentage; }
    public void setAllocationPercentage(int allocationPercentage) { this.allocationPercentage = allocationPercentage; }
}
