package com.sist.rms.dto;

import java.math.BigDecimal;

/**
 * Budget summary of one project.
 *
 * The burn rate and the forecast live in BudgetForecastResponse.
 *
 *   remainingBudget = totalBudget - currentSpending
 *   utilization     = (currentSpending / totalBudget) * 100
 *
 * When totalBudget is 0 or nothing has been spent yet, utilization is not
 * calculated at all (no divide by zero) and 'message' explains why.
 */
public class BudgetResponse {
    private Long projectId;
    private String projectName;
    private BigDecimal totalBudget = BigDecimal.ZERO;
    private BigDecimal currentSpending = BigDecimal.ZERO;
    private BigDecimal remainingBudget = BigDecimal.ZERO;

    /** null when it cannot be calculated. */
    private Double utilizationPercentage;

    private boolean hasSpendingData;

    /** "NONE", "WARNING" or "CRITICAL". */
    private String warningLevel = "NONE";

    private String message;

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
    public Double getUtilizationPercentage() { return utilizationPercentage; }
    public void setUtilizationPercentage(Double utilizationPercentage) { this.utilizationPercentage = utilizationPercentage; }
    public boolean isHasSpendingData() { return hasSpendingData; }
    public void setHasSpendingData(boolean hasSpendingData) { this.hasSpendingData = hasSpendingData; }
    public String getWarningLevel() { return warningLevel; }
    public void setWarningLevel(String warningLevel) { this.warningLevel = warningLevel; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
