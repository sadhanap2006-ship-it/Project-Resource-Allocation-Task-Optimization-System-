package com.sist.rms.dto;

import java.util.ArrayList;
import java.util.List;

/** The simpler dashboard a developer sees. */
public class DeveloperDashboardResponse {
    private Long developerId;
    private String developerName;
    private String role;
    private int availability;

    /** Allocation that applies to today. */
    private int currentAllocation;

    /** availability - currentAllocation, never below 0. */
    private int availableCapacity;

    private List<String> myProjects = new ArrayList<>();
    private List<MilestoneResponse> myMilestones = new ArrayList<>();
    private List<AllocationResponse> myAllocations = new ArrayList<>();
    private List<MilestoneResponse> upcomingDeadlines = new ArrayList<>();
    private List<LeaveResponse> myLeave = new ArrayList<>();
    private List<ConflictResponse> myConflicts = new ArrayList<>();
    private List<NotificationResponse> myNotifications = new ArrayList<>();

    public Long getDeveloperId() { return developerId; }
    public void setDeveloperId(Long developerId) { this.developerId = developerId; }
    public String getDeveloperName() { return developerName; }
    public void setDeveloperName(String developerName) { this.developerName = developerName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public int getAvailability() { return availability; }
    public void setAvailability(int availability) { this.availability = availability; }
    public int getCurrentAllocation() { return currentAllocation; }
    public void setCurrentAllocation(int currentAllocation) { this.currentAllocation = currentAllocation; }
    public int getAvailableCapacity() { return availableCapacity; }
    public void setAvailableCapacity(int availableCapacity) { this.availableCapacity = availableCapacity; }
    public List<String> getMyProjects() { return myProjects; }
    public void setMyProjects(List<String> myProjects) { this.myProjects = myProjects; }
    public List<MilestoneResponse> getMyMilestones() { return myMilestones; }
    public void setMyMilestones(List<MilestoneResponse> myMilestones) { this.myMilestones = myMilestones; }
    public List<AllocationResponse> getMyAllocations() { return myAllocations; }
    public void setMyAllocations(List<AllocationResponse> myAllocations) { this.myAllocations = myAllocations; }
    public List<MilestoneResponse> getUpcomingDeadlines() { return upcomingDeadlines; }
    public void setUpcomingDeadlines(List<MilestoneResponse> upcomingDeadlines) { this.upcomingDeadlines = upcomingDeadlines; }
    public List<LeaveResponse> getMyLeave() { return myLeave; }
    public void setMyLeave(List<LeaveResponse> myLeave) { this.myLeave = myLeave; }
    public List<ConflictResponse> getMyConflicts() { return myConflicts; }
    public void setMyConflicts(List<ConflictResponse> myConflicts) { this.myConflicts = myConflicts; }
    public List<NotificationResponse> getMyNotifications() { return myNotifications; }
    public void setMyNotifications(List<NotificationResponse> myNotifications) { this.myNotifications = myNotifications; }
}
