package edu.sist.prs.engine;

import edu.sist.prs.engine.model.AllocationSnapshot;
import edu.sist.prs.engine.model.MilestoneDeliveryForecast;
import edu.sist.prs.engine.model.MilestoneSnapshot;
import edu.sist.prs.engine.model.ResourceSnapshot;
import edu.sist.prs.engine.model.UnavailabilitySnapshot;
import edu.sist.prs.entity.ResourceType;
import edu.sist.prs.util.WorkingDays;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pure calculation engine for forecasting milestone delivery dates based on
 * remaining effort, allocated developer bandwidth, skill match speed factors,
 * and working days.
 * Adheres to Section 9.7 and Section 16.1.
 */
public final class DeliveryForecaster {

    private DeliveryForecaster() {}

    /**
     * eff(r, m) = 0.5 + 0.5 * match(r, m) / 100.0
     */
    public static double calculateSpeedFactor(ResourceSnapshot resource, MilestoneSnapshot milestone) {
        double match = SkillMatcher.calculateMatchPercentage(resource, milestone);
        return 0.5 + (0.5 * (match / 100.0));
    }

    /**
     * Forecasts delivery finish date and delays for a milestone.
     */
    public static MilestoneDeliveryForecast forecastMilestone(
            MilestoneSnapshot milestone,
            Collection<AllocationSnapshot> allocations,
            Collection<ResourceSnapshot> resources,
            Collection<UnavailabilitySnapshot> unavailabilities,
            LocalDate today,
            int hoursPerDay,
            int maxForecastDays
    ) {
        Map<Long, ResourceSnapshot> resourceMap = resources.stream()
                .collect(Collectors.toMap(ResourceSnapshot::id, Function.identity(), (a, b) -> a));

        // Filter active developer allocations for this milestone
        List<AllocationSnapshot> devAllocs = allocations.stream()
                .filter(a -> a.milestoneId().equals(milestone.id()))
                .filter(a -> !CapacityCalculator.isExcludedByRuleR9(a))
                .filter(a -> {
                    ResourceSnapshot r = resourceMap.get(a.resourceId());
                    return r != null && r.type() == ResourceType.DEVELOPER;
                })
                .toList();

        BigDecimal estEffort = milestone.estimatedEffortHours();
        double remainingRatio = (100.0 - milestone.progressPct()) / 100.0;
        BigDecimal remainingEffort = estEffort.multiply(BigDecimal.valueOf(remainingRatio))
                .setScale(2, RoundingMode.HALF_UP);

        if (remainingEffort.compareTo(BigDecimal.ZERO) <= 0) {
            return new MilestoneDeliveryForecast(
                    milestone.id(),
                    estEffort,
                    BigDecimal.ZERO,
                    today,
                    0,
                    "COMPLETED",
                    "Milestone progress is complete",
                    false
            );
        }

        if (devAllocs.isEmpty()) {
            return new MilestoneDeliveryForecast(
                    milestone.id(),
                    estEffort,
                    remainingEffort,
                    null,
                    0,
                    "NO_CAPACITY",
                    "Cannot forecast: no working capacity",
                    false
            );
        }

        LocalDate startDay = milestone.startDate().isAfter(today) ? milestone.startDate() : today;
        LocalDate current = startDay;
        double accumulatedHours = 0.0;
        double targetHours = remainingEffort.doubleValue();
        boolean extended = false;
        int daysWalked = 0;

        LocalDate forecastFinish = null;

        while (daysWalked < maxForecastDays) {
            if (WorkingDays.isWorkingDay(current)) {
                double dayHours = 0.0;
                for (AllocationSnapshot a : devAllocs) {
                    ResourceSnapshot r = resourceMap.get(a.resourceId());
                    if (r == null) continue;

                    int cap = CapacityCalculator.calculateCapacity(r, current, unavailabilities);
                    if (cap > 0) {
                        // Check if within allocation dates or extended beyond end date
                        boolean inRange = !current.isBefore(a.startDate()) && !current.isAfter(a.endDate());
                        boolean isExt = current.isAfter(a.endDate());

                        if (inRange || isExt) {
                            if (isExt) {
                                extended = true;
                            }
                            double eff = calculateSpeedFactor(r, milestone);
                            double h = ((double) a.allocationPct() / 100.0) * hoursPerDay * eff;
                            dayHours += h;
                        }
                    }
                }
                accumulatedHours += dayHours;
                if (accumulatedHours >= targetHours) {
                    forecastFinish = current;
                    break;
                }
            }
            current = current.plusDays(1);
            daysWalked++;
        }

        if (forecastFinish == null) {
            return new MilestoneDeliveryForecast(
                    milestone.id(),
                    estEffort,
                    remainingEffort,
                    null,
                    0,
                    "NO_CAPACITY",
                    "Cannot forecast: no capacity accumulated within forecast limit",
                    extended
            );
        }

        long delayWorkingDays = 0;
        if (forecastFinish.isAfter(milestone.endDate())) {
            // Count working days strictly after end_date up to and including forecastFinish
            delayWorkingDays = WorkingDays.countWorkingDays(milestone.endDate().plusDays(1), forecastFinish);
        }

        String status = delayWorkingDays > 0 ? "DELAYED" : "ON_TRACK";
        String message = delayWorkingDays > 0
                ? String.format("Milestone is projected to complete on %s (%d working days delayed).", forecastFinish, delayWorkingDays)
                : String.format("Milestone is projected to complete on track by %s.", forecastFinish);

        return new MilestoneDeliveryForecast(
                milestone.id(),
                estEffort,
                remainingEffort,
                forecastFinish,
                delayWorkingDays,
                status,
                message,
                extended
        );
    }
}
