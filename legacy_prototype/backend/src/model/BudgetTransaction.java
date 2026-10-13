package model;

public class BudgetTransaction {
    private int id;
    private int projectId;
    private double amount;
    private String transactionDate;
    private String description;
    private String createdAt;

    public BudgetTransaction() {}

    public BudgetTransaction(int projectId, double amount, String transactionDate, String description) {
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

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
