package edu.sist.prs.engine.model;

import edu.sist.prs.entity.TransactionCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record BudgetForecastResult(
        BigDecimal totalBudget,
        BigDecimal actualSpend,
        Map<TransactionCategory, BigDecimal> spendByCategory,
        BigDecimal remaining,
        Double utilisationPct,
        BigDecimal burnRate,
        int windowDays,
        Double daysLeft,
        LocalDate runOutDate,
        String forecastStatus,
        String message,
        BigDecimal planTotal,
        BigDecimal pv,
        BigDecimal ev,
        Double cpi,
        Double spi,
        BigDecimal eac,
        List<DailyChartPoint> series,
        List<DailyChartPoint> remainingSeries
) {}
