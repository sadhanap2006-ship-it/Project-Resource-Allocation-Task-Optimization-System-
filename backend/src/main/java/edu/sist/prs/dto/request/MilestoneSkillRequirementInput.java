package edu.sist.prs.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MilestoneSkillRequirementInput(
    @NotNull(message = "Skill ID is required")
    Long skillId,

    @NotNull(message = "Minimum proficiency is required")
    @Min(value = 1, message = "Minimum proficiency must be between 1 and 3")
    @Max(value = 3, message = "Minimum proficiency must be between 1 and 3")
    Integer minProficiency,

    Boolean mandatory
) {}
