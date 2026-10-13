package edu.sist.prs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "allocation")
public class AllocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", insertable = false, updatable = false)
    private ResourceEntity resource;

    @Column(name = "milestone_id", nullable = false)
    private Long milestoneId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id", insertable = false, updatable = false)
    private MilestoneEntity milestone;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "allocation_pct", nullable = false)
    private Integer allocationPct;

    @Column(name = "override_confirmed", nullable = false)
    private Boolean overrideConfirmed = false;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public AllocationEntity() {}

    public AllocationEntity(Long resourceId, Long milestoneId, LocalDate startDate, LocalDate endDate, Integer allocationPct, Boolean overrideConfirmed) {
        this.resourceId = resourceId;
        this.milestoneId = milestoneId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.allocationPct = allocationPct;
        this.overrideConfirmed = overrideConfirmed != null ? overrideConfirmed : false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    @JsonIgnore
    public ResourceEntity getResource() {
        return resource;
    }

    public void setResource(ResourceEntity resource) {
        this.resource = resource;
        this.resourceId = resource != null ? resource.getId() : null;
    }

    public Long getMilestoneId() {
        return milestoneId;
    }

    public void setMilestoneId(Long milestoneId) {
        this.milestoneId = milestoneId;
    }

    @JsonIgnore
    public MilestoneEntity getMilestone() {
        return milestone;
    }

    public void setMilestone(MilestoneEntity milestone) {
        this.milestone = milestone;
        this.milestoneId = milestone != null ? milestone.getId() : null;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getAllocationPct() {
        return allocationPct;
    }

    public void setAllocationPct(Integer allocationPct) {
        this.allocationPct = allocationPct;
    }

    public Boolean getOverrideConfirmed() {
        return overrideConfirmed;
    }

    public void setOverrideConfirmed(Boolean overrideConfirmed) {
        this.overrideConfirmed = overrideConfirmed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
