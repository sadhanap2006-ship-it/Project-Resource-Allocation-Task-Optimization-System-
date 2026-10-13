package dto;

import java.util.ArrayList;
import java.util.List;

public class DashboardDeveloperResponseDTO {
    private int developerId;
    private String name;
    private String email;
    private String roleTitle;
    private String department;
    private double hourlyCost;
    private List<String> skills = new ArrayList<>();
    private int currentAllocationPercentage;
    private int remainingCapacityPercentage;
    private List<AssignedMilestoneDTO> assignedMilestones = new ArrayList<>();
    private List<AvailabilityResponseDTO> plannedLeaves = new ArrayList<>();
    private List<NotificationResponseDTO> notifications = new ArrayList<>();

    public DashboardDeveloperResponseDTO() {}

    public int getDeveloperId() { return developerId; }
    public void setDeveloperId(int developerId) { this.developerId = developerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRoleTitle() { return roleTitle; }
    public void setRoleTitle(String roleTitle) { this.roleTitle = roleTitle; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public double getHourlyCost() { return hourlyCost; }
    public void setHourlyCost(double hourlyCost) { this.hourlyCost = hourlyCost; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public int getCurrentAllocationPercentage() { return currentAllocationPercentage; }
    public void setCurrentAllocationPercentage(int currentAllocationPercentage) { this.currentAllocationPercentage = currentAllocationPercentage; }

    public int getRemainingCapacityPercentage() { return remainingCapacityPercentage; }
    public void setRemainingCapacityPercentage(int remainingCapacityPercentage) { this.remainingCapacityPercentage = remainingCapacityPercentage; }

    public List<AssignedMilestoneDTO> getAssignedMilestones() { return assignedMilestones; }
    public void setAssignedMilestones(List<AssignedMilestoneDTO> assignedMilestones) { this.assignedMilestones = assignedMilestones; }

    public List<AvailabilityResponseDTO> getPlannedLeaves() { return plannedLeaves; }
    public void setPlannedLeaves(List<AvailabilityResponseDTO> plannedLeaves) { this.plannedLeaves = plannedLeaves; }

    public List<NotificationResponseDTO> getNotifications() { return notifications; }
    public void setNotifications(List<NotificationResponseDTO> notifications) { this.notifications = notifications; }

    public static class AssignedMilestoneDTO {
        private int milestoneId;
        private String name;
        private String projectName;
        private String startDate;
        private String endDate;
        private int progressPercentage;
        private String status;
        private int myAllocationPercentage;

        public AssignedMilestoneDTO() {}

        public int getMilestoneId() { return milestoneId; }
        public void setMilestoneId(int milestoneId) { this.milestoneId = milestoneId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }

        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }

        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }

        public int getProgressPercentage() { return progressPercentage; }
        public void setProgressPercentage(int progressPercentage) { this.progressPercentage = progressPercentage; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public int getMyAllocationPercentage() { return myAllocationPercentage; }
        public void setMyAllocationPercentage(int myAllocationPercentage) { this.myAllocationPercentage = myAllocationPercentage; }
    }
}
