package edu.sist.prs.engine;

import edu.sist.prs.engine.model.AllocationSnapshot;
import edu.sist.prs.engine.model.BudgetForecastResult;
import edu.sist.prs.engine.model.BudgetTransactionSnapshot;
import edu.sist.prs.engine.model.DailyChartPoint;
import edu.sist.prs.engine.model.MilestoneSnapshot;
import edu.sist.prs.engine.model.ResourceSnapshot;
import edu.sist.prs.engine.model.UnavailabilitySnapshot;
import edu.sist.prs.entity.ResourceType;
import edu.sist.prs.entity.TransactionCategory;
import edu.sist.prs.util.WorkingDays;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pure calculation engine for project budgets, burn rates, forecasts,
 * Earned Value Management (EVM), and burn-down chart time series.
 * Adheres to Section 9.6, Rule R8, and Section 16.1.
 */
public final class BudgetCalculator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private BudgetCalculator() {}

    /**
     * Calculates planned cost of an allocation on a given resource across a date range.
     */
    public static BigDecimal calculateAllocationPlannedCost(
            AllocationSnapshot alloc,
            ResourceSnapshot resource,
            Collection<UnavailabilitySnapshot> unavailabilities,
            int hoursPerDay,
            LocalDate limitStart,
            LocalDate limitEnd
    ) {
        if (CapacityCalculator.isExcludedByRuleR9(alloc)) {
            return BigDecimal.ZERO;
        }

        LocalDate start = alloc.startDate().isAfter(limitStart) ? alloc.startDate() : limitStart;
        LocalDate end = alloc.endDate().isBefore(limitEnd) ? alloc.endDate() : limitEnd;

        if (end.isBefore(start)) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal allocPctRatio = BigDecimal.valueOf(alloc.allocationPct())
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

        LocalDate current = start;
        while (!current.isAfter(end)) {
            int cap = CapacityCalculator.calculateCapacity(resource, current, unavailabilities);
            if (cap > 0) {
                if (resource.type() == ResourceType.DEVELOPER) {
                    if (WorkingDays.isWorkingDay(current)) {
                        // a.pct/100 * hours.per.day * cost_rate
                        BigDecimal dayCost = allocPctRatio
                                .multiply(BigDecimal.valueOf(hoursPerDay))
                                .multiply(resource.costRate());
                        total = total.add(dayCost);
                    }
                } else if (resource.type() == ResourceType.HARDWARE) {
                    // a.pct/100 * cost_rate
                    BigDecimal dayCost = allocPctRatio.multiply(resource.costRate());
                    total = total.add(dayCost);
                } else if (resource.type() == ResourceType.CLOUD) {
                    // a.pct/100 * daily_quota_units * cost_rate
                    BigDecimal units = allocPctRatio.multiply(resource.dailyQuotaUnits() != null ? resource.dailyQuotaUnits() : BigDecimal.ZERO);
                    BigDecimal dayCost = units.multiply(resource.costRate());
                    total = total.add(dayCost);
                }
            }
            current = current.plusDays(1);
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates planned cost of a milestone.
     */
    public static BigDecimal calculateMilestonePlannedCost(
            Long milestoneId,
            Collection<AllocationSnapshot> allocations,
            Map<Long, ResourceSnapshot> resourceMap,
            Collection<UnavailabilitySnapshot> unavailabilities,
            int hoursPerDay
    ) {
        BigDecimal total = BigDecimal.ZERO;
        for (AllocationSnapshot a : allocations) {
            if (a.milestoneId().equals(milestoneId)) {
                ResourceSnapshot r = resourceMap.get(a.resourceId());
                if (r != null) {
                    total = total.add(calculateAllocationPlannedCost(
                            a, r, unavailabilities, hoursPerDay, a.startDate(), a.endDate()
                    ));
                }
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Full project budget forecast, burn rate, EVM metrics, and chart series.
     */
    public static BudgetForecastResult calculateProjectForecast(
            Long projectId,
            String projectName,
            BigDecimal totalBudget,
            LocalDate projectStartDate,
            LocalDate projectEndDate,
            Collection<MilestoneSnapshot> milestones,
            Collection<AllocationSnapshot> allocations,
            Collection<ResourceSnapshot> resources,
            Collection<UnavailabilitySnapshot> unavailabilities,
            Collection<BudgetTransactionSnapshot> transactions,
            LocalDate today,
            int burnWindowSettingDays,
            int hoursPerDay
    ) {
        Map<Long, ResourceSnapshot> resourceMap = resources.stream()
                .collect(Collectors.toMap(ResourceSnapshot::id, Function.identity(), (a, b) -> a));

        // 1. Actual spend (txnDate <= today) and spendByCategory
        BigDecimal actualSpend = BigDecimal.ZERO;
        Map<TransactionCategory, BigDecimal> spendByCategory = new EnumMap<>(TransactionCategory.class);
        for (TransactionCategory cat : TransactionCategory.values()) {
            spendByCategory.put(cat, BigDecimal.ZERO);
        }

        LocalDate firstTxnDate = null;
        List<BudgetTransactionSnapshot> projectTxns = new ArrayList<>();

        for (BudgetTransactionSnapshot t : transactions) {
            if (t.projectId().equals(projectId) && !t.txnDate().isAfter(today)) {
                projectTxns.add(t);
                actualSpend = actualSpend.add(t.amount());
                spendByCategory.put(t.category(), spendByCategory.get(t.category()).add(t.amount()));

                if (firstTxnDate == null || t.txnDate().isBefore(firstTxnDate)) {
                    firstTxnDate = t.txnDate();
                }
            }
        }
        actualSpend = actualSpend.setScale(2, RoundingMode.HALF_UP);

        // 2. Remaining and Utilisation
        BigDecimal remaining = totalBudget.subtract(actualSpend).setScale(2, RoundingMode.HALF_UP);
        Double utilisationPct = totalBudget.compareTo(BigDecimal.ZERO) > 0
                ? actualSpend.multiply(BigDecimal.valueOf(100)).divide(totalBudget, 2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        // 3. Burn Rate window W
        int windowDays = burnWindowSettingDays;
        if (firstTxnDate != null) {
            long daysSinceFirst = ChronoUnit.DAYS.between(firstTxnDate, today) + 1;
            if (daysSinceFirst < windowDays) {
                windowDays = Math.max(1, (int) daysSinceFirst);
            }
        }

        LocalDate windowStart = today.minusDays(windowDays - 1);
        BigDecimal windowSpend = BigDecimal.ZERO;
        for (BudgetTransactionSnapshot t : projectTxns) {
            if (!t.txnDate().isBefore(windowStart) && !t.txnDate().isAfter(today)) {
                windowSpend = windowSpend.add(t.amount());
            }
        }

        BigDecimal burnRate = windowDays > 0
                ? windowSpend.divide(BigDecimal.valueOf(windowDays), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 4. Days Left & Run Out Date
        Double daysLeft = null;
        LocalDate runOutDate = null;
        String forecastStatus;
        String message;

        if (projectTxns.isEmpty() || burnRate.compareTo(BigDecimal.ZERO) <= 0) {
            forecastStatus = "INSUFFICIENT_DATA";
            message = "Insufficient spending data";
        } else if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            forecastStatus = "EXHAUSTED";
            message = "Budget exhausted";
            daysLeft = 0.0;
            runOutDate = today;
        } else if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            forecastStatus = "EXCEEDED";
            message = "Budget exceeded by ₹" + remaining.abs().toPlainString();
            daysLeft = 0.0;
            runOutDate = today;
        } else {
            daysLeft = remaining.divide(burnRate, 4, RoundingMode.HALF_UP).doubleValue();
            long daysCeil = (long) Math.ceil(daysLeft);
            runOutDate = today.plusDays(daysCeil);

            if (runOutDate.isBefore(projectEndDate)) {
                forecastStatus = "EARLY_RUNOUT";
            } else {
                forecastStatus = "ON_TRACK";
            }
            message = String.format(
                    "At the current burn rate, %s is projected to exhaust its budget around %s.",
                    projectName,
                    runOutDate.format(DATE_FMT)
            );
        }

        // 5. Earned Value (PV, EV, CPI, SPI, EAC) and Milestone planned costs
        BigDecimal planTotal = BigDecimal.ZERO;
        BigDecimal pv = BigDecimal.ZERO;
        BigDecimal ev = BigDecimal.ZERO;

        Map<Long, BigDecimal> milestonePlanMap = new HashMap<>();
        for (MilestoneSnapshot m : milestones) {
            if (m.projectId().equals(projectId)) {
                BigDecimal mPlan = calculateMilestonePlannedCost(
                        m.id(), allocations, resourceMap, unavailabilities, hoursPerDay
                );
                milestonePlanMap.put(m.id(), mPlan);
                planTotal = planTotal.add(mPlan);

                BigDecimal mEv = mPlan.multiply(BigDecimal.valueOf(m.progressPct()))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                ev = ev.add(mEv);
            }
        }

        for (AllocationSnapshot a : allocations) {
            if (projectId.equals(a.projectId())) {
                ResourceSnapshot r = resourceMap.get(a.resourceId());
                if (r != null) {
                    BigDecimal aPv = calculateAllocationPlannedCost(
                            a, r, unavailabilities, hoursPerDay, a.startDate(), today
                    );
                    pv = pv.add(aPv);
                }
            }
        }
        pv = pv.setScale(2, RoundingMode.HALF_UP);
        ev = ev.setScale(2, RoundingMode.HALF_UP);
        planTotal = planTotal.setScale(2, RoundingMode.HALF_UP);

        Double cpi = (actualSpend.compareTo(BigDecimal.ZERO) > 0)
                ? ev.divide(actualSpend, 4, RoundingMode.HALF_UP).doubleValue()
                : null;

        Double spi = (pv.compareTo(BigDecimal.ZERO) > 0)
                ? ev.divide(pv, 4, RoundingMode.HALF_UP).doubleValue()
                : null;

        BigDecimal eac = (ev.compareTo(BigDecimal.ZERO) > 0 && actualSpend.compareTo(BigDecimal.ZERO) > 0)
                ? totalBudget.multiply(actualSpend).divide(ev, 2, RoundingMode.HALF_UP)
                : null;

        // 6. Generate Time Series for charts
        LocalDate chartEnd = projectEndDate;
        if (runOutDate != null && runOutDate.isAfter(chartEnd)) {
            chartEnd = runOutDate;
        }

        List<DailyChartPoint> series = new ArrayList<>();
        List<DailyChartPoint> remainingSeries = new ArrayList<>();

        Map<LocalDate, BigDecimal> dailyActualMap = new HashMap<>();
        for (BudgetTransactionSnapshot t : projectTxns) {
            dailyActualMap.merge(t.txnDate(), t.amount(), BigDecimal::add);
        }

        BigDecimal cumActual = BigDecimal.ZERO;
        BigDecimal cumProjected = BigDecimal.ZERO;
        BigDecimal cumPv = BigDecimal.ZERO;

        LocalDate cur = projectStartDate;
        while (!cur.isAfter(chartEnd)) {
            // Actual spend up to today
            if (!cur.isAfter(today)) {
                cumActual = cumActual.add(dailyActualMap.getOrDefault(cur, BigDecimal.ZERO));
                cumProjected = cumActual;
            } else {
                if (burnRate.compareTo(BigDecimal.ZERO) > 0) {
                    cumProjected = cumProjected.add(burnRate);
                }
            }

            // Planned value cumulative on cur
            BigDecimal dayPv = BigDecimal.ZERO;
            for (AllocationSnapshot a : allocations) {
                if (projectId.equals(a.projectId())) {
                    ResourceSnapshot r = resourceMap.get(a.resourceId());
                    if (r != null) {
                        dayPv = dayPv.add(calculateAllocationPlannedCost(a, r, unavailabilities, hoursPerDay, cur, cur));
                    }
                }
            }
            cumPv = cumPv.add(dayPv);

            BigDecimal ptActual = !cur.isAfter(today) ? cumActual.setScale(2, RoundingMode.HALF_UP) : null;
            BigDecimal ptProj = cur.isAfter(today) ? cumProjected.setScale(2, RoundingMode.HALF_UP) : cumActual.setScale(2, RoundingMode.HALF_UP);
            BigDecimal ptPv = cumPv.setScale(2, RoundingMode.HALF_UP);
            BigDecimal ptRemaining = totalBudget.subtract(ptProj).setScale(2, RoundingMode.HALF_UP);

            DailyChartPoint point = new DailyChartPoint(
                    cur,
                    ptActual,
                    ptProj,
                    ptPv,
                    totalBudget,
                    ptRemaining
            );
            series.add(point);
            remainingSeries.add(point);

            cur = cur.plusDays(1);
        }

        return new BudgetForecastResult(
                totalBudget,
                actualSpend,
                spendByCategory,
                remaining,
                utilisationPct,
                burnRate,
                windowDays,
                daysLeft,
                runOutDate,
                forecastStatus,
                message,
                planTotal,
                pv,
                ev,
                cpi,
                spi,
                eac,
                series,
                remainingSeries
        );
    }
}
