package dto;

public class AvailabilityRequestDTO {
    private int developerId;
    private String startDate;
    private String endDate;
    private String reason = "Planned Vacation";
    private String status = "APPROVED";
    private Integer availabilityPercentage; // For partial availability changes

    public AvailabilityRequestDTO() {}

    public int getDeveloperId() { return developerId; }
    public void setDeveloperId(int developerId) { this.developerId = developerId; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getAvailabilityPercentage() { return availabilityPercentage; }
    public void setAvailabilityPercentage(Integer availabilityPercentage) { this.availabilityPercentage = availabilityPercentage; }
}
