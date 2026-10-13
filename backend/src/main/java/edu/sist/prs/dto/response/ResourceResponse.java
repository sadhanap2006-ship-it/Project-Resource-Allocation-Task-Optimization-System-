package edu.sist.prs.dto.response;

import edu.sist.prs.entity.CostUnit;
import edu.sist.prs.entity.ResourceStatus;
import edu.sist.prs.entity.ResourceType;
import java.math.BigDecimal;
import java.util.List;

public record ResourceResponse(
    Long id,
    ResourceType type,
    String name,
    ResourceStatus status,
    Integer capacityPct,
    BigDecimal costRate,
    CostUnit costUnit,

    // Developer fields
    String email,
    String jobTitle,
    List<ResourceSkillDto> skills,

    // Hardware fields
    String assetTag,
    String labLocation,

    // Cloud fields
    String provider,
    String unitName,
    BigDecimal dailyQuotaUnits,
    BigDecimal monthlyCap,

    // Calculated profile fields (AC1 / Section 6)
    Integer currentLoad,
    Integer peakLoad,
    Integer remainingCapacity,
    List<ResourceProjectAllocationDto> activeProjects
) {}
