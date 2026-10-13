package edu.sist.prs.controller;

import edu.sist.prs.dto.response.ApiResponse;
import edu.sist.prs.entity.ConflictEntity;
import edu.sist.prs.entity.ConflictSeverity;
import edu.sist.prs.entity.ConflictStatus;
import edu.sist.prs.exception.ApiException;
import edu.sist.prs.repository.ConflictRepository;
import edu.sist.prs.service.ConflictDetectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conflicts")
public class ConflictController {

    private final ConflictRepository conflictRepository;
    private final ConflictDetectionService conflictDetectionService;

    public ConflictController(ConflictRepository conflictRepository, ConflictDetectionService conflictDetectionService) {
        this.conflictRepository = conflictRepository;
        this.conflictDetectionService = conflictDetectionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConflictEntity>>> getConflicts(
            @RequestParam(required = false) ConflictSeverity severity,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) ConflictStatus status,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long resourceId
    ) {
        List<ConflictEntity> all = conflictRepository.findAll();
        List<ConflictEntity> filtered = all.stream().filter(c -> {
            if (severity != null && c.getSeverity() != severity) return false;
            if (type != null && !type.equalsIgnoreCase(c.getType())) return false;
            if (status != null && c.getStatus() != status) return false;
            if (projectId != null && (c.getProjectId() == null || !c.getProjectId().equals(projectId))) return false;
            if (resourceId != null && (c.getResourceId() == null || !c.getResourceId().equals(resourceId))) return false;
            return true;
        }).toList();

        return ResponseEntity.ok(ApiResponse.success(filtered, "Conflicts retrieved"));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<ApiResponse<List<ConflictEntity>>> getProjectConflicts(@PathVariable Long projectId) {
        List<ConflictEntity> list = conflictRepository.findByProjectId(projectId);
        return ResponseEntity.ok(ApiResponse.success(list, "Project conflicts retrieved"));
    }

    @PutMapping("/{id}/acknowledge")
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<ApiResponse<ConflictEntity>> acknowledgeConflict(@PathVariable Long id) {
        ConflictEntity conflict = conflictRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Conflict not found"));

        if (conflict.getStatus() == ConflictStatus.OPEN) {
            conflict.setStatus(ConflictStatus.ACKNOWLEDGED);
            conflict = conflictRepository.save(conflict);
        }
        return ResponseEntity.ok(ApiResponse.success(conflict, "Conflict acknowledged"));
    }

    @PostMapping("/scan")
    @PreAuthorize("hasAnyRole('PM', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ConflictEntity>>> triggerScan() {
        List<ConflictEntity> results = conflictDetectionService.runFullScan();
        return ResponseEntity.ok(ApiResponse.success(results, "Conflict scan completed"));
    }
}
