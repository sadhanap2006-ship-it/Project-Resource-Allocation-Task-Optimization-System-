package edu.sist.prs.engine.model;

import edu.sist.prs.entity.UnavailabilityStatus;
import edu.sist.prs.entity.UnavailabilityType;

import java.time.LocalDate;

/**
 * Pure data snapshot of Unavailability (leave or maintenance).
 */
public record UnavailabilitySnapshot(
        Long id,
        Long resourceId,
        LocalDate startDate,
        LocalDate endDate,
        UnavailabilityType type,
        UnavailabilityStatus status
) {}
