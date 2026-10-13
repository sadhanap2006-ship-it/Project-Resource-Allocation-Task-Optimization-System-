package edu.sist.prs.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "resource")
public class ResourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private ResourceType type;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ResourceStatus status;

    @Column(name = "capacity_pct", nullable = false)
    private Integer capacityPct;

    @Column(name = "cost_rate", precision = 12, scale = 2, nullable = false)
    private BigDecimal costRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "cost_unit", length = 10, nullable = false)
    private CostUnit costUnit;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public ResourceEntity() {}

    public ResourceEntity(ResourceType type, String name, ResourceStatus status, Integer capacityPct, BigDecimal costRate, CostUnit costUnit) {
        this.type = type;
        this.name = name;
        this.status = status;
        this.capacityPct = capacityPct;
        this.costRate = costRate;
        this.costUnit = costUnit;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ResourceType getType() {
        return type;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public Integer getCapacityPct() {
        return capacityPct;
    }

    public void setCapacityPct(Integer capacityPct) {
        this.capacityPct = capacityPct;
    }

    public BigDecimal getCostRate() {
        return costRate;
    }

    public void setCostRate(BigDecimal costRate) {
        this.costRate = costRate;
    }

    public CostUnit getCostUnit() {
        return costUnit;
    }

    public void setCostUnit(CostUnit costUnit) {
        this.costUnit = costUnit;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
