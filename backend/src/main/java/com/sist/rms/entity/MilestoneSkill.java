package com.sist.rms.entity;

import jakarta.persistence.*;

/** Join row saying "this milestone requires this skill". */
@Entity
@Table(name = "milestone_skills",
       uniqueConstraints = @UniqueConstraint(columnNames = {"milestone_id", "skill_id"}))
public class MilestoneSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "milestone_id", nullable = false)
    private Milestone milestone;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    public MilestoneSkill() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Milestone getMilestone() { return milestone; }
    public void setMilestone(Milestone milestone) { this.milestone = milestone; }

    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }
}
