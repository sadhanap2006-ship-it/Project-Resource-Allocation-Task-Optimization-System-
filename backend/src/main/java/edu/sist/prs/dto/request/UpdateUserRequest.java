package edu.sist.prs.dto.request;

import edu.sist.prs.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    String name,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    String email,

    @NotNull(message = "Role is required")
    UserRole role,

    Long resourceId,

    @NotNull(message = "Active status is required")
    Boolean active
) {}
