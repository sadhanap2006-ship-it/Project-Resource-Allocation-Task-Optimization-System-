package com.sist.rms.entity;

import jakarta.persistence.*;

/**
 * Join row saying "this developer knows this skill at this level".
 * Written as a normal entity (instead of @ManyToMany) because the row
 * carries extra data and is much easier to explain.
 */
@Entity
@Table(name = "developer_skills",
       uniqueConstraints = @UniqueConstraint(columnNames = {"developer_id", "skill_id"}))
public class DeveloperSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "developer_id", nullable = false)
    private Developer developer;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(name = "proficiency_level", nullable = false, length = 20)
    private ProficiencyLevel proficiencyLevel = ProficiencyLevel.INTERMEDIATE;

    public DeveloperSkill() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Developer getDeveloper() { return developer; }
    public void setDeveloper(Developer developer) { this.developer = developer; }

    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }

    public ProficiencyLevel getProficiencyLevel() { return proficiencyLevel; }
    public void setProficiencyLevel(ProficiencyLevel proficiencyLevel) {
        this.proficiencyLevel = proficiencyLevel;
    }
}
