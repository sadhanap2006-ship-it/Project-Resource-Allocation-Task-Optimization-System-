package model;

import java.sql.Timestamp;

public class EmployeeSkill {
    private int developerId;
    private int skillId;
    private String skillName;
    private String category;
    private String proficiency; // BEGINNER, INTERMEDIATE, EXPERT
    private int yearsExperience = 1;
    private String status;      // PENDING, VERIFIED_MANAGER, VERIFIED_TRACK_RECORD, REJECTED
    private String source;      // MANUAL, EXTRACTED_TEXT, EXTRACTED_CERT, TASK_HISTORY
    private boolean interestedInFuture = false;
    private int endorsementsCount = 0;
    private Timestamp createdAt;

    public EmployeeSkill() {}

    public EmployeeSkill(int developerId, int skillId, String skillName, String category, String proficiency, int yearsExperience, String status, String source, boolean interestedInFuture, int endorsementsCount) {
        this.developerId = developerId;
        this.skillId = skillId;
        this.skillName = skillName;
        this.category = category;
        this.proficiency = proficiency;
        this.yearsExperience = yearsExperience;
        this.status = status;
        this.source = source;
        this.interestedInFuture = interestedInFuture;
        this.endorsementsCount = endorsementsCount;
    }

    public int getDeveloperId() { return developerId; }
    public void setDeveloperId(int developerId) { this.developerId = developerId; }

    public int getSkillId() { return skillId; }
    public void setSkillId(int skillId) { this.skillId = skillId; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getProficiency() { return proficiency; }
    public void setProficiency(String proficiency) { this.proficiency = proficiency; }

    public int getYearsExperience() { return yearsExperience; }
    public void setYearsExperience(int yearsExperience) { this.yearsExperience = yearsExperience; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public boolean isInterestedInFuture() { return interestedInFuture; }
    public void setInterestedInFuture(boolean interestedInFuture) { this.interestedInFuture = interestedInFuture; }

    public int getEndorsementsCount() { return endorsementsCount; }
    public void setEndorsementsCount(int endorsementsCount) { this.endorsementsCount = endorsementsCount; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
