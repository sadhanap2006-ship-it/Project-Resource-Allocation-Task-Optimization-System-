package edu.sist.prs.controller;

import edu.sist.prs.dto.response.ApiResponse;
import edu.sist.prs.dto.response.DeveloperDashboardResponse;
import edu.sist.prs.dto.response.PmDashboardResponse;
import edu.sist.prs.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/pm")
    @PreAuthorize("hasAnyRole('PM', 'ADMIN')")
    public ResponseEntity<ApiResponse<PmDashboardResponse>> getPmDashboard() {
        PmDashboardResponse response = dashboardService.getPmDashboard();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/developer/me")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'PM', 'ADMIN')")
    public ResponseEntity<ApiResponse<DeveloperDashboardResponse>> getDeveloperDashboard(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        DeveloperDashboardResponse response = dashboardService.getDeveloperDashboard(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
