package com.sist.rms.controller;

import com.sist.rms.dto.LeaveRequest;
import com.sist.rms.dto.LeaveResponse;
import com.sist.rms.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** REST API for developer leave / availability. */
@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @GetMapping
    public List<LeaveResponse> getAll(@RequestParam(required = false) Long developerId) {
        return developerId == null ? leaveService.findAll() : leaveService.findByDeveloper(developerId);
    }

    @PostMapping
    public ResponseEntity<LeaveResponse> create(@Valid @RequestBody LeaveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.create(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        leaveService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
