package com.sist.rms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A what-if scenario the manager chose to SAVE.
 *
 * Running a simulation never writes to this table - the numbers are
 * calculated in memory and returned. A row is created only when the user
 * presses "Save scenario".
 */
@Entity
@Table(name = "simulation_scenarios")
public class SimulationScenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_name", nullable = false, length = 150)
    private String scenarioName;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "milestone_id")
    private Milestone milestone;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "current_developer_id", nullable = false)
    private Developer currentDeveloper;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "new_developer_id", nullable = false)
    private Developer newDeveloper;

    /** What the current developer is allocated today. */
    @Column(name = "current_allocation_percentage", nullable = false)
    private Integer currentAllocationPercentage;

    /** What the replacement developer would be allocated. */
    @Column(name = "simulated_allocation_percentage", nullable = false)
    private Integer simulatedAllocationPercentage;

    @Column(name = "current_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal currentCost = BigDecimal.ZERO;

    @Column(name = "simulated_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal simulatedCost = BigDecimal.ZERO;

    @Column(name = "current_skill_match", nullable = false)
    private Integer currentSkillMatch = 0;

    @Column(name = "simulated_skill_match", nullable = false)
    private Integer simulatedSkillMatch = 0;

    @Column(name = "current_conflicts", nullable = false)
    private Integer currentConflicts = 0;

    @Column(name = "simulated_conflicts", nullable = false)
    private Integer simulatedConflicts = 0;

    @Column(name = "current_delivery_date")
    private LocalDate currentDeliveryDate;

    @Column(name = "simulated_delivery_date")
    private LocalDate simulatedDeliveryDate;

    @Column(name = "budget_impact", nullable = false, precision = 14, scale = 2)
    private BigDecimal budgetImpact = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public SimulationScenario() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public Milestone getMilestone() { return milestone; }
    public void setMilestone(Milestone milestone) { this.milestone = milestone; }

    public Developer getCurrentDeveloper() { return currentDeveloper; }
    public void setCurrentDeveloper(Developer currentDeveloper) { this.currentDeveloper = currentDeveloper; }

    public Developer getNewDeveloper() { return newDeveloper; }
    public void setNewDeveloper(Developer newDeveloper) { this.newDeveloper = newDeveloper; }

    public Integer getCurrentAllocationPercentage() { return currentAllocationPercentage; }
    public void setCurrentAllocationPercentage(Integer currentAllocationPercentage) { this.currentAllocationPercentage = currentAllocationPercentage; }

    public Integer getSimulatedAllocationPercentage() { return simulatedAllocationPercentage; }
    public void setSimulatedAllocationPercentage(Integer simulatedAllocationPercentage) { this.simulatedAllocationPercentage = simulatedAllocationPercentage; }

    public BigDecimal getCurrentCost() { return currentCost; }
    public void setCurrentCost(BigDecimal currentCost) { this.currentCost = currentCost; }

    public BigDecimal getSimulatedCost() { return simulatedCost; }
    public void setSimulatedCost(BigDecimal simulatedCost) { this.simulatedCost = simulatedCost; }

    public Integer getCurrentSkillMatch() { return currentSkillMatch; }
    public void setCurrentSkillMatch(Integer currentSkillMatch) { this.currentSkillMatch = currentSkillMatch; }

    public Integer getSimulatedSkillMatch() { return simulatedSkillMatch; }
    public void setSimulatedSkillMatch(Integer simulatedSkillMatch) { this.simulatedSkillMatch = simulatedSkillMatch; }

    public Integer getCurrentConflicts() { return currentConflicts; }
    public void setCurrentConflicts(Integer currentConflicts) { this.currentConflicts = currentConflicts; }

    public Integer getSimulatedConflicts() { return simulatedConflicts; }
    public void setSimulatedConflicts(Integer simulatedConflicts) { this.simulatedConflicts = simulatedConflicts; }

    public LocalDate getCurrentDeliveryDate() { return currentDeliveryDate; }
    public void setCurrentDeliveryDate(LocalDate currentDeliveryDate) { this.currentDeliveryDate = currentDeliveryDate; }

    public LocalDate getSimulatedDeliveryDate() { return simulatedDeliveryDate; }
    public void setSimulatedDeliveryDate(LocalDate simulatedDeliveryDate) { this.simulatedDeliveryDate = simulatedDeliveryDate; }

    public BigDecimal getBudgetImpact() { return budgetImpact; }
    public void setBudgetImpact(BigDecimal budgetImpact) { this.budgetImpact = budgetImpact; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
