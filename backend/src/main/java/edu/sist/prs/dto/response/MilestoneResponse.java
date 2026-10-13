package edu.sist.prs.dto.response;

import edu.sist.prs.entity.MilestoneStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MilestoneResponse(
    Long id,
    Long projectId,
    String name,
    String description,
    LocalDate startDate,
    LocalDate endDate,
    MilestoneStatus status,
    Integer progressPct,
    BigDecimal estimatedEffortHours,
    List<MilestoneSkillRequirementDto> skills,
    LocalDate forecastFinishDate,
    Integer delayWorkingDays,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
