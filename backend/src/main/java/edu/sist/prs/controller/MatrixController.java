package edu.sist.prs.controller;

import edu.sist.prs.dto.response.ApiResponse;
import edu.sist.prs.dto.response.AllocationMatrixResponse;
import edu.sist.prs.service.MatrixService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
@PreAuthorize("hasAnyRole('PM', 'ADMIN')")
public class MatrixController {

    private final MatrixService matrixService;

    public MatrixController(MatrixService matrixService) {
        this.matrixService = matrixService;
    }

    @GetMapping("/{id}/matrix")
    public ResponseEntity<ApiResponse<AllocationMatrixResponse>> getProjectMatrix(@PathVariable Long id) {
        AllocationMatrixResponse response = matrixService.getProjectMatrix(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
