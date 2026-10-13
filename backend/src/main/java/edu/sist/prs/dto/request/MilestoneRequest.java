package edu.sist.prs.dto.request;

import edu.sist.prs.entity.MilestoneStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MilestoneRequest(
    @NotBlank(message = "Milestone name is required")
    @Size(max = 150, message = "Name cannot exceed 150 characters")
    String name,

    String description,

    @NotNull(message = "Start date is required")
    LocalDate startDate,

    @NotNull(message = "End date is required")
    LocalDate endDate,

    MilestoneStatus status,

    @Min(value = 0, message = "Progress percentage cannot be negative")
    @Max(value = 100, message = "Progress percentage cannot exceed 100")
    Integer progressPct,

    @NotNull(message = "Estimated effort hours is required")
    @DecimalMin(value = "0.01", message = "Estimated effort hours must be greater than zero")
    BigDecimal estimatedEffortHours
) {}
