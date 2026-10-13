package edu.sist.prs.engine.model;

/**
 * Pure data snapshot of a Resource's skill.
 */
public record ResourceSkillSnapshot(
        Long skillId,
        String skillName,
        int proficiency
) {}
