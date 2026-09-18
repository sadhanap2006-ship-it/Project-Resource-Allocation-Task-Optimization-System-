package com.sist.rms.dto;

/** Tells the browser which dashboard to open after a successful login. */
public class LoginResponse {
    private Long userId;
    private String username;
    private String role;          // PROJECT_MANAGER or DEVELOPER
    private Long developerId;     // null for a manager
    private String developerName;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Long getDeveloperId() { return developerId; }
    public void setDeveloperId(Long developerId) { this.developerId = developerId; }
    public String getDeveloperName() { return developerName; }
    public void setDeveloperName(String developerName) { this.developerName = developerName; }
}
