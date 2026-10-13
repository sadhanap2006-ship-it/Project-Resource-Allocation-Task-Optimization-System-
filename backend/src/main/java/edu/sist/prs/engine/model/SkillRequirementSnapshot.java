package edu.sist.prs.engine.model;

/**
 * Pure data snapshot of a Milestone skill requirement.
 */
public record SkillRequirementSnapshot(
        Long skillId,
        String skillName,
        int minProficiency,
        boolean mandatory
) {}
