package edu.sist.prs.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SimulationSaveRequest(
        @NotBlank(message = "Scenario name is required")
        @Size(max = 150, message = "Scenario name cannot exceed 150 characters")
        String name,

        @NotNull(message = "Project ID is required")
        Long projectId,

        @NotEmpty(message = "Changes list cannot be empty")
        List<SimulationChangeItem> changes
) {}
