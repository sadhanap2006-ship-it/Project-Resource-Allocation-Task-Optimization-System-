package model;

import java.sql.Timestamp;

public class SkillEndorsement {
    private int id;
    private int developerId;
    private int skillId;
    private int endorsedByUserId;
    private String endorserName;
    private Timestamp createdAt;

    public SkillEndorsement() {}

    public SkillEndorsement(int id, int developerId, int skillId, int endorsedByUserId) {
        this.id = id;
        this.developerId = developerId;
        this.skillId = skillId;
        this.endorsedByUserId = endorsedByUserId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getDeveloperId() { return developerId; }
    public void setDeveloperId(int developerId) { this.developerId = developerId; }

    public int getSkillId() { return skillId; }
    public void setSkillId(int skillId) { this.skillId = skillId; }

    public int getEndorsedByUserId() { return endorsedByUserId; }
    public void setEndorsedByUserId(int endorsedByUserId) { this.endorsedByUserId = endorsedByUserId; }

    public String getEndorserName() { return endorserName; }
    public void setEndorserName(String endorserName) { this.endorserName = endorserName; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
