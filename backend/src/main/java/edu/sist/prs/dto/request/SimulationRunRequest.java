package edu.sist.prs.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SimulationRunRequest(
        @NotNull(message = "Project ID is required")
        Long projectId,

        @NotEmpty(message = "Changes list cannot be empty")
        List<SimulationChangeItem> changes
) {}
