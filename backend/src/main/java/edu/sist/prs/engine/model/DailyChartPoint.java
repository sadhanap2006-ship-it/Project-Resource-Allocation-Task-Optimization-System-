package edu.sist.prs.engine.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyChartPoint(
        LocalDate date,
        BigDecimal actualCumulative,
        BigDecimal projectedCumulative,
        BigDecimal plannedValueCumulative,
        BigDecimal budget,
        BigDecimal remainingBudget
) {
    @com.fasterxml.jackson.annotation.JsonProperty("budgetRemaining")
    public BigDecimal budgetRemaining() {
        return remainingBudget;
    }
}

