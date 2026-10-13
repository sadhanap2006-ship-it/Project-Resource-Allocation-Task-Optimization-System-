package edu.sist.prs.service;

import edu.sist.prs.dto.response.HeatmapResponse;
import edu.sist.prs.engine.CapacityCalculator;
import edu.sist.prs.engine.model.AllocationSnapshot;
import edu.sist.prs.entity.ResourceEntity;
import edu.sist.prs.entity.ResourceStatus;
import edu.sist.prs.entity.ResourceType;
import edu.sist.prs.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class HeatmapService {

    private final ResourceRepository resourceRepository;
    private final ConflictDetectionService conflictDetectionService;

    public HeatmapService(ResourceRepository resourceRepository, ConflictDetectionService conflictDetectionService) {
        this.resourceRepository = resourceRepository;
        this.conflictDetectionService = conflictDetectionService;
    }

    @Transactional(readOnly = true)
    public HeatmapResponse getHeatmap(LocalDate from, LocalDate to, ResourceType type, String granularity) {
        String gran = (granularity != null && granularity.equalsIgnoreCase("WEEK")) ? "WEEK" : "DAY";
        LocalDate today = LocalDate.now();

        LocalDate startDate = from != null ? from : (gran.equals("WEEK") ? today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) : today);
        LocalDate endDate = to != null ? to : (gran.equals("WEEK") ? startDate.plusWeeks(8) : startDate.plusDays(30));

        List<ResourceEntity> resources = resourceRepository.findAll().stream()
                .filter(r -> r.getStatus() == ResourceStatus.ACTIVE)
                .filter(r -> type == null || r.getType() == type)
                .sorted(Comparator.comparing(ResourceEntity::getName))
                .toList();

        List<AllocationSnapshot> allocations = conflictDetectionService.loadAllocationSnapshots();

        List<String> headers = new ArrayList<>();
        List<LocalDate[]> periods = new ArrayList<>();

        if (gran.equals("WEEK")) {
            LocalDate curr = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            int weekIdx = 1;
            while (!curr.isAfter(endDate)) {
                LocalDate periodEnd = curr.plusDays(4); // Mon to Fri
                headers.add("W" + weekIdx + " (" + curr.getMonth().name().substring(0, 3) + " " + curr.getDayOfMonth() + ")");
                periods.add(new LocalDate[]{curr, periodEnd});
                curr = curr.plusWeeks(1);
                weekIdx++;
            }
        } else {
            LocalDate curr = startDate;
            while (!curr.isAfter(endDate)) {
                headers.add(curr.toString());
                periods.add(new LocalDate[]{curr, curr});
                curr = curr.plusDays(1);
            }
        }

        List<HeatmapResponse.HeatmapRow> rows = new ArrayList<>();

        for (ResourceEntity r : resources) {
            List<HeatmapResponse.HeatmapCell> cells = new ArrayList<>();

            for (int i = 0; i < periods.size(); i++) {
                LocalDate pStart = periods.get(i)[0];
                LocalDate pEnd = periods.get(i)[1];

                int load;
                if (pStart.equals(pEnd)) {
                    load = CapacityCalculator.calculateLoad(r.getId(), pStart, allocations);
                } else {
                    load = CapacityCalculator.calculatePeakLoad(r.getId(), pStart, pEnd, allocations);
                }

                double loadPct = load;
                double capacityPct = r.getCapacityPct();

                String band;
                if (loadPct > capacityPct || loadPct > 100.0) {
                    band = "OVER";
                } else if (loadPct > 85.0) {
                    band = "HIGH";
                } else if (loadPct >= 50.0) {
                    band = "HEALTHY";
                } else {
                    band = "LOW";
                }

                cells.add(new HeatmapResponse.HeatmapCell(
                        headers.get(i),
                        loadPct,
                        capacityPct,
                        band
                ));
            }

            rows.add(new HeatmapResponse.HeatmapRow(
                    r.getId(),
                    r.getName(),
                    cells
            ));
        }

        return new HeatmapResponse(gran, headers, rows);
    }
}
