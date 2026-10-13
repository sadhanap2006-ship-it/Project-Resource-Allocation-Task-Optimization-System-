package edu.sist.prs.dto.response;

public record MilestoneSkillRequirementDto(
    Long skillId,
    String skillName,
    String category,
    Integer minProficiency,
    Boolean mandatory
) {}
