package service;

import dao.BudgetTransactionDAO;
import dao.ProjectDAO;
import dto.BudgetResponseDTO;
import model.BudgetTransaction;
import model.Project;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class BudgetService {

    private final ProjectDAO projectDAO = new ProjectDAO();
    private final BudgetTransactionDAO transactionDAO = new BudgetTransactionDAO();

    private static final int ROLLING_WINDOW_DAYS = 30;

    public BudgetResponseDTO calculateProjectBudget(int projectId) throws SQLException {
        Project p = projectDAO.findById(projectId);
        if (p == null) return null;

        double totalBudget = p.getTotalBudget();
        double currentSpend = p.getCurrentSpend();
        double remaining = Math.max(0, totalBudget - currentSpend);

        LocalDate startDate = LocalDate.parse(p.getStartDate());
        LocalDate endDate = LocalDate.parse(p.getEndDate());
        LocalDate today = LocalDate.now();

        long totalDurationDays = Math.max(1, ChronoUnit.DAYS.between(startDate, endDate));
        long elapsedDays = 0;
        if (today.isAfter(startDate)) {
            elapsedDays = ChronoUnit.DAYS.between(startDate, today.isBefore(endDate) ? today : endDate);
        }
        if (elapsedDays <= 0) elapsedDays = 1;

        // Fetch recent transactions over rolling window
        List<BudgetTransaction> recentTxs = transactionDAO.findRecentByProjectId(projectId, ROLLING_WINDOW_DAYS);
        List<BudgetResponseDTO.BudgetTransactionDTO> txDTOs = new ArrayList<>();

        double recentSpend = 0.0;
        LocalDate earliestTxDate = today;
        for (BudgetTransaction t : recentTxs) {
            recentSpend += t.getAmount();
            LocalDate txDate = LocalDate.parse(t.getTransactionDate());
            if (txDate.isBefore(earliestTxDate)) {
                earliestTxDate = txDate;
            }
            txDTOs.add(new BudgetResponseDTO.BudgetTransactionDTO(
                    t.getId(), t.getProjectId(), t.getAmount(), t.getTransactionDate(), t.getDescription()
            ));
        }

        double dailyBurnRate;
        long daysUntilExhaustion;
        String exhaustionDate;

        if (recentTxs.isEmpty()) {
            if (currentSpend <= 0) {
                dailyBurnRate = 0.0;
                daysUntilExhaustion = -1;
                exhaustionDate = "N/A (No Spend Recorded)";
            } else {
                // Fallback when current_spend > 0 but no transaction rows exist in the rolling window
                dailyBurnRate = currentSpend / elapsedDays;
                if (dailyBurnRate > 0 && remaining > 0) {
                    daysUntilExhaustion = (long) Math.ceil(remaining / dailyBurnRate);
                    exhaustionDate = today.plusDays(daysUntilExhaustion).toString();
                } else if (remaining <= 0) {
                    exhaustionDate = "EXHAUSTED";
                    daysUntilExhaustion = 0;
                } else {
                    dailyBurnRate = 0.0;
                    daysUntilExhaustion = -1;
                    exhaustionDate = "Insufficient spending data";
                }
            }
        } else {
            // burnRate = recentSpend / elapsedTime in window
            long windowElapsedDays = ChronoUnit.DAYS.between(earliestTxDate, today);
            if (windowElapsedDays <= 0) windowElapsedDays = 1;

            dailyBurnRate = recentSpend / windowElapsedDays;

            if (remaining <= 0) {
                exhaustionDate = "EXHAUSTED";
                daysUntilExhaustion = 0;
            } else if (dailyBurnRate > 0) {
                daysUntilExhaustion = (long) Math.ceil(remaining / dailyBurnRate);
                exhaustionDate = today.plusDays(daysUntilExhaustion).toString();
            } else {
                daysUntilExhaustion = -1;
                exhaustionDate = "Insufficient spending data";
            }
        }

        double utilPct = totalBudget > 0 ? (currentSpend / totalBudget) * 100.0 : 0;
        String budgetStatus = utilPct > 90 ? "CRITICAL" : (utilPct > 75 ? "AT_RISK" : "HEALTHY");

        BudgetResponseDTO dto = new BudgetResponseDTO();
        dto.setProjectId(p.getId());
        dto.setProjectName(p.getName());
        dto.setTotalBudget(totalBudget);
        dto.setCurrentSpend(currentSpend);
        dto.setRemainingBudget(remaining);
        dto.setElapsedDays(elapsedDays);
        dto.setTotalDurationDays(totalDurationDays);
        dto.setDailyBurnRate(Math.round(dailyBurnRate * 100.0) / 100.0);
        dto.setDaysUntilExhaustion(daysUntilExhaustion);
        dto.setForecastExhaustionDate(exhaustionDate);
        dto.setBudgetUtilizationPercentage(Math.round(utilPct * 10.0) / 10.0);
        dto.setBudgetStatus(budgetStatus);
        dto.setRecentTransactions(txDTOs);

        return dto;
    }
}
