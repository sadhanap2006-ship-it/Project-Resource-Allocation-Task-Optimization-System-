package edu.sist.prs.controller;

import edu.sist.prs.dto.response.ApiResponse;
import edu.sist.prs.dto.response.HeatmapResponse;
import edu.sist.prs.entity.ResourceType;
import edu.sist.prs.service.HeatmapService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/heatmap")
@PreAuthorize("hasAnyRole('PM', 'ADMIN')")
public class HeatmapController {

    private final HeatmapService heatmapService;

    public HeatmapController(HeatmapService heatmapService) {
        this.heatmapService = heatmapService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<HeatmapResponse>> getHeatmap(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "type", required = false) ResourceType type,
            @RequestParam(name = "granularity", required = false) String granularity
    ) {
        HeatmapResponse response = heatmapService.getHeatmap(from, to, type, granularity);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
