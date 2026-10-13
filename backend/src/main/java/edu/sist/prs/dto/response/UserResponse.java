package edu.sist.prs.dto.response;

import edu.sist.prs.entity.UserRole;
import java.time.LocalDateTime;

public record UserResponse(
    Long id,
    String name,
    String email,
    UserRole role,
    Long resourceId,
    Boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
