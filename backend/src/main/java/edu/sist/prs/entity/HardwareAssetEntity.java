package edu.sist.prs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hardware_asset")
public class HardwareAssetEntity {

    @Id
    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "resource_id")
    private ResourceEntity resource;

    @Column(name = "asset_tag", length = 50, nullable = false, unique = true)
    private String assetTag;

    @Column(name = "lab_location", length = 100)
    private String labLocation;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public HardwareAssetEntity() {}

    public HardwareAssetEntity(ResourceEntity resource, String assetTag, String labLocation) {
        this.resource = resource;
        this.assetTag = assetTag;
        this.labLocation = labLocation;
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

    public String getAssetTag() {
        return assetTag;
    }

    public void setAssetTag(String assetTag) {
        this.assetTag = assetTag;
    }

    public String getLabLocation() {
        return labLocation;
    }

    public void setLabLocation(String labLocation) {
        this.labLocation = labLocation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
