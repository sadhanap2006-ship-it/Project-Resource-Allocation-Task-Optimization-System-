package model;

public class Notification {
    private int id;
    private Integer userId;
    private String title;
    private String message;
    private String type; // OVER_ALLOCATION, LEAVE_CONFLICT, SKILL_GAP, BUDGET_WARNING, SYSTEM_ALERT
    private String severity; // CRITICAL, WARNING, INFO
    private boolean read;
    private String createdAt;

    public Notification() {}

    public Notification(int id, Integer userId, String title, String message, String type, String severity, boolean read) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.severity = severity;
        this.read = read;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
