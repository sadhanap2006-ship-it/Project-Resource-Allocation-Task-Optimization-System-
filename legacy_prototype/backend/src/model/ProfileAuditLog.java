package model;

import java.sql.Timestamp;

public class ProfileAuditLog {
    private int id;
    private int developerId;
    private int actorUserId;
    private String actorName;
    private String actionType;
    private String oldValue;
    private String newValue;
    private Timestamp createdAt;

    public ProfileAuditLog() {}

    public ProfileAuditLog(int id, int developerId, int actorUserId, String actionType, String oldValue, String newValue) {
        this.id = id;
        this.developerId = developerId;
        this.actorUserId = actorUserId;
        this.actionType = actionType;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getDeveloperId() { return developerId; }
    public void setDeveloperId(int developerId) { this.developerId = developerId; }

    public int getActorUserId() { return actorUserId; }
    public void setActorUserId(int actorUserId) { this.actorUserId = actorUserId; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
