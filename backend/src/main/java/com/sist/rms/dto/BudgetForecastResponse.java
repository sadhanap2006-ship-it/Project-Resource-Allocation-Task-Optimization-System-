package com.sist.rms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A deterministic forecast based only on the current burn rate.
 * This is ordinary arithmetic - it is NOT an AI or machine-learning prediction.
 */
public class BudgetForecastResponse {
    private Long projectId;
    private String projectName;
    private BigDecimal totalBudget = BigDecimal.ZERO;
    private BigDecimal currentSpending = BigDecimal.ZERO;
    private BigDecimal remainingBudget = BigDecimal.ZERO;

    private BigDecimal burnRatePerDay;
    private Long daysElapsed;
    private Long estimatedDaysRemaining;
    private LocalDate estimatedExhaustionDate;

    private boolean hasSpendingData;

    /** The sentence shown on the screen. */
    private String message;

    /** Always states, in words, that this is a plain calculation. */
    private String methodNote =
        "This forecast is a simple calculation: remaining budget divided by the current burn rate. "
        + "It is not an AI prediction.";

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public BigDecimal getTotalBudget() { return totalBudget; }
    public void setTotalBudget(BigDecimal totalBudget) { this.totalBudget = totalBudget; }
    public BigDecimal getCurrentSpending() { return currentSpending; }
    public void setCurrentSpending(BigDecimal currentSpending) { this.currentSpending = currentSpending; }
    public BigDecimal getRemainingBudget() { return remainingBudget; }
    public void setRemainingBudget(BigDecimal remainingBudget) { this.remainingBudget = remainingBudget; }
    public BigDecimal getBurnRatePerDay() { return burnRatePerDay; }
    public void setBurnRatePerDay(BigDecimal burnRatePerDay) { this.burnRatePerDay = burnRatePerDay; }
    public Long getDaysElapsed() { return daysElapsed; }
    public void setDaysElapsed(Long daysElapsed) { this.daysElapsed = daysElapsed; }
    public Long getEstimatedDaysRemaining() { return estimatedDaysRemaining; }
    public void setEstimatedDaysRemaining(Long estimatedDaysRemaining) { this.estimatedDaysRemaining = estimatedDaysRemaining; }
    public LocalDate getEstimatedExhaustionDate() { return estimatedExhaustionDate; }
    public void setEstimatedExhaustionDate(LocalDate estimatedExhaustionDate) { this.estimatedExhaustionDate = estimatedExhaustionDate; }
    public boolean isHasSpendingData() { return hasSpendingData; }
    public void setHasSpendingData(boolean hasSpendingData) { this.hasSpendingData = hasSpendingData; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getMethodNote() { return methodNote; }
    public void setMethodNote(String methodNote) { this.methodNote = methodNote; }
}
