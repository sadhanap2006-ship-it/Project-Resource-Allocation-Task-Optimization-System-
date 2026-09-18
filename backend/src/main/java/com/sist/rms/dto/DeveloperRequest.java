package com.sist.rms.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Body of POST/PUT /api/developers. */
public class DeveloperRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot be longer than 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Role is required")
    private String role;

    @NotNull(message = "Hourly cost is required")
    @DecimalMin(value = "0.0", message = "Hourly cost cannot be negative")
    private BigDecimal hourlyCost;

    @NotNull(message = "Availability percentage is required")
    @Min(value = 0, message = "Availability percentage cannot be below 0")
    @Max(value = 100, message = "Availability percentage cannot be above 100")
    private Integer availabilityPercentage = 100;

    /** ACTIVE, INACTIVE or ON_LEAVE. Optional - defaults to ACTIVE. */
    private String status;

    /** Optional list of skill names, e.g. ["Java", "MySQL"]. Unknown names are created. */
    private List<String> skills = new ArrayList<>();

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
