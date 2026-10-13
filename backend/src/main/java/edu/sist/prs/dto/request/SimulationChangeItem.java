package edu.sist.prs.dto.request;

import edu.sist.prs.entity.SimulationChangeType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SimulationChangeItem(
        @NotNull(message = "Change type is required")
        SimulationChangeType type,

        Long allocationId,
        Long fromResourceId,
        Long toResourceId,
        Long resourceId,
        Long milestoneId,
        LocalDate startDate,
        LocalDate endDate,

        @Min(value = 1, message = "Percentage must be at least 1")
        @Max(value = 100, message = "Percentage cannot exceed 100")
        Integer pct,

        Integer newPct
) {}
