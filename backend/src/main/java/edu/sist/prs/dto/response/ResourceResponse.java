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
) {
    @com.fasterxml.jackson.annotation.JsonProperty("costPerHour")
    public BigDecimal costPerHour() {
        return costRate;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("costPerDay")
    public BigDecimal costPerDay() {
        return costRate;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("costPerUnit")
    public BigDecimal costPerUnit() {
        return costRate;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("dailyQuota")
    public BigDecimal dailyQuota() {
        return dailyQuotaUnits;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("hardware")
    public java.util.Map<String, Object> hardware() {
        if (type != ResourceType.HARDWARE) return null;
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("assetTag", assetTag);
        map.put("location", labLocation);
        return map;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("cloud")
    public java.util.Map<String, Object> cloud() {
        if (type != ResourceType.CLOUD) return null;
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("provider", provider);
        map.put("unitType", unitName);
        return map;
    }
}

