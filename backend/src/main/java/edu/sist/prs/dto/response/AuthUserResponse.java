package edu.sist.prs.dto.response;

import edu.sist.prs.entity.UserRole;

public record AuthUserResponse(
    Long id,
    String name,
    String email,
    UserRole role,
    Long resourceId,
    String token
) {
    public AuthUserResponse(Long id, String name, String email, UserRole role, Long resourceId) {
        this(id, name, email, role, resourceId, null);
    }
}
