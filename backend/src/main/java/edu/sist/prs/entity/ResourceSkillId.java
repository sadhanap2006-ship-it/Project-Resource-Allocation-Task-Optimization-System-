package edu.sist.prs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ResourceSkillId implements Serializable {

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "skill_id", nullable = false)
    private Long skillId;

    public ResourceSkillId() {}

    public ResourceSkillId(Long resourceId, Long skillId) {
        this.resourceId = resourceId;
        this.skillId = skillId;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Long getSkillId() {
        return skillId;
    }

    public void setSkillId(Long skillId) {
        this.skillId = skillId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceSkillId that = (ResourceSkillId) o;
        return Objects.equals(resourceId, that.resourceId) && Objects.equals(skillId, that.skillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceId, skillId);
    }
}
