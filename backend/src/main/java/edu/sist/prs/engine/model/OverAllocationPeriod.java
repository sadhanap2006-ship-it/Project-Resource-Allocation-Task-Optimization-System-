package edu.sist.prs.engine.model;

import edu.sist.prs.entity.ConflictSeverity;

import java.time.LocalDate;

/**
 * Represents a merged contiguous period of over-allocation for a resource.
 */
public record OverAllocationPeriod(
        Long resourceId,
        LocalDate startDate,
        LocalDate endDate,
        int peakLoad,
        ConflictSeverity severity
) {}
