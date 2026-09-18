package com.sist.rms.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * One detected conflict.
 * type is either "OVER_ALLOCATION" or "LEAVE_CONFLICT".
 */
public class ConflictResponse {
    private String type;
    private String severity;
    private Long developerId;
    private String developerName;
    private LocalDate startDate;
    private LocalDate endDate;

    /** Total allocation percentage in this period (over-allocation only). */
    private Integer totalAllocation;
    private Integer availability;

    private String message;

    /** Which allocations caused the conflict, e.g. "Hospital Portal (60%)". */
    private List<String> involvedAllocations = new ArrayList<>();

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public Long getDeveloperId() { return developerId; }
    public void setDeveloperId(Long developerId) { this.developerId = developerId; }
    public String getDeveloperName() { return developerName; }
    public void setDeveloperName(String developerName) { this.developerName = developerName; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Integer getTotalAllocation() { return totalAllocation; }
    public void setTotalAllocation(Integer totalAllocation) { this.totalAllocation = totalAllocation; }
    public Integer getAvailability() { return availability; }
    public void setAvailability(Integer availability) { this.availability = availability; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<String> getInvolvedAllocations() { return involvedAllocations; }
    public void setInvolvedAllocations(List<String> involvedAllocations) { this.involvedAllocations = involvedAllocations; }
}
