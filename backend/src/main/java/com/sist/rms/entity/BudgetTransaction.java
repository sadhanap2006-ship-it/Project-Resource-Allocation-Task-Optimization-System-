package com.sist.rms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** One amount actually spent on a project. */
@Entity
@Table(name = "budget_transactions")
public class BudgetTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_budget_id", nullable = false)
    private ProjectBudget projectBudget;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(length = 80)
    private String category;

    @Column(length = 255)
    private String description;

    public BudgetTransaction() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ProjectBudget getProjectBudget() { return projectBudget; }
    public void setProjectBudget(ProjectBudget projectBudget) { this.projectBudget = projectBudget; }

    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
