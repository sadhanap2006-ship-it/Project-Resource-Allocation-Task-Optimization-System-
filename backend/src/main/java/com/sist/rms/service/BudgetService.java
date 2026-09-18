package com.sist.rms.service;

import com.sist.rms.dto.BudgetForecastResponse;
import com.sist.rms.dto.BudgetResponse;
import com.sist.rms.dto.BudgetTransactionRequest;
import com.sist.rms.dto.BudgetTransactionResponse;
import com.sist.rms.entity.BudgetTransaction;
import com.sist.rms.entity.Project;
import com.sist.rms.entity.ProjectBudget;
import com.sist.rms.exception.ResourceNotFoundException;
import com.sist.rms.exception.ValidationException;
import com.sist.rms.repository.BudgetTransactionRepository;
import com.sist.rms.repository.ProjectBudgetRepository;
import com.sist.rms.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * BUDGET RULES
 *
 *   remaining budget  = total budget - current spending
 *   utilization (%)   = (current spending / total budget) x 100
 *
 * Divide-by-zero is impossible here: when the total budget is 0, or when
 * nothing has been spent yet, no division happens at all and the response
 * carries the message "No spending data available" instead.
 *
 * BUDGET FORECAST (plain arithmetic, NOT an AI prediction)
 *
 *   days elapsed  = days from the first spending entry until today (at least 1)
 *   burn rate     = current spending / days elapsed          (rupees per day)
 *   days left     = remaining budget / burn rate             (rounded down)
 *   exhaustion    = today + days left
 *
 * Specification example:
 *   500,000 budget, 300,000 spent, 10,000 per day
 *     -> remaining 200,000 -> 200,000 / 10,000 = 20 days
 */
@Service
public class BudgetService {

    /** At or above this utilization the dashboard shows a CRITICAL warning. */
    public static final double CRITICAL_THRESHOLD = 90.0;
    /** At or above this utilization the dashboard shows a WARNING. */
    public static final double WARNING_THRESHOLD = 75.0;

    public static final String NO_SPENDING_MESSAGE = "No spending data available";

    private final ProjectRepository projectRepository;
    private final ProjectBudgetRepository budgetRepository;
    private final BudgetTransactionRepository transactionRepository;

    public BudgetService(ProjectRepository projectRepository,
                         ProjectBudgetRepository budgetRepository,
                         BudgetTransactionRepository transactionRepository) {
        this.projectRepository = projectRepository;
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
    }

    // -----------------------------------------------------------------
    //  Pure calculations - no database, so they are easy to unit test
    // -----------------------------------------------------------------

    /**
     * Builds the budget summary from plain numbers.
     * Returns utilization = null whenever it cannot be calculated.
     */
    public BudgetResponse calculateBudget(Long projectId, String projectName,
                                          BigDecimal totalBudget, BigDecimal currentSpending) {
        BigDecimal total = totalBudget == null ? BigDecimal.ZERO : totalBudget;
        BigDecimal spent = currentSpending == null ? BigDecimal.ZERO : currentSpending;

        BudgetResponse response = new BudgetResponse();
        response.setProjectId(projectId);
        response.setProjectName(projectName);
        response.setTotalBudget(total);
        response.setCurrentSpending(spent);
        response.setRemainingBudget(total.subtract(spent));
        response.setHasSpendingData(spent.compareTo(BigDecimal.ZERO) > 0);

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            // no budget at all -> never divide
            response.setUtilizationPercentage(null);
            response.setWarningLevel("NONE");
            response.setMessage("No budget has been set for this project.");
            return response;
        }

        if (!response.isHasSpendingData()) {
            response.setUtilizationPercentage(null);
            response.setWarningLevel("NONE");
            response.setMessage(NO_SPENDING_MESSAGE);
            return response;
        }

        double utilization = spent.multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP)
                .doubleValue();
        response.setUtilizationPercentage(utilization);

        if (utilization >= CRITICAL_THRESHOLD) {
            response.setWarningLevel("CRITICAL");
            response.setMessage("CRITICAL: " + projectName + " has used "
                    + String.format("%.2f", utilization) + "% of its budget.");
        } else if (utilization >= WARNING_THRESHOLD) {
            response.setWarningLevel("WARNING");
            response.setMessage("WARNING: " + projectName + " has used "
                    + String.format("%.2f", utilization) + "% of its budget.");
        } else {
            response.setWarningLevel("NONE");
            response.setMessage("Budget usage is within the safe range.");
        }
        return response;
    }

    /**
     * Builds the forecast from plain numbers.
     *
     * @param firstSpendDate date of the earliest spending entry (null when nothing spent)
     * @param today          the date to calculate against (passed in so tests are repeatable)
     */
    public BudgetForecastResponse calculateForecast(Long projectId, String projectName,
                                                    BigDecimal totalBudget, BigDecimal currentSpending,
                                                    LocalDate firstSpendDate, LocalDate today) {
        BigDecimal total = totalBudget == null ? BigDecimal.ZERO : totalBudget;
        BigDecimal spent = currentSpending == null ? BigDecimal.ZERO : currentSpending;
        BigDecimal remaining = total.subtract(spent);

        BudgetForecastResponse forecast = new BudgetForecastResponse();
        forecast.setProjectId(projectId);
        forecast.setProjectName(projectName);
        forecast.setTotalBudget(total);
        forecast.setCurrentSpending(spent);
        forecast.setRemainingBudget(remaining);
        forecast.setHasSpendingData(spent.compareTo(BigDecimal.ZERO) > 0);

        if (!forecast.isHasSpendingData() || firstSpendDate == null) {
            forecast.setMessage(NO_SPENDING_MESSAGE);
            return forecast;
        }

        // days elapsed is at least 1, so we never divide by zero
        long daysElapsed = Math.max(1, ChronoUnit.DAYS.between(firstSpendDate, today));
        forecast.setDaysElapsed(daysElapsed);

        BigDecimal burnRate = spent.divide(BigDecimal.valueOf(daysElapsed), 2, RoundingMode.HALF_UP);
        forecast.setBurnRatePerDay(burnRate);

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            forecast.setEstimatedDaysRemaining(0L);
            forecast.setEstimatedExhaustionDate(today);
            forecast.setMessage("The budget is already fully used. Spending is at "
                    + spent + " against a budget of " + total + ".");
            return forecast;
        }

        if (burnRate.compareTo(BigDecimal.ZERO) <= 0) {
            forecast.setMessage(NO_SPENDING_MESSAGE);
            return forecast;
        }

        long daysRemaining = remaining.divide(burnRate, 0, RoundingMode.FLOOR).longValue();
        forecast.setEstimatedDaysRemaining(daysRemaining);
        forecast.setEstimatedExhaustionDate(today.plusDays(daysRemaining));
        forecast.setMessage("At the current burn rate, the project is estimated to exhaust its budget in approximately "
                + daysRemaining + " days.");
        return forecast;
    }

    // -----------------------------------------------------------------
    //  Database-backed methods used by the controllers
    // -----------------------------------------------------------------

    public BudgetResponse getBudget(Long projectId) {
        ProjectBudget budget = findBudgetOrThrow(projectId);
        return calculateBudget(projectId, budget.getProject().getName(),
                budget.getTotalBudget(), budget.getCurrentSpending());
    }

    public BudgetForecastResponse getForecast(Long projectId) {
        ProjectBudget budget = findBudgetOrThrow(projectId);
        LocalDate firstSpend = findFirstSpendDate(budget);
        return calculateForecast(projectId, budget.getProject().getName(),
                budget.getTotalBudget(), budget.getCurrentSpending(), firstSpend, LocalDate.now());
    }

    /** Budget summary of every project - used by the dashboard. */
    public List<BudgetResponse> getAllBudgets() {
        List<BudgetResponse> budgets = new ArrayList<>();
        for (ProjectBudget budget : budgetRepository.findAll()) {
            budgets.add(calculateBudget(budget.getProject().getId(), budget.getProject().getName(),
                    budget.getTotalBudget(), budget.getCurrentSpending()));
        }
        return budgets;
    }

    public List<BudgetTransactionResponse> getTransactions(Long projectId) {
        ProjectBudget budget = findBudgetOrThrow(projectId);
        List<BudgetTransactionResponse> responses = new ArrayList<>();
        for (BudgetTransaction transaction :
                transactionRepository.findByProjectBudgetIdOrderByTransactionDateAsc(budget.getId())) {
            BudgetTransactionResponse response = new BudgetTransactionResponse();
            response.setId(transaction.getId());
            response.setTransactionDate(transaction.getTransactionDate());
            response.setAmount(transaction.getAmount());
            response.setCategory(transaction.getCategory());
            response.setDescription(transaction.getDescription());
            responses.add(response);
        }
        return responses;
    }

    /** Adds a spending entry and keeps current_spending in step with it. */
    @Transactional
    public BudgetTransactionResponse addTransaction(Long projectId, BudgetTransactionRequest request) {
        ProjectBudget budget = findBudgetOrThrow(projectId);

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Amount must be greater than zero");
        }

        BudgetTransaction transaction = new BudgetTransaction();
        transaction.setProjectBudget(budget);
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setAmount(request.getAmount());
        transaction.setCategory(request.getCategory());
        transaction.setDescription(request.getDescription());
        transactionRepository.save(transaction);

        budget.setCurrentSpending(budget.getCurrentSpending().add(request.getAmount()));
        budgetRepository.save(budget);

        BudgetTransactionResponse response = new BudgetTransactionResponse();
        response.setId(transaction.getId());
        response.setTransactionDate(transaction.getTransactionDate());
        response.setAmount(transaction.getAmount());
        response.setCategory(transaction.getCategory());
        response.setDescription(transaction.getDescription());
        return response;
    }

    /**
     * Every project needs exactly one budget row. This creates it the first
     * time, and keeps the total in step when the project budget is edited.
     */
    @Transactional
    public ProjectBudget createOrUpdateBudgetFor(Project project) {
        ProjectBudget budget = budgetRepository.findByProjectId(project.getId()).orElse(null);
        if (budget == null) {
            budget = new ProjectBudget();
            budget.setProject(project);
            budget.setCurrentSpending(BigDecimal.ZERO);
        }
        budget.setTotalBudget(project.getBudget());
        return budgetRepository.save(budget);
    }

    private ProjectBudget findBudgetOrThrow(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        return budgetRepository.findByProjectId(projectId)
                .orElseGet(() -> createOrUpdateBudgetFor(project));
    }

    private LocalDate findFirstSpendDate(ProjectBudget budget) {
        List<BudgetTransaction> transactions =
                transactionRepository.findByProjectBudgetIdOrderByTransactionDateAsc(budget.getId());
        if (!transactions.isEmpty()) {
            return transactions.get(0).getTransactionDate();
        }
        // spending was set without individual entries - fall back to the project start
        return budget.getCurrentSpending().compareTo(BigDecimal.ZERO) > 0
                ? budget.getProject().getStartDate() : null;
    }
}
