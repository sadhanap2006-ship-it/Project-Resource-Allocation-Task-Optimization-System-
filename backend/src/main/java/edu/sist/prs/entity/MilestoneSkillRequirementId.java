package edu.sist.prs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class MilestoneSkillRequirementId implements Serializable {

    @Column(name = "milestone_id", nullable = false)
    private Long milestoneId;

    @Column(name = "skill_id", nullable = false)
    private Long skillId;

    public MilestoneSkillRequirementId() {}

    public MilestoneSkillRequirementId(Long milestoneId, Long skillId) {
        this.milestoneId = milestoneId;
        this.skillId = skillId;
    }

    public Long getMilestoneId() {
        return milestoneId;
    }

    public void setMilestoneId(Long milestoneId) {
        this.milestoneId = milestoneId;
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
        MilestoneSkillRequirementId that = (MilestoneSkillRequirementId) o;
        return Objects.equals(milestoneId, that.milestoneId) && Objects.equals(skillId, that.skillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(milestoneId, skillId);
    }
}
