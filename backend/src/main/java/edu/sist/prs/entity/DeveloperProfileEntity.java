package edu.sist.prs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "developer_profile")
public class DeveloperProfileEntity {

    @Id
    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "resource_id")
    private ResourceEntity resource;

    @Column(name = "email", length = 150, nullable = false, unique = true)
    private String email;

    @Column(name = "job_title", length = 100)
    private String jobTitle;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public DeveloperProfileEntity() {}

    public DeveloperProfileEntity(ResourceEntity resource, String email, String jobTitle) {
        this.resource = resource;
        this.email = email;
        this.jobTitle = jobTitle;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
