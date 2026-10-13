package edu.sist.prs.dto.response;

import java.util.List;

public record HeatmapResponse(
        String granularity,
        List<String> dateHeaders,
        List<HeatmapRow> rows
) {
    public record HeatmapRow(
            Long resourceId,
            String resourceName,
            List<HeatmapCell> cells
    ) {}

    public record HeatmapCell(
            String date,
            double loadPct,
            double capacityPct,
            String band // LOW, HEALTHY, HIGH, OVER
    ) {}
}
