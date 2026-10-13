package edu.sist.prs.controller;

import edu.sist.prs.dto.response.ApiResponse;
import edu.sist.prs.dto.request.SimulationRunRequest;
import edu.sist.prs.dto.request.SimulationSaveRequest;
import edu.sist.prs.engine.model.SimulationResult;
import edu.sist.prs.entity.SimulationScenarioEntity;
import edu.sist.prs.service.ReportService;
import edu.sist.prs.service.SimulationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/simulations")
@PreAuthorize("hasAnyRole('PM', 'ADMIN')")
public class SimulationController {

    private final SimulationService simulationService;
    private final ReportService reportService;

    public SimulationController(SimulationService simulationService, ReportService reportService) {
        this.simulationService = simulationService;
        this.reportService = reportService;
    }

    @PostMapping("/run")
    public ResponseEntity<ApiResponse<SimulationResult>> runSimulation(@Valid @RequestBody SimulationRunRequest request) {
        SimulationResult result = simulationService.runSimulation(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SimulationScenarioEntity>> saveSimulation(
            @Valid @RequestBody SimulationSaveRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        SimulationScenarioEntity saved = simulationService.saveSimulation(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(saved));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SimulationScenarioEntity>>> getScenarios(
            @RequestParam(name = "projectId", required = false) Long projectId
    ) {
        List<SimulationScenarioEntity> list = projectId != null
                ? simulationService.getScenariosByProject(projectId)
                : List.of();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SimulationScenarioEntity>> getScenario(@PathVariable Long id) {
        SimulationScenarioEntity scenario = simulationService.getScenarioById(id);
        return ResponseEntity.ok(ApiResponse.ok(scenario));
    }

    @GetMapping("/{id}/report.pdf")
    public ResponseEntity<byte[]> getScenarioPdf(@PathVariable Long id) {
        SimulationScenarioEntity scenario = simulationService.getScenarioById(id);
        byte[] pdfBytes = reportService.generateScenarioPdf(scenario);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"scenario-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
