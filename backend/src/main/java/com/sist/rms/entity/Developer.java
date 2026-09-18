package com.sist.rms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * A developer, i.e. one "resource profile" in the system.
 * Maps to the developers table.
 */
@Entity
@Table(name = "developers")
public class Developer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 150, unique = true)
    private String email;

    /** Job title, e.g. "Backend Developer". */
    @Column(nullable = false, length = 80)
    private String role;

    /** Cost of one hour of this developer's time, in rupees. */
    @Column(name = "hourly_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyCost;

    /** How much of this developer's time is available, 0-100. */
    @Column(name = "availability_percentage", nullable = false)
    private Integer availabilityPercentage = 100;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeveloperStatus status = DeveloperStatus.ACTIVE;

    public Developer() {
    }

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
    public void setAvailabilityPercentage(Integer availabilityPercentage) {
        this.availabilityPercentage = availabilityPercentage;
    }

    public DeveloperStatus getStatus() { return status; }
    public void setStatus(DeveloperStatus status) { this.status = status; }
}
