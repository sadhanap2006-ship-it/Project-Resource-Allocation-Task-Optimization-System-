package edu.sist.prs.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ResourceSkillInput(
    @NotNull(message = "Skill ID is required")
    Long skillId,

    @NotNull(message = "Proficiency is required")
    @Min(value = 1, message = "Proficiency must be between 1 and 3")
    @Max(value = 3, message = "Proficiency must be between 1 and 3")
    Integer proficiency
) {}
