package edu.sist.prs.dto.response;

import edu.sist.prs.entity.ProjectStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectResponse(
    Long id,
    String code,
    String name,
    String description,
    Long pmUserId,
    String pmUserName,
    LocalDate startDate,
    LocalDate endDate,
    ProjectStatus status,
    BigDecimal totalBudget,
    String health, // RED, AMBER, GREEN (Section 9.9)
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
