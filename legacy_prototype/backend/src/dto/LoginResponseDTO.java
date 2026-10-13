package dto;

public class LoginResponseDTO {
    private int userId;
    private String name;
    private String email;
    private String role;
    private Integer developerId;
    private String token;
    private String refreshToken;
    private boolean profileCompleted = true;

    public LoginResponseDTO() {}

    public LoginResponseDTO(int userId, String name, String email, String role, Integer developerId) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.developerId = developerId;
    }

    public LoginResponseDTO(int userId, String name, String email, String role, Integer developerId, String token, String refreshToken, boolean profileCompleted) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.developerId = developerId;
        this.token = token;
        this.refreshToken = refreshToken;
        this.profileCompleted = profileCompleted;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getDeveloperId() { return developerId; }
    public void setDeveloperId(Integer developerId) { this.developerId = developerId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public boolean isProfileCompleted() { return profileCompleted; }
    public void setProfileCompleted(boolean profileCompleted) { this.profileCompleted = profileCompleted; }
}
