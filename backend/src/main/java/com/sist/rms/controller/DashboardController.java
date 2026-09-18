package com.sist.rms.controller;

import com.sist.rms.dto.DashboardSummaryResponse;
import com.sist.rms.dto.DeveloperDashboardResponse;
import com.sist.rms.dto.TimelineResponse;
import com.sist.rms.service.DashboardService;
import com.sist.rms.service.TimelineService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** REST API for the two dashboards and the timeline. */
@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService dashboardService;
    private final TimelineService timelineService;

    public DashboardController(DashboardService dashboardService, TimelineService timelineService) {
        this.dashboardService = dashboardService;
        this.timelineService = timelineService;
    }

    /** Everything the Project Manager dashboard needs, in one call. */
    @GetMapping("/dashboard/summary")
    public DashboardSummaryResponse getManagerDashboard() {
        return dashboardService.getManagerDashboard();
    }

    @GetMapping("/dashboard/developer/{developerId}")
    public DeveloperDashboardResponse getDeveloperDashboard(@PathVariable Long developerId) {
        return dashboardService.getDeveloperDashboard(developerId);
    }

    @GetMapping("/timeline")
    public List<TimelineResponse> getTimeline() {
        return timelineService.getFullTimeline();
    }
}
