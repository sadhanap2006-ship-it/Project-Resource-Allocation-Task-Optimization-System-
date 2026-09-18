package com.sist.rms.controller;

import com.sist.rms.dto.SimulationRequest;
import com.sist.rms.dto.SimulationResponse;
import com.sist.rms.service.ReportService;
import com.sist.rms.service.SimulationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for the what-if simulator.
 *
 *   POST /api/simulations/run  -> calculates and returns the result.
 *                                 NOTHING is written to the database.
 *   POST /api/simulations      -> saves the scenario (needs scenarioName).
 */
@RestController
@RequestMapping("/api/simulations")
public class SimulationController {

    private final SimulationService simulationService;
    private final ReportService reportService;

    public SimulationController(SimulationService simulationService, ReportService reportService) {
        this.simulationService = simulationService;
        this.reportService = reportService;
    }

    /** Run only - the database is not touched. */
    @PostMapping("/run")
    public SimulationResponse run(@Valid @RequestBody SimulationRequest request) {
        return simulationService.run(request);
    }

    /** Save the scenario, because the user explicitly asked for it. */
    @PostMapping
    public ResponseEntity<SimulationResponse> save(@Valid @RequestBody SimulationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(simulationService.save(request));
    }

    @GetMapping
    public List<SimulationResponse> getAll() {
        return simulationService.findAllSaved();
    }

    @GetMapping("/{id}")
    public SimulationResponse getOne(@PathVariable Long id) {
        return simulationService.findSavedById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        simulationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Downloads the saved scenario as a PDF. */
    @GetMapping("/{id}/report")
    public ResponseEntity<byte[]> downloadReport(@PathVariable Long id) {
        SimulationResponse simulation = simulationService.findSavedById(id);
        byte[] pdf = reportService.buildSimulationReport(simulation);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"simulation-report-" + id + ".pdf\"")
                .body(pdf);
    }

    /**
     * Builds a PDF for a scenario that has NOT been saved, so the manager can
     * take the report away without storing anything.
     */
    @PostMapping("/report")
    public ResponseEntity<byte[]> downloadUnsavedReport(@Valid @RequestBody SimulationRequest request) {
        SimulationResponse simulation = simulationService.run(request);
        byte[] pdf = reportService.buildSimulationReport(simulation);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"simulation-report.pdf\"")
                .body(pdf);
    }
}
