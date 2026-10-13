package edu.sist.prs.engine;

import edu.sist.prs.engine.model.AllocationSnapshot;
import edu.sist.prs.engine.model.DetectedConflict;
import edu.sist.prs.engine.model.MilestoneSnapshot;
import edu.sist.prs.engine.model.OverAllocationPeriod;
import edu.sist.prs.engine.model.ResourceSkillSnapshot;
import edu.sist.prs.engine.model.ResourceSnapshot;
import edu.sist.prs.engine.model.SkillRequirementSnapshot;
import edu.sist.prs.engine.model.UnavailabilitySnapshot;
import edu.sist.prs.entity.ConflictSeverity;
import edu.sist.prs.entity.MilestoneStatus;
import edu.sist.prs.entity.ResourceStatus;
import edu.sist.prs.entity.ResourceType;
import edu.sist.prs.entity.UnavailabilityStatus;
import edu.sist.prs.util.DateRanges;
import edu.sist.prs.util.WorkingDays;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pure calculation engine for detecting conflicts across resources, milestones, and allocations.
 * Implements conflict detection rules from Section 9.2 and Section 16.1.
 */
public final class ConflictRules {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private ConflictRules() {}

    /**
     * Runs conflict detection across given resources and milestones in the scan window.
     */
    public static List<DetectedConflict> evaluateConflicts(
            Collection<ResourceSnapshot> resources,
            Collection<MilestoneSnapshot> milestones,
            Collection<AllocationSnapshot> allocations,
            Collection<UnavailabilitySnapshot> unavailabilities,
            LocalDate scanStart,
            LocalDate scanEnd,
            LocalDate today,
            double burnoutThresholdPct,
            int burnoutConsecutiveWeeks,
            double skillMismatchThresholdPct,
            int unstaffedLookaheadDays
    ) {
        List<DetectedConflict> conflicts = new ArrayList<>();
        Map<Long, ResourceSnapshot> resourceMap = resources.stream()
                .collect(Collectors.toMap(ResourceSnapshot::id, Function.identity(), (a, b) -> a));
        Map<Long, MilestoneSnapshot> milestoneMap = milestones.stream()
                .collect(Collectors.toMap(MilestoneSnapshot::id, Function.identity(), (a, b) -> a));

        // 1. OVER_ALLOCATION (DEVELOPER and HARDWARE) & CLOUD_QUOTA (CLOUD)
        for (ResourceSnapshot r : resources) {
            if (r.type() == ResourceType.DEVELOPER || r.type() == ResourceType.HARDWARE) {
                List<OverAllocationPeriod> periods = CapacityCalculator.findOverAllocationPeriods(
                        r, scanStart, scanEnd, allocations, unavailabilities
                );
                for (OverAllocationPeriod p : periods) {
                    String dedupKey = String.format("OVERALLOC:%d:%s:%s", r.id(), p.startDate(), p.endDate());
                    String desc = String.format(
                            "%s is allocated at %d%% between %s and %s (capacity %d%%).",
                            r.name(),
                            p.peakLoad(),
                            p.startDate().format(DATE_FMT),
                            p.endDate().format(DATE_FMT),
                            r.capacityPct()
                    );
                    conflicts.add(new DetectedConflict(
                            ConflictTypeConstants.OVER_ALLOCATION,
                            p.severity(),
                            null,
                            null,
                            r.id(),
                            p.startDate(),
                            p.endDate(),
                            BigDecimal.valueOf(p.peakLoad()),
                            desc,
                            dedupKey
                    ));
                }
            } else if (r.type() == ResourceType.CLOUD) {
                conflicts.addAll(detectCloudQuotaAndCapConflicts(r, scanStart, scanEnd, allocations));
            }
        }

        // 2. LEAVE_CONFLICT
        for (AllocationSnapshot alloc : allocations) {
            if (CapacityCalculator.isExcludedByRuleR9(alloc)) {
                continue;
            }
            ResourceSnapshot res = resourceMap.get(alloc.resourceId());
            if (res == null) continue;

            for (UnavailabilitySnapshot unavail : unavailabilities) {
                if (!unavail.resourceId().equals(alloc.resourceId())) {
                    continue;
                }
                if (unavail.status() != UnavailabilityStatus.APPROVED) {
                    continue;
                }
                if (DateRanges.overlaps(alloc.startDate(), alloc.endDate(), unavail.startDate(), unavail.endDate())) {
                    // Check if there is an actual overlap on a day with load
                    LocalDate overlapStart = alloc.startDate().isAfter(unavail.startDate()) ? alloc.startDate() : unavail.startDate();
                    LocalDate overlapEnd = alloc.endDate().isBefore(unavail.endDate()) ? alloc.endDate() : unavail.endDate();

                    String dedupKey = String.format("LEAVE:%d:%d", alloc.id(), unavail.id());
                    String desc = String.format(
                            "%s has an approved %s from %s to %s that conflicts with allocation #%d (%d%%).",
                            res.name(),
                            unavail.type(),
                            unavail.startDate().format(DATE_FMT),
                            unavail.endDate().format(DATE_FMT),
                            alloc.id(),
                            alloc.allocationPct()
                    );
                    conflicts.add(new DetectedConflict(
                            ConflictTypeConstants.LEAVE_CONFLICT,
                            ConflictSeverity.CRITICAL,
                            alloc.projectId(),
                            alloc.milestoneId(),
                            res.id(),
                            overlapStart,
                            overlapEnd,
                            BigDecimal.valueOf(alloc.allocationPct()),
                            desc,
                            dedupKey
                    ));
                }
            }
        }

        // 3. INACTIVE_RESOURCE
        for (AllocationSnapshot alloc : allocations) {
            if (CapacityCalculator.isExcludedByRuleR9(alloc)) {
                continue;
            }
            ResourceSnapshot res = resourceMap.get(alloc.resourceId());
            if (res != null && res.status() == ResourceStatus.INACTIVE) {
                String dedupKey = String.format("INACTIVE:%d", alloc.id());
                String desc = String.format(
                        "Allocation #%d is assigned to inactive resource %s.",
                        alloc.id(),
                        res.name()
                );
                conflicts.add(new DetectedConflict(
                        ConflictTypeConstants.INACTIVE_RESOURCE,
                        ConflictSeverity.CRITICAL,
                        alloc.projectId(),
                        alloc.milestoneId(),
                        res.id(),
                        alloc.startDate(),
                        alloc.endDate(),
                        BigDecimal.valueOf(alloc.allocationPct()),
                        desc,
                        dedupKey
                ));
            }
        }

        // 4. SKILL_GAP
        List<ResourceSnapshot> activeDevelopers = resources.stream()
                .filter(r -> r.type() == ResourceType.DEVELOPER && r.status() == ResourceStatus.ACTIVE)
                .toList();

        for (MilestoneSnapshot m : milestones) {
            if (m.status() == MilestoneStatus.COMPLETED) {
                continue;
            }
            for (SkillRequirementSnapshot req : m.skillRequirements()) {
                if (!req.mandatory()) {
                    continue;
                }

                // Find active developers holding this skill at >= minProficiency
                List<ResourceSnapshot> holders = activeDevelopers.stream()
                        .filter(dev -> dev.skills().stream()
                                .anyMatch(s -> s.skillId().equals(req.skillId()) && s.proficiency() >= req.minProficiency()))
                        .toList();

                String dedupKey = String.format("SKILLGAP:%d:%d", m.id(), req.skillId());

                if (holders.isEmpty()) {
                    String desc = String.format(
                            "Milestone '%s' requires mandatory skill '%s' (level %d), but no active developer holds this skill at the required level.",
                            m.name(),
                            req.skillName(),
                            req.minProficiency()
                    );
                    conflicts.add(new DetectedConflict(
                            ConflictTypeConstants.SKILL_GAP,
                            ConflictSeverity.CRITICAL,
                            m.projectId(),
                            m.id(),
                            null,
                            m.startDate(),
                            m.endDate(),
                            null,
                            desc,
                            dedupKey
                    ));
                } else {
                    // Check if EVERY holder has remaining < 1% on EVERY working day of the milestone where capacity > 0
                    boolean allFullyBooked = true;
                    for (ResourceSnapshot holder : holders) {
                        boolean holderHasFreeDay = false;
                        LocalDate current = m.startDate();
                        while (!current.isAfter(m.endDate())) {
                            if (WorkingDays.isWorkingDay(current)) {
                                int cap = CapacityCalculator.calculateCapacity(holder, current, unavailabilities);
                                if (cap > 0) {
                                    int rem = CapacityCalculator.calculateRemaining(holder, current, allocations, unavailabilities);
                                    if (rem >= 1) {
                                        holderHasFreeDay = true;
                                        break;
                                    }
                                }
                            }
                            current = current.plusDays(1);
                        }
                        if (holderHasFreeDay) {
                            allFullyBooked = false;
                            break;
                        }
                    }

                    if (allFullyBooked) {
                        String desc = String.format(
                                "Milestone '%s' requires mandatory skill '%s' (level %d), but all qualifying developers are fully booked across the milestone.",
                                m.name(),
                                req.skillName(),
                                req.minProficiency()
                        );
                        conflicts.add(new DetectedConflict(
                                ConflictTypeConstants.SKILL_GAP,
                                ConflictSeverity.WARNING,
                                m.projectId(),
                                m.id(),
                                null,
                                m.startDate(),
                                m.endDate(),
                                null,
                                desc,
                                dedupKey
                        ));
                    }
                }
            }
        }

        // 5. SKILL_MISMATCH
        for (AllocationSnapshot alloc : allocations) {
            if (CapacityCalculator.isExcludedByRuleR9(alloc)) {
                continue;
            }
            ResourceSnapshot res = resourceMap.get(alloc.resourceId());
            MilestoneSnapshot mil = milestoneMap.get(alloc.milestoneId());
            if (res != null && mil != null && res.type() == ResourceType.DEVELOPER) {
                double match = SkillMatcher.calculateMatchPercentage(res, mil);
                if (match < skillMismatchThresholdPct) {
                    String dedupKey = String.format("MISMATCH:%d", alloc.id());
                    String desc = String.format(
                            "%s has only %.1f%% skill match for milestone '%s' (threshold %.0f%%).",
                            res.name(),
                            match,
                            mil.name(),
                            skillMismatchThresholdPct
                    );
                    conflicts.add(new DetectedConflict(
                            ConflictTypeConstants.SKILL_MISMATCH,
                            ConflictSeverity.WARNING,
                            alloc.projectId(),
                            alloc.milestoneId(),
                            res.id(),
                            alloc.startDate(),
                            alloc.endDate(),
                            BigDecimal.valueOf(match).setScale(2, RoundingMode.HALF_UP),
                            desc,
                            dedupKey
                    ));
                }
            }
        }

        // 6. UNSTAFFED_MILESTONE
        // PLANNED or IN_PROGRESS milestone with start <= today + lookahead and end >= today, and no DEVELOPER allocation
        LocalDate lookaheadDate = today.plusDays(unstaffedLookaheadDays);
        for (MilestoneSnapshot m : milestones) {
            if (m.status() == MilestoneStatus.PLANNED || m.status() == MilestoneStatus.IN_PROGRESS) {
                if (!m.startDate().isAfter(lookaheadDate) && !m.endDate().isBefore(today)) {
                    boolean hasDevAllocation = allocations.stream()
                            .filter(a -> a.milestoneId().equals(m.id()))
                            .filter(a -> !CapacityCalculator.isExcludedByRuleR9(a))
                            .anyMatch(a -> {
                                ResourceSnapshot r = resourceMap.get(a.resourceId());
                                return r != null && r.type() == ResourceType.DEVELOPER;
                            });

                    if (!hasDevAllocation) {
                        String dedupKey = String.format("UNSTAFFED:%d", m.id());
                        String desc = String.format(
                                "Milestone '%s' starts on %s but has no developers assigned.",
                                m.name(),
                                m.startDate().format(DATE_FMT)
                        );
                        conflicts.add(new DetectedConflict(
                                ConflictTypeConstants.UNSTAFFED_MILESTONE,
                                ConflictSeverity.WARNING,
                                m.projectId(),
                                m.id(),
                                null,
                                m.startDate(),
                                m.endDate(),
                                null,
                                desc,
                                dedupKey
                        ));
                    }
                }
            }
        }

        // 7. BURNOUT_RISK
        for (ResourceSnapshot r : resources) {
            if (r.type() == ResourceType.DEVELOPER && r.status() == ResourceStatus.ACTIVE) {
                conflicts.addAll(BurnoutTracker.detectBurnout(
                        r,
                        scanStart,
                        scanEnd,
                        allocations,
                        unavailabilities,
                        burnoutThresholdPct,
                        burnoutConsecutiveWeeks
                ));
            }
        }

        return conflicts;
    }

    /**
     * Detects CLOUD_QUOTA over daily quota or over monthly cap per Section 9.2.
     */
    private static List<DetectedConflict> detectCloudQuotaAndCapConflicts(
            ResourceSnapshot cloud,
            LocalDate scanStart,
            LocalDate scanEnd,
            Collection<AllocationSnapshot> allocations
    ) {
        List<DetectedConflict> conflicts = new ArrayList<>();
        if (cloud.type() != ResourceType.CLOUD || cloud.status() != ResourceStatus.ACTIVE) {
            return conflicts;
        }

        // Daily quota check (merged periods where load > 100%)
        LocalDate currentPeriodStart = null;
        LocalDate currentPeriodEnd = null;
        int currentPeak = 0;

        LocalDate current = scanStart;
        while (!current.isAfter(scanEnd)) {
            int load = CapacityCalculator.calculateLoad(cloud.id(), current, allocations);
            if (load > 100) {
                if (currentPeriodStart == null) {
                    currentPeriodStart = current;
                    currentPeriodEnd = current;
                    currentPeak = load;
                } else {
                    currentPeriodEnd = current;
                    if (load > currentPeak) {
                        currentPeak = load;
                    }
                }
            } else {
                if (currentPeriodStart != null) {
                    String dedupKey = String.format("CLOUDQ:%d:%s:%s", cloud.id(), currentPeriodStart, currentPeriodEnd);
                    String desc = String.format(
                            "Cloud resource %s exceeds daily quota at %d%% between %s and %s.",
                            cloud.name(),
                            currentPeak,
                            currentPeriodStart.format(DATE_FMT),
                            currentPeriodEnd.format(DATE_FMT)
                    );
                    conflicts.add(new DetectedConflict(
                            ConflictTypeConstants.CLOUD_QUOTA,
                            ConflictSeverity.CRITICAL,
                            null,
                            null,
                            cloud.id(),
                            currentPeriodStart,
                            currentPeriodEnd,
                            BigDecimal.valueOf(currentPeak),
                            desc,
                            dedupKey
                    ));
                    currentPeriodStart = null;
                    currentPeriodEnd = null;
                    currentPeak = 0;
                }
            }
            current = current.plusDays(1);
        }

        if (currentPeriodStart != null) {
            String dedupKey = String.format("CLOUDQ:%d:%s:%s", cloud.id(), currentPeriodStart, currentPeriodEnd);
            String desc = String.format(
                    "Cloud resource %s exceeds daily quota at %d%% between %s and %s.",
                    cloud.name(),
                    currentPeak,
                    currentPeriodStart.format(DATE_FMT),
                    currentPeriodEnd.format(DATE_FMT)
            );
            conflicts.add(new DetectedConflict(
                    ConflictTypeConstants.CLOUD_QUOTA,
                    ConflictSeverity.CRITICAL,
                    null,
                    null,
                    cloud.id(),
                    currentPeriodStart,
                    currentPeriodEnd,
                    BigDecimal.valueOf(currentPeak),
                    desc,
                    dedupKey
            ));
        }

        // Monthly cap check
        if (cloud.monthlyCap() != null && cloud.monthlyCap().compareTo(BigDecimal.ZERO) > 0) {
            YearMonth startYm = YearMonth.from(scanStart);
            YearMonth endYm = YearMonth.from(scanEnd);

            YearMonth ym = startYm;
            while (!ym.isAfter(endYm)) {
                BigDecimal monthPlannedCost = BigDecimal.ZERO;
                LocalDate monthStart = ym.atDay(1);
                LocalDate monthEnd = ym.atEndOfMonth();

                LocalDate d = monthStart;
                while (!d.isAfter(monthEnd)) {
                    int load = CapacityCalculator.calculateLoad(cloud.id(), d, allocations);
                    if (load > 0 && cloud.dailyQuotaUnits() != null && cloud.costRate() != null) {
                        BigDecimal units = cloud.dailyQuotaUnits()
                                .multiply(BigDecimal.valueOf(load))
                                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                        BigDecimal dayCost = units.multiply(cloud.costRate());
                        monthPlannedCost = monthPlannedCost.add(dayCost);
                    }
                    d = d.plusDays(1);
                }

                if (monthPlannedCost.compareTo(cloud.monthlyCap()) > 0) {
                    String dedupKey = String.format("CLOUDCAP:%d:%s", cloud.id(), ym);
                    String desc = String.format(
                            "Cloud resource %s has planned spend of ₹%.2f in %s, exceeding monthly cap of ₹%.2f.",
                            cloud.name(),
                            monthPlannedCost,
                            ym,
                            cloud.monthlyCap()
                    );
                    conflicts.add(new DetectedConflict(
                            ConflictTypeConstants.CLOUD_QUOTA,
                            ConflictSeverity.CRITICAL,
                            null,
                            null,
                            cloud.id(),
                            monthStart,
                            monthEnd,
                            monthPlannedCost.setScale(2, RoundingMode.HALF_UP),
                            desc,
                            dedupKey
                    ));
                }

                ym = ym.plusMonths(1);
            }
        }

        return conflicts;
    }
}
