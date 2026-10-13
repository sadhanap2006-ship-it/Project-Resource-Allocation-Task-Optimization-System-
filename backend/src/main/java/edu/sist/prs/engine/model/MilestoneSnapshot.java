package edu.sist.prs.engine.model;

import edu.sist.prs.entity.MilestoneStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Pure data snapshot of a Milestone for engine calculations.
 */
public record MilestoneSnapshot(
        Long id,
        Long projectId,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        MilestoneStatus status,
        int progressPct,
        BigDecimal estimatedEffortHours,
        List<SkillRequirementSnapshot> skillRequirements
) {
    public MilestoneSnapshot {
        if (skillRequirements == null) {
            skillRequirements = List.of();
        }
    }
}
