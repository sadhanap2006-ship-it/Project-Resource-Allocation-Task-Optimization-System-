package dto;

public class AllocationRequestDTO {
    private int developerId;
    private int milestoneId;
    private int allocationPercentage;

    public AllocationRequestDTO() {}

    public AllocationRequestDTO(int developerId, int milestoneId, int allocationPercentage) {
        this.developerId = developerId;
        this.milestoneId = milestoneId;
        this.allocationPercentage = allocationPercentage;
    }

    public int getDeveloperId() { return developerId; }
    public void setDeveloperId(int developerId) { this.developerId = developerId; }

    public int getMilestoneId() { return milestoneId; }
    public void setMilestoneId(int milestoneId) { this.milestoneId = milestoneId; }

    public int getAllocationPercentage() { return allocationPercentage; }
    public void setAllocationPercentage(int allocationPercentage) { this.allocationPercentage = allocationPercentage; }
}
