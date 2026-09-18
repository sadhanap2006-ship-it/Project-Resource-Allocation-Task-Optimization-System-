package com.sist.rms.dto;

import jakarta.validation.constraints.NotBlank;

/** Body of POST /api/developers/{id}/skills. */
public class DeveloperSkillRequest {

    @NotBlank(message = "Skill name is required")
    private String skillName;

    /** BEGINNER, INTERMEDIATE or EXPERT. Optional - defaults to INTERMEDIATE. */
    private String proficiencyLevel;

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public String getProficiencyLevel() { return proficiencyLevel; }
    public void setProficiencyLevel(String proficiencyLevel) { this.proficiencyLevel = proficiencyLevel; }
}
