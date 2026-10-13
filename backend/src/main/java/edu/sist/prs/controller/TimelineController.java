package edu.sist.prs.controller;

import edu.sist.prs.dto.response.ApiResponse;
import edu.sist.prs.dto.response.ProjectTimelineResponse;
import edu.sist.prs.dto.response.ResourceTimelineResponse;
import edu.sist.prs.entity.ResourceType;
import edu.sist.prs.service.TimelineService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('PM', 'ADMIN')")
public class TimelineController {

    private final TimelineService timelineService;

    public TimelineController(TimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @GetMapping("/projects/{id}/timeline")
    public ResponseEntity<ApiResponse<ProjectTimelineResponse>> getProjectTimeline(@PathVariable Long id) {
        ProjectTimelineResponse response = timelineService.getProjectTimeline(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/timeline/resources")
    public ResponseEntity<ApiResponse<ResourceTimelineResponse>> getResourceTimeline(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "type", required = false) ResourceType type
    ) {
        ResourceTimelineResponse response = timelineService.getResourceTimeline(from, to, type);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
