package edu.sist.prs.dto.request;

import edu.sist.prs.entity.ProjectStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectRequest(
    @NotBlank(message = "Project code is required")
    @Size(max = 20, message = "Code cannot exceed 20 characters")
    String code,

    @NotBlank(message = "Project name is required")
    @Size(max = 150, message = "Name cannot exceed 150 characters")
    String name,

    String description,

    @NotNull(message = "Start date is required")
    LocalDate startDate,

    @NotNull(message = "End date is required")
    LocalDate endDate,

    ProjectStatus status,

    @NotNull(message = "Total budget is required")
    @DecimalMin(value = "0.01", message = "Total budget must be greater than zero")
    BigDecimal totalBudget
) {}
