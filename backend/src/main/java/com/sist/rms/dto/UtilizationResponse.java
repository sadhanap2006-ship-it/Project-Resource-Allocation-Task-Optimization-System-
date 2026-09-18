package com.sist.rms.dto;

/** One row of the resource utilization table. */
public class UtilizationResponse {
    private Long developerId;
    private String developerName;

    /** Highest simultaneous allocation the developer has at any time. */
    private int peakAllocation;

    /** Allocation that applies to today (0 when the developer is free today). */
    private int currentAllocation;

    private int availability;

    /** "Available", "High Load", "Fully Allocated" or "Conflict". */
    private String status;

    /** availability - peakAllocation, never below 0. */
    private int remainingCapacity;

    private boolean onLeaveToday;

    public Long getDeveloperId() { return developerId; }
    public void setDeveloperId(Long developerId) { this.developerId = developerId; }
    public String getDeveloperName() { return developerName; }
    public void setDeveloperName(String developerName) { this.developerName = developerName; }
    public int getPeakAllocation() { return peakAllocation; }
    public void setPeakAllocation(int peakAllocation) { this.peakAllocation = peakAllocation; }
    public int getCurrentAllocation() { return currentAllocation; }
    public void setCurrentAllocation(int currentAllocation) { this.currentAllocation = currentAllocation; }
    public int getAvailability() { return availability; }
    public void setAvailability(int availability) { this.availability = availability; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getRemainingCapacity() { return remainingCapacity; }
    public void setRemainingCapacity(int remainingCapacity) { this.remainingCapacity = remainingCapacity; }
    public boolean isOnLeaveToday() { return onLeaveToday; }
    public void setOnLeaveToday(boolean onLeaveToday) { this.onLeaveToday = onLeaveToday; }
}
