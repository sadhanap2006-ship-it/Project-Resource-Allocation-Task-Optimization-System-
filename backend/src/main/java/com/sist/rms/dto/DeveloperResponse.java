package com.sist.rms.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** What GET /api/developers returns for one developer. */
public class DeveloperResponse {
    private Long id;
    private String name;
    private String email;
    private String role;
    private BigDecimal hourlyCost;
    private Integer availabilityPercentage;
    private String status;
    private List<String> skills = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public BigDecimal getHourlyCost() { return hourlyCost; }
    public void setHourlyCost(BigDecimal hourlyCost) { this.hourlyCost = hourlyCost; }
    public Integer getAvailabilityPercentage() { return availabilityPercentage; }
    public void setAvailabilityPercentage(Integer availabilityPercentage) { this.availabilityPercentage = availabilityPercentage; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }
}
