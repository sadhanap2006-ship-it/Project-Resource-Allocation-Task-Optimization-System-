package edu.sist.prs.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SkillRequest(
    @NotBlank(message = "Skill name is required")
    @Size(max = 60, message = "Skill name cannot exceed 60 characters")
    String name,

    @Size(max = 60, message = "Category cannot exceed 60 characters")
    String category
) {}
