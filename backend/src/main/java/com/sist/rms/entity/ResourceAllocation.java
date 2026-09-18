package com.sist.rms.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * "Developer X works on project P (milestone M) from start to end at N %".
 * This is the central table of the whole system.
 */
@Entity
@Table(name = "resource_allocations")
public class ResourceAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "developer_id", nullable = false)
    private Developer developer;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /** Optional: an allocation may be to the project as a whole. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "milestone_id")
    private Milestone milestone;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "allocation_percentage", nullable = false)
    private Integer allocationPercentage;

    public ResourceAllocation() {
    }

    /**
     * Two allocations compete for the same developer only when their date
     * ranges overlap. This is the single overlap rule used everywhere.
     */
    public boolean overlaps(LocalDate otherStart, LocalDate otherEnd) {
        return !startDate.isAfter(otherEnd) && !endDate.isBefore(otherStart);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Developer getDeveloper() { return developer; }
    public void setDeveloper(Developer developer) { this.developer = developer; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public Milestone getMilestone() { return milestone; }
    public void setMilestone(Milestone milestone) { this.milestone = milestone; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Integer getAllocationPercentage() { return allocationPercentage; }
    public void setAllocationPercentage(Integer allocationPercentage) {
        this.allocationPercentage = allocationPercentage;
    }
}
