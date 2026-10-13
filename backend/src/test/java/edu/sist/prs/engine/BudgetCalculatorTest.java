package edu.sist.prs.engine;

import edu.sist.prs.engine.model.AllocationSnapshot;
import edu.sist.prs.engine.model.BudgetForecastResult;
import edu.sist.prs.engine.model.BudgetTransactionSnapshot;
import edu.sist.prs.engine.model.MilestoneSnapshot;
import edu.sist.prs.engine.model.ResourceSnapshot;
import edu.sist.prs.entity.CostUnit;
import edu.sist.prs.entity.MilestoneStatus;
import edu.sist.prs.entity.ProjectStatus;
import edu.sist.prs.entity.ResourceStatus;
import edu.sist.prs.entity.ResourceType;
import edu.sist.prs.entity.TransactionCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class BudgetCalculatorTest {

    @Test
    @DisplayName("Budget Example 5: total 500k, actual 300k, 14-day spend 140k -> remaining 200k, utilisation 60%, burn 10k/day, daysLeft 20, runOut = today + 20")
    void testBudgetExample5() {
        LocalDate today = LocalDate.of(2027, 2, 1);
        LocalDate start = today.minusDays(60);
        LocalDate end = today.plusDays(60);

        List<BudgetTransactionSnapshot> txns = new ArrayList<>();
        // 8 older txns totaling 160,000
        for (int i = 0; i < 8; i++) {
            txns.add(new BudgetTransactionSnapshot(
                    (long) i, 1L, today.minusDays(20 + i * 2), BigDecimal.valueOf(20000),
                    TransactionCategory.LABOUR, 1L, "Historical"
            ));
        }

        // 14 days in window totaling 140,000 (10,000 per day)
        for (int i = 0; i < 14; i++) {
            txns.add(new BudgetTransactionSnapshot(
                    (long) (100 + i), 1L, today.minusDays(i), BigDecimal.valueOf(10000),
                    TransactionCategory.LABOUR, 1L, "Recent"
            ));
        }

        BudgetForecastResult result = BudgetCalculator.calculateProjectForecast(
                1L, "ALPHA", BigDecimal.valueOf(500000), start, end,
                List.of(), List.of(), List.of(), List.of(),
                txns, today, 14, 8
        );

        assertThat(result.actualSpend()).isEqualByComparingTo("300000.00");
        assertThat(result.remaining()).isEqualByComparingTo("200000.00");
        assertThat(result.utilisationPct()).isEqualTo(60.0);
        assertThat(result.burnRate()).isEqualByComparingTo("10000.00");
        assertThat(result.daysLeft()).isEqualTo(20.0);
        assertThat(result.runOutDate()).isEqualTo(today.plusDays(20));
    }

    @Test
    @DisplayName("EVM Worked Example: PV 320k, EV 280k, actual 300k -> CPI 0.9333, SPI 0.875, EAC 535,714.29")
    void testEvmMetrics() {
        LocalDate today = LocalDate.of(2027, 2, 1); // Monday
        BigDecimal totalBudget = BigDecimal.valueOf(500000);
        BigDecimal actualSpend = BigDecimal.valueOf(300000);

        // CPI = 280,000 / 300,000 = 0.9333
        // SPI = 280,000 / 320,000 = 0.875
        // EAC = 500,000 / 0.933333 = 535,714.29
        double cpi = BigDecimal.valueOf(280000).divide(actualSpend, 4, java.math.RoundingMode.HALF_UP).doubleValue();
        double spi = BigDecimal.valueOf(280000).divide(BigDecimal.valueOf(320000), 4, java.math.RoundingMode.HALF_UP).doubleValue();
        BigDecimal eac = totalBudget.multiply(actualSpend).divide(BigDecimal.valueOf(280000), 2, java.math.RoundingMode.HALF_UP);

        assertThat(cpi).isCloseTo(0.9333, within(0.0001));
        assertThat(spi).isCloseTo(0.8750, within(0.0001));
        assertThat(eac).isEqualByComparingTo("535714.29");
    }

    @Test
    @DisplayName("Budget edge cases: no transactions -> INSUFFICIENT_DATA, negative remaining -> EXCEEDED")
    void testBudgetEdgeCases() {
        LocalDate today = LocalDate.of(2027, 2, 1);

        // 1. No transactions
        BudgetForecastResult noTxns = BudgetCalculator.calculateProjectForecast(
                1L, "Proj", BigDecimal.valueOf(100000), today.minusDays(10), today.plusDays(10),
                List.of(), List.of(), List.of(), List.of(),
                List.of(), today, 14, 8
        );
        assertThat(noTxns.forecastStatus()).isEqualTo("INSUFFICIENT_DATA");
        assertThat(noTxns.runOutDate()).isNull();

        // 2. Exceeded budget
        BudgetTransactionSnapshot bigTxn = new BudgetTransactionSnapshot(
                1L, 1L, today, BigDecimal.valueOf(150000), TransactionCategory.LABOUR, 1L, "Big spend"
        );
        BudgetForecastResult exceeded = BudgetCalculator.calculateProjectForecast(
                1L, "Proj", BigDecimal.valueOf(100000), today.minusDays(10), today.plusDays(10),
                List.of(), List.of(), List.of(), List.of(),
                List.of(bigTxn), today, 14, 8
        );
        assertThat(exceeded.forecastStatus()).isEqualTo("EXCEEDED");
        assertThat(exceeded.remaining()).isEqualByComparingTo("-50000.00");
    }
}
