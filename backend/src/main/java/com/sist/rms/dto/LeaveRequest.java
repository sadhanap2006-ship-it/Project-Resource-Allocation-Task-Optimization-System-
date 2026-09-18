package com.sist.rms.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Body of POST /api/leaves. */
public class LeaveRequest {

    @NotNull(message = "Developer id is required")
    private Long developerId;

    @NotNull(message = "Leave start date is required")
    private LocalDate startDate;

    @NotNull(message = "Leave end date is required")
    private LocalDate endDate;

    private String reason;

    /** PLANNED, APPROVED or CANCELLED. Optional - defaults to APPROVED. */
    private String status;

    public Long getDeveloperId() { return developerId; }
    public void setDeveloperId(Long developerId) { this.developerId = developerId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
