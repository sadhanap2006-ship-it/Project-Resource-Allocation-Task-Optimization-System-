package edu.sist.prs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "milestone_skill_requirement")
public class MilestoneSkillRequirementEntity {

    @EmbeddedId
    private MilestoneSkillRequirementId id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("milestoneId")
    @JoinColumn(name = "milestone_id", nullable = false)
    private MilestoneEntity milestone;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("skillId")
    @JoinColumn(name = "skill_id", nullable = false)
    private SkillEntity skill;

    @Column(name = "min_proficiency", nullable = false)
    private Integer minProficiency;

    @Column(name = "mandatory", nullable = false)
    private Boolean mandatory = true;

    public MilestoneSkillRequirementEntity() {}

    public MilestoneSkillRequirementEntity(MilestoneEntity milestone, SkillEntity skill, Integer minProficiency, Boolean mandatory) {
        this.milestone = milestone;
        this.skill = skill;
        this.minProficiency = minProficiency;
        this.mandatory = mandatory != null ? mandatory : true;
        this.id = new MilestoneSkillRequirementId(milestone.getId(), skill.getId());
    }

    public MilestoneSkillRequirementId getId() {
        return id;
    }

    public void setId(MilestoneSkillRequirementId id) {
        this.id = id;
    }

    @JsonIgnore
    public MilestoneEntity getMilestone() {
        return milestone;
    }

    public void setMilestone(MilestoneEntity milestone) {
        this.milestone = milestone;
        if (milestone != null && this.id != null) {
            this.id.setMilestoneId(milestone.getId());
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

    public Integer getMinProficiency() {
        return minProficiency;
    }

    public void setMinProficiency(Integer minProficiency) {
        this.minProficiency = minProficiency;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }
}
