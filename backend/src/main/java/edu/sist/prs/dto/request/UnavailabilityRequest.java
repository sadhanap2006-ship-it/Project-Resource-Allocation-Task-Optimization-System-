package edu.sist.prs.dto.request;

import edu.sist.prs.entity.UnavailabilityType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UnavailabilityRequest(
        @NotNull(message = "Resource ID is required")
        Long resourceId,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        @NotNull(message = "Type is required")
        UnavailabilityType type,

        @Size(max = 255, message = "Reason cannot exceed 255 characters")
        String reason,

        Boolean approve
) {}
