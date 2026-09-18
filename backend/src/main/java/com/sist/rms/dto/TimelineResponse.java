package com.sist.rms.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** One project with its milestones, ready to be drawn as a timeline. */
public class TimelineResponse {
    private Long projectId;
    private String projectName;
    private LocalDate projectStartDate;
    private LocalDate projectEndDate;
    private String projectStatus;
    private List<MilestoneResponse> milestones = new ArrayList<>();

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public LocalDate getProjectStartDate() { return projectStartDate; }
    public void setProjectStartDate(LocalDate projectStartDate) { this.projectStartDate = projectStartDate; }
    public LocalDate getProjectEndDate() { return projectEndDate; }
    public void setProjectEndDate(LocalDate projectEndDate) { this.projectEndDate = projectEndDate; }
    public String getProjectStatus() { return projectStatus; }
    public void setProjectStatus(String projectStatus) { this.projectStatus = projectStatus; }
    public List<MilestoneResponse> getMilestones() { return milestones; }
    public void setMilestones(List<MilestoneResponse> milestones) { this.milestones = milestones; }
}
