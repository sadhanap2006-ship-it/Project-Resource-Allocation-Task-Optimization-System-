package model;

import java.util.ArrayList;
import java.util.List;

public class Developer {
    private int id;
    private Integer userId;
    private String name;
    private String email;
    private String roleTitle;
    private String department;
    private double hourlyCost;
    private int availabilityPercentage;
    private int currentAllocation;
    private int remainingCapacity;
    private String status; // ACTIVE, ON_LEAVE, INACTIVE
    private List<String> skills = new ArrayList<>();
    private List<Integer> skillIds = new ArrayList<>();

    public Developer() {}

    public Developer(int id, String name, String email, String roleTitle, double hourlyCost, int availabilityPercentage) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.roleTitle = roleTitle;
        this.hourlyCost = hourlyCost;
        this.availabilityPercentage = availabilityPercentage;
        this.status = "ACTIVE";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

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

    public int getAvailabilityPercentage() { return availabilityPercentage; }
    public void setAvailabilityPercentage(int availabilityPercentage) { this.availabilityPercentage = availabilityPercentage; }

    public int getCurrentAllocation() { return currentAllocation; }
    public void setCurrentAllocation(int currentAllocation) { this.currentAllocation = currentAllocation; }

    public int getRemainingCapacity() { return remainingCapacity; }
    public void setRemainingCapacity(int remainingCapacity) { this.remainingCapacity = remainingCapacity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public List<Integer> getSkillIds() { return skillIds; }
    public void setSkillIds(List<Integer> skillIds) { this.skillIds = skillIds; }
}
