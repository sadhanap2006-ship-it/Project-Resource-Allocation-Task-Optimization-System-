package edu.sist.prs.engine.model;

import edu.sist.prs.entity.CostUnit;
import edu.sist.prs.entity.ResourceStatus;
import edu.sist.prs.entity.ResourceType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pure data snapshot of a Resource for engine calculations.
 */
public record ResourceSnapshot(
        Long id,
        ResourceType type,
        String name,
        ResourceStatus status,
        int capacityPct,
        BigDecimal costRate,
        CostUnit costUnit,
        BigDecimal dailyQuotaUnits,
        BigDecimal monthlyCap,
        List<ResourceSkillSnapshot> skills
) {
    public ResourceSnapshot {
        if (skills == null) {
            skills = List.of();
        }
    }
}
