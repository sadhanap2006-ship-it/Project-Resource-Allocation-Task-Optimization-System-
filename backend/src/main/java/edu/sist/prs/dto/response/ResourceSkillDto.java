package edu.sist.prs.dto.response;

public record ResourceSkillDto(
    Long skillId,
    String skillName,
    String category,
    Integer proficiency
) {}
