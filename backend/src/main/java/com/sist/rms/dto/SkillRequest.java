package com.sist.rms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of POST /api/skills. */
public class SkillRequest {

    @NotBlank(message = "Skill name is required")
    @Size(max = 80, message = "Skill name cannot be longer than 80 characters")
    private String name;

    private String category;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
