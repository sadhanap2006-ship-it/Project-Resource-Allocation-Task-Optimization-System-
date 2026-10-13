package dto;

import java.util.ArrayList;
import java.util.List;

public class ProjectRequestDTO {
    private String name;
    private String description;
    private String startDate;
    private String endDate;
    private double totalBudget;
    private double currentSpend;
    private String status = "PLANNED";
    private List<Integer> requiredSkillIds = new ArrayList<>();

    public ProjectRequestDTO() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public double getTotalBudget() { return totalBudget; }
    public void setTotalBudget(double totalBudget) { this.totalBudget = totalBudget; }

    public double getCurrentSpend() { return currentSpend; }
    public void setCurrentSpend(double currentSpend) { this.currentSpend = currentSpend; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<Integer> getRequiredSkillIds() { return requiredSkillIds; }
    public void setRequiredSkillIds(List<Integer> requiredSkillIds) { this.requiredSkillIds = requiredSkillIds; }
}
