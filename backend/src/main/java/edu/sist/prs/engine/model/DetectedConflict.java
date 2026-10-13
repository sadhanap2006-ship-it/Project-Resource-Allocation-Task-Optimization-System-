package edu.sist.prs.engine.model;

import edu.sist.prs.entity.ConflictSeverity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pure calculation representation of a detected conflict.
 */
public record DetectedConflict(
        String type,
        ConflictSeverity severity,
        Long projectId,
        Long milestoneId,
        Long resourceId,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal peakValue,
        String description,
        String dedupKey
) {}
