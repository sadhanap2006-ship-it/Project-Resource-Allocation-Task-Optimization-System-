package model;

import java.sql.Timestamp;

public class TaskFeedback {
    private int id;
    private int milestoneId;
    private String milestoneName;
    private int developerId;
    private String developerName;
    private int managerId;
    private String managerName;
    private int rating; // 1-5
    private String comment;
    private Timestamp createdAt;

    public TaskFeedback() {}

    public TaskFeedback(int id, int milestoneId, int developerId, int managerId, int rating, String comment) {
        this.id = id;
        this.milestoneId = milestoneId;
        this.developerId = developerId;
        this.managerId = managerId;
        this.rating = rating;
        this.comment = comment;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMilestoneId() { return milestoneId; }
    public void setMilestoneId(int milestoneId) { this.milestoneId = milestoneId; }

    public String getMilestoneName() { return milestoneName; }
    public void setMilestoneName(String milestoneName) { this.milestoneName = milestoneName; }

    public int getDeveloperId() { return developerId; }
    public void setDeveloperId(int developerId) { this.developerId = developerId; }

    public String getDeveloperName() { return developerName; }
    public void setDeveloperName(String developerName) { this.developerName = developerName; }

    public int getManagerId() { return managerId; }
    public void setManagerId(int managerId) { this.managerId = managerId; }

    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
