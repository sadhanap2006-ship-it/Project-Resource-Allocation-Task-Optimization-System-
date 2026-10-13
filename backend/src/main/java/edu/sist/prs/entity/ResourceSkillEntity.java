package edu.sist.prs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "resource_skill")
public class ResourceSkillEntity {

    @EmbeddedId
    private ResourceSkillId id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("resourceId")
    @JoinColumn(name = "resource_id", nullable = false)
    private ResourceEntity resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("skillId")
    @JoinColumn(name = "skill_id", nullable = false)
    private SkillEntity skill;

    @Column(name = "proficiency", nullable = false)
    private Integer proficiency;

    public ResourceSkillEntity() {}

    public ResourceSkillEntity(ResourceEntity resource, SkillEntity skill, Integer proficiency) {
        this.resource = resource;
        this.skill = skill;
        this.proficiency = proficiency;
        this.id = new ResourceSkillId(resource.getId(), skill.getId());
    }

    public ResourceSkillId getId() {
        return id;
    }

    public void setId(ResourceSkillId id) {
        this.id = id;
    }

    @JsonIgnore
    public ResourceEntity getResource() {
        return resource;
    }

    public void setResource(ResourceEntity resource) {
        this.resource = resource;
        if (resource != null && this.id != null) {
            this.id.setResourceId(resource.getId());
        }
    }

    public SkillEntity getSkill() {
        return skill;
    }

    public void setSkill(SkillEntity skill) {
        this.skill = skill;
        if (skill != null && this.id != null) {
            this.id.setSkillId(skill.getId());
        }
    }

    public Integer getProficiency() {
        return proficiency;
    }

    public void setProficiency(Integer proficiency) {
        this.proficiency = proficiency;
    }
}
