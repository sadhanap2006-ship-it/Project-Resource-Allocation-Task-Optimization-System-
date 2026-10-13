package edu.sist.prs.engine.model;

import edu.sist.prs.entity.MilestoneStatus;
import edu.sist.prs.entity.ProjectStatus;

import java.time.LocalDate;

/**
 * Pure data snapshot of an Allocation for engine calculations.
 */
public record AllocationSnapshot(
        Long id,
        Long resourceId,
        Long milestoneId,
        Long projectId,
        LocalDate startDate,
        LocalDate endDate,
        int allocationPct,
        boolean overrideConfirmed,
        MilestoneStatus milestoneStatus,
        ProjectStatus projectStatus
) {}
