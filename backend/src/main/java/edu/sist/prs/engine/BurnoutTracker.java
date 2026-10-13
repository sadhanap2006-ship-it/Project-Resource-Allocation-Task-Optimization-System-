package edu.sist.prs.engine;

import edu.sist.prs.engine.model.AllocationSnapshot;
import edu.sist.prs.engine.model.DetectedConflict;
import edu.sist.prs.engine.model.ResourceSnapshot;
import edu.sist.prs.engine.model.UnavailabilitySnapshot;
import edu.sist.prs.entity.ConflictSeverity;
import edu.sist.prs.entity.ResourceType;
import edu.sist.prs.util.WorkingDays;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure calculation engine for detecting developer burnout (sustained load).
 * Adheres to Section 9.4:
 * - Weeks are ISO weeks (Monday–Sunday).
 * - Weekly load(r, w) = average of load(r, d) over working days d in w where capacity(r, d) > 0.
 * - Weeks with no such days break a streak.
 * - If weekly load >= burnout.threshold.pct for >= burnout.consecutive.weeks consecutive weeks
 *   -> BURNOUT_RISK (one conflict per streak, keyed by the streak's first Monday).
 * - DEVELOPER resources only.
 */
public final class BurnoutTracker {

    private BurnoutTracker() {}

    public static List<DetectedConflict> detectBurnout(
            ResourceSnapshot resource,
            LocalDate scanStart,
            LocalDate scanEnd,
            Collection<AllocationSnapshot> allocations,
            Collection<UnavailabilitySnapshot> unavailabilities,
            double thresholdPct,
            int consecutiveWeeksRequired
    ) {
        if (resource.type() != ResourceType.DEVELOPER) {
            return List.of();
        }

        List<DetectedConflict> conflicts = new ArrayList<>();
        Set<LocalDate> reportedStreakMondays = new HashSet<>();

        LocalDate currentMonday = scanStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastMonday = scanEnd.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        int currentStreak = 0;
        LocalDate streakFirstMonday = null;
        double maxWeeklyLoadInStreak = 0.0;

        while (!currentMonday.isAfter(lastMonday)) {
            LocalDate sunday = currentMonday.plusDays(6);

            // Compute average load on working days in this week where capacity > 0
            int validWorkingDays = 0;
            int totalLoad = 0;

            LocalDate d = currentMonday;
            while (!d.isAfter(sunday)) {
                if (WorkingDays.isWorkingDay(d)) {
                    int cap = CapacityCalculator.calculateCapacity(resource, d, unavailabilities);
                    if (cap > 0) {
                        validWorkingDays++;
                        totalLoad += CapacityCalculator.calculateLoad(resource.id(), d, allocations);
                    }
                }
                d = d.plusDays(1);
            }

            if (validWorkingDays == 0) {
                // Weeks with no working capacity break a streak
                currentStreak = 0;
                streakFirstMonday = null;
                maxWeeklyLoadInStreak = 0.0;
            } else {
                double avgLoad = (double) totalLoad / (double) validWorkingDays;
                if (avgLoad >= thresholdPct) {
                    if (currentStreak == 0) {
                        streakFirstMonday = currentMonday;
                        maxWeeklyLoadInStreak = avgLoad;
                    } else {
                        if (avgLoad > maxWeeklyLoadInStreak) {
                            maxWeeklyLoadInStreak = avgLoad;
                        }
                    }
                    currentStreak++;

                    if (currentStreak >= consecutiveWeeksRequired && !reportedStreakMondays.contains(streakFirstMonday)) {
                        reportedStreakMondays.add(streakFirstMonday);
                        String dedupKey = "BURNOUT:" + resource.id() + ":" + streakFirstMonday;
                        String desc = String.format(
                                "%s has sustained an average load of %.1f%% across %d consecutive weeks starting %s.",
                                resource.name(),
                                maxWeeklyLoadInStreak,
                                currentStreak,
                                streakFirstMonday
                        );

                        conflicts.add(new DetectedConflict(
                                ConflictTypeConstants.BURNOUT_RISK,
                                ConflictSeverity.WARNING,
                                null,
                                null,
                                resource.id(),
                                streakFirstMonday,
                                sunday,
                                BigDecimal.valueOf(maxWeeklyLoadInStreak).setScale(2, RoundingMode.HALF_UP),
                                desc,
                                dedupKey
                        ));
                    }
                } else {
                    currentStreak = 0;
                    streakFirstMonday = null;
                    maxWeeklyLoadInStreak = 0.0;
                }
            }

            currentMonday = currentMonday.plusWeeks(1);
        }

        return conflicts;
    }
}
