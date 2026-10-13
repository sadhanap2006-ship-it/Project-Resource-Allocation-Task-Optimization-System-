package dto;

import java.util.ArrayList;
import java.util.List;

public class DeveloperRequestDTO {
    private String name;
    private String email;
    private String roleTitle;
    private String department;
    private double hourlyCost;
    private int availabilityPercentage = 100;
    private String status = "ACTIVE";
    private List<Integer> skillIds = new ArrayList<>();

    public DeveloperRequestDTO() {}

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<Integer> getSkillIds() { return skillIds; }
    public void setSkillIds(List<Integer> skillIds) { this.skillIds = skillIds; }
}
