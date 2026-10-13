package edu.sist.prs.dto.response;

import edu.sist.prs.entity.ResourceType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AllocationResponse(
    Long id,
    Long resourceId,
    String resourceName,
    ResourceType resourceType,
    Long milestoneId,
    String milestoneName,
    Long projectId,
    String projectCode,
    LocalDate startDate,
    LocalDate endDate,
    Integer allocationPct,
    Boolean overrideConfirmed,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
