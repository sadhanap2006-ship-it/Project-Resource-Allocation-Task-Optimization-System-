package dto;

import java.util.ArrayList;
import java.util.List;

public class BudgetResponseDTO {
    private int projectId;
    private String projectName;
    private double totalBudget;
    private double currentSpend;
    private double remainingBudget;
    private long elapsedDays;
    private long totalDurationDays;
    private double dailyBurnRate;
    private long daysUntilExhaustion;
    private String forecastExhaustionDate;
    private double budgetUtilizationPercentage;
    private String budgetStatus;
    private List<BudgetTransactionDTO> recentTransactions = new ArrayList<>();

    public BudgetResponseDTO() {}

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public double getTotalBudget() { return totalBudget; }
    public void setTotalBudget(double totalBudget) { this.totalBudget = totalBudget; }

    public double getCurrentSpend() { return currentSpend; }
    public void setCurrentSpend(double currentSpend) { this.currentSpend = currentSpend; }

    public double getRemainingBudget() { return remainingBudget; }
    public void setRemainingBudget(double remainingBudget) { this.remainingBudget = remainingBudget; }

    public long getElapsedDays() { return elapsedDays; }
    public void setElapsedDays(long elapsedDays) { this.elapsedDays = elapsedDays; }

    public long getTotalDurationDays() { return totalDurationDays; }
    public void setTotalDurationDays(long totalDurationDays) { this.totalDurationDays = totalDurationDays; }

    public double getDailyBurnRate() { return dailyBurnRate; }
    public void setDailyBurnRate(double dailyBurnRate) { this.dailyBurnRate = dailyBurnRate; }

    public long getDaysUntilExhaustion() { return daysUntilExhaustion; }
    public void setDaysUntilExhaustion(long daysUntilExhaustion) { this.daysUntilExhaustion = daysUntilExhaustion; }

    public String getForecastExhaustionDate() { return forecastExhaustionDate; }
    public void setForecastExhaustionDate(String forecastExhaustionDate) { this.forecastExhaustionDate = forecastExhaustionDate; }

    public double getBudgetUtilizationPercentage() { return budgetUtilizationPercentage; }
    public void setBudgetUtilizationPercentage(double budgetUtilizationPercentage) { this.budgetUtilizationPercentage = budgetUtilizationPercentage; }

    public String getBudgetStatus() { return budgetStatus; }
    public void setBudgetStatus(String budgetStatus) { this.budgetStatus = budgetStatus; }

    public List<BudgetTransactionDTO> getRecentTransactions() { return recentTransactions; }
    public void setRecentTransactions(List<BudgetTransactionDTO> recentTransactions) { this.recentTransactions = recentTransactions; }

    public static class BudgetTransactionDTO {
        private int id;
        private int projectId;
        private double amount;
        private String transactionDate;
        private String description;

        public BudgetTransactionDTO() {}

        public BudgetTransactionDTO(int id, int projectId, double amount, String transactionDate, String description) {
            this.id = id;
            this.projectId = projectId;
            this.amount = amount;
            this.transactionDate = transactionDate;
            this.description = description;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public int getProjectId() { return projectId; }
        public void setProjectId(int projectId) { this.projectId = projectId; }

        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }

        public String getTransactionDate() { return transactionDate; }
        public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
