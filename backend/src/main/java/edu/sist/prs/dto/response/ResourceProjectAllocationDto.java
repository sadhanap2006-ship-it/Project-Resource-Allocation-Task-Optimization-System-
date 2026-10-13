package edu.sist.prs.dto.response;

public record ResourceProjectAllocationDto(
    Long projectId,
    String projectCode,
    String projectName,
    Integer allocationPct
) {}
