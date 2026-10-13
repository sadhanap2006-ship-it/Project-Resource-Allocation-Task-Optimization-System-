package edu.sist.prs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cloud_resource")
public class CloudResourceEntity {

    @Id
    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "resource_id")
    private ResourceEntity resource;

    @Column(name = "provider", length = 50, nullable = false)
    private String provider;

    @Column(name = "unit_name", length = 30, nullable = false)
    private String unitName;

    @Column(name = "daily_quota_units", precision = 12, scale = 2, nullable = false)
    private BigDecimal dailyQuotaUnits;

    @Column(name = "monthly_cap", precision = 12, scale = 2, nullable = false)
    private BigDecimal monthlyCap;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public CloudResourceEntity() {}

    public CloudResourceEntity(ResourceEntity resource, String provider, String unitName, BigDecimal dailyQuotaUnits, BigDecimal monthlyCap) {
        this.resource = resource;
        this.provider = provider;
        this.unitName = unitName;
        this.dailyQuotaUnits = dailyQuotaUnits;
        this.monthlyCap = monthlyCap;
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
        if (resource != null) {
            this.resourceId = resource.getId();
        }
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public BigDecimal getDailyQuotaUnits() {
        return dailyQuotaUnits;
    }

    public void setDailyQuotaUnits(BigDecimal dailyQuotaUnits) {
        this.dailyQuotaUnits = dailyQuotaUnits;
    }

    public BigDecimal getMonthlyCap() {
        return monthlyCap;
    }

    public void setMonthlyCap(BigDecimal monthlyCap) {
        this.monthlyCap = monthlyCap;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
