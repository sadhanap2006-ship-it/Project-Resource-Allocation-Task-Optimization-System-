package com.sist.rms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * The budget of one project.
 * currentSpending is kept as the running total of the project's transactions,
 * so the dashboard does not have to SUM the transaction table every time.
 */
@Entity
@Table(name = "project_budgets")
public class ProjectBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private Project project;

    @Column(name = "total_budget", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalBudget = BigDecimal.ZERO;

    @Column(name = "current_spending", nullable = false, precision = 14, scale = 2)
    private BigDecimal currentSpending = BigDecimal.ZERO;

    public ProjectBudget() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public BigDecimal getTotalBudget() { return totalBudget; }
    public void setTotalBudget(BigDecimal totalBudget) { this.totalBudget = totalBudget; }

    public BigDecimal getCurrentSpending() { return currentSpending; }
    public void setCurrentSpending(BigDecimal currentSpending) { this.currentSpending = currentSpending; }
}
