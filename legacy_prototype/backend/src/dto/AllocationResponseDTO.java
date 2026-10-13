package dto;

public class AllocationResponseDTO {
    private int id;
    private int developerId;
    private String developerName;
    private int projectId;
    private String projectName;
    private int milestoneId;
    private String milestoneName;
    private int allocationPercentage;
    private String startDate;
    private String endDate;
    private double hourlyCostSnapshot;
    private double estimatedHours;

    public AllocationResponseDTO() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getDeveloperId() { return developerId; }
    public void setDeveloperId(int developerId) { this.developerId = developerId; }

    public String getDeveloperName() { return developerName; }
    public void setDeveloperName(String developerName) { this.developerName = developerName; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public int getMilestoneId() { return milestoneId; }
    public void setMilestoneId(int milestoneId) { this.milestoneId = milestoneId; }

    public String getMilestoneName() { return milestoneName; }
    public void setMilestoneName(String milestoneName) { this.milestoneName = milestoneName; }

    public int getAllocationPercentage() { return allocationPercentage; }
    public void setAllocationPercentage(int allocationPercentage) { this.allocationPercentage = allocationPercentage; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public double getHourlyCostSnapshot() { return hourlyCostSnapshot; }
    public void setHourlyCostSnapshot(double hourlyCostSnapshot) { this.hourlyCostSnapshot = hourlyCostSnapshot; }

    public double getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(double estimatedHours) { this.estimatedHours = estimatedHours; }
}
