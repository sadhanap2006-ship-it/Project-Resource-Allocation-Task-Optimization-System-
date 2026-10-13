package edu.sist.prs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "simulation_change")
public class SimulationChangeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", insertable = false, updatable = false)
    private SimulationScenarioEntity scenario;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", length = 20, nullable = false)
    private SimulationChangeType changeType;

    @Column(name = "allocation_id")
    private Long allocationId;

    @Column(name = "from_resource_id")
    private Long fromResourceId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_resource_id", insertable = false, updatable = false)
    private ResourceEntity fromResource;

    @Column(name = "to_resource_id")
    private Long toResourceId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_resource_id", insertable = false, updatable = false)
    private ResourceEntity toResource;

    @Column(name = "milestone_id")
    private Long milestoneId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id", insertable = false, updatable = false)
    private MilestoneEntity milestone;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "new_pct")
    private Integer newPct;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public SimulationChangeEntity() {}

    public SimulationChangeEntity(Long scenarioId, SimulationChangeType changeType, Long allocationId, Long fromResourceId, Long toResourceId, Long milestoneId, LocalDate startDate, LocalDate endDate, Integer newPct) {
        this.scenarioId = scenarioId;
        this.changeType = changeType;
        this.allocationId = allocationId;
        this.fromResourceId = fromResourceId;
        this.toResourceId = toResourceId;
        this.milestoneId = milestoneId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.newPct = newPct;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(Long scenarioId) {
        this.scenarioId = scenarioId;
    }

    @JsonIgnore
    public SimulationScenarioEntity getScenario() {
        return scenario;
    }

    public void setScenario(SimulationScenarioEntity scenario) {
        this.scenario = scenario;
        this.scenarioId = scenario != null ? scenario.getId() : null;
    }

    public SimulationChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(SimulationChangeType changeType) {
        this.changeType = changeType;
    }

    public Long getAllocationId() {
        return allocationId;
    }

    public void setAllocationId(Long allocationId) {
        this.allocationId = allocationId;
    }

    public Long getFromResourceId() {
        return fromResourceId;
    }

    public void setFromResourceId(Long fromResourceId) {
        this.fromResourceId = fromResourceId;
    }

    @JsonIgnore
    public ResourceEntity getFromResource() {
        return fromResource;
    }

    public void setFromResource(ResourceEntity resource) {
        this.fromResource = resource;
        this.fromResourceId = resource != null ? resource.getId() : null;
    }

    public Long getToResourceId() {
        return toResourceId;
    }

    public void setToResourceId(Long toResourceId) {
        this.toResourceId = toResourceId;
    }

    @JsonIgnore
    public ResourceEntity getToResource() {
        return toResource;
    }

    public void setToResource(ResourceEntity resource) {
        this.toResource = resource;
        this.toResourceId = resource != null ? resource.getId() : null;
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

    public Integer getNewPct() {
        return newPct;
    }

    public void setNewPct(Integer newPct) {
        this.newPct = newPct;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
