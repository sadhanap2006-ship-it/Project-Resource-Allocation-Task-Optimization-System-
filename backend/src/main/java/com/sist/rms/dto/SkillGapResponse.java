package com.sist.rms.dto;

import java.util.ArrayList;
import java.util.List;

/** One milestone that is missing at least one required skill. */
public class SkillGapResponse {
    private Long milestoneId;
    private String milestoneName;
    private Long projectId;
    private String projectName;

    private List<String> requiredSkills = new ArrayList<>();
    private List<String> coveredSkills = new ArrayList<>();
    private List<String> missingSkills = new ArrayList<>();

    /**
     * For every missing skill, either the developers who could fill it,
     * or the sentence "No available developer has this skill."
     */
    private List<String> suggestions = new ArrayList<>();

    private String message;

    public Long getMilestoneId() { return milestoneId; }
    public void setMilestoneId(Long milestoneId) { this.milestoneId = milestoneId; }
    public String getMilestoneName() { return milestoneName; }
    public void setMilestoneName(String milestoneName) { this.milestoneName = milestoneName; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public List<String> getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(List<String> requiredSkills) { this.requiredSkills = requiredSkills; }
    public List<String> getCoveredSkills() { return coveredSkills; }
    public void setCoveredSkills(List<String> coveredSkills) { this.coveredSkills = coveredSkills; }
    public List<String> getMissingSkills() { return missingSkills; }
    public void setMissingSkills(List<String> missingSkills) { this.missingSkills = missingSkills; }
    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
