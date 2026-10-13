package edu.sist.prs.dto.request;

import edu.sist.prs.entity.CostUnit;
import edu.sist.prs.entity.ResourceType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateResourceRequest(
    @NotNull(message = "Resource type is required")
    ResourceType type,

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    String name,

    @NotNull(message = "Capacity percentage is required")
    @Min(value = 0, message = "Capacity percentage cannot be negative")
    @Max(value = 100, message = "Capacity percentage cannot exceed 100")
    Integer capacityPct,

    @NotNull(message = "Cost rate is required")
    @DecimalMin(value = "0.0", message = "Cost rate cannot be negative")
    BigDecimal costRate,

    @NotNull(message = "Cost unit is required")
    CostUnit costUnit,

    // Developer Profile fields (if type == DEVELOPER)
    String email,
    String jobTitle,

    // Hardware Asset fields (if type == HARDWARE)
    String assetTag,
    String labLocation,

    // Cloud Resource fields (if type == CLOUD)
    String provider,
    String unitName,
    BigDecimal dailyQuotaUnits,
    BigDecimal monthlyCap
) {}
