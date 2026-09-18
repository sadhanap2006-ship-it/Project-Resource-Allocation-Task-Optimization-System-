package com.sist.rms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budget;
    private String status;
    private int milestoneCount;
    private int allocationCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getMilestoneCount() { return milestoneCount; }
    public void setMilestoneCount(int milestoneCount) { this.milestoneCount = milestoneCount; }
    public int getAllocationCount() { return allocationCount; }
    public void setAllocationCount(int allocationCount) { this.allocationCount = allocationCount; }
}
