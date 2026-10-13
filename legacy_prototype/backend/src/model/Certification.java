package model;

import java.sql.Timestamp;

public class Certification {
    private int id;
    private int developerId;
    private String name;
    private String issuingBody;
    private String issueDate; // YYYY-MM-DD
    private String credentialId;
    private String fileName;
    private String fileData;
    private double confidenceScore = 0.95;
    private boolean verified = true;
    private Timestamp createdAt;

    public Certification() {}

    public Certification(int id, int developerId, String name, String issuingBody, String issueDate, String credentialId, String fileName, double confidenceScore, boolean verified) {
        this.id = id;
        this.developerId = developerId;
        this.name = name;
        this.issuingBody = issuingBody;
        this.issueDate = issueDate;
        this.credentialId = credentialId;
        this.fileName = fileName;
        this.confidenceScore = confidenceScore;
        this.verified = verified;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getDeveloperId() { return developerId; }
    public void setDeveloperId(int developerId) { this.developerId = developerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIssuingBody() { return issuingBody; }
    public void setIssuingBody(String issuingBody) { this.issuingBody = issuingBody; }

    public String getIssueDate() { return issueDate; }
    public void setIssueDate(String issueDate) { this.issueDate = issueDate; }

    public String getCredentialId() { return credentialId; }
    public void setCredentialId(String credentialId) { this.credentialId = credentialId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileData() { return fileData; }
    public void setFileData(String fileData) { this.fileData = fileData; }

    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
