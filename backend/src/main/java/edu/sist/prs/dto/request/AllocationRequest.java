package edu.sist.prs.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AllocationRequest(
    @NotNull(message = "Resource ID is required")
    Long resourceId,

    @NotNull(message = "Milestone ID is required")
    Long milestoneId,

    @NotNull(message = "Start date is required")
    LocalDate startDate,

    @NotNull(message = "End date is required")
    LocalDate endDate,

    @NotNull(message = "Allocation percentage is required")
    @Min(value = 1, message = "Allocation percentage must be at least 1")
    @Max(value = 100, message = "Allocation percentage cannot exceed 100")
    Integer allocationPct,

    Boolean confirmOverride
) {}
