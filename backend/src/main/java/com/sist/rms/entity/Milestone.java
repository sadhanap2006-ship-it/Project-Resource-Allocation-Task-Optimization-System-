package com.sist.rms.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * A milestone inside a project.
 *
 * estimatedEffortDays is the planned effort measured in DEVELOPER-DAYS.
 * 20 developer-days means "one full-time developer would need 20 days".
 * It is the input of the delivery-date formula in DeliveryEstimateService.
 */
@Entity
@Table(name = "milestones")
public class Milestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "progress_percentage", nullable = false)
    private Integer progressPercentage = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MilestoneStatus status = MilestoneStatus.PLANNED;

    @Column(name = "estimated_effort_days", nullable = false)
    private Integer estimatedEffortDays = 10;

    public Milestone() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Integer getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(Integer progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public MilestoneStatus getStatus() { return status; }
    public void setStatus(MilestoneStatus status) { this.status = status; }

    public Integer getEstimatedEffortDays() { return estimatedEffortDays; }
    public void setEstimatedEffortDays(Integer estimatedEffortDays) {
        this.estimatedEffortDays = estimatedEffortDays;
    }
}
