package edu.sist.prs.service;

import edu.sist.prs.dto.response.AllocationMatrixResponse;
import edu.sist.prs.engine.CapacityCalculator;
import edu.sist.prs.engine.model.AllocationSnapshot;
import edu.sist.prs.entity.*;
import edu.sist.prs.exception.ApiException;
import edu.sist.prs.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class MatrixService {

    private final ProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final AllocationRepository allocationRepository;
    private final ResourceRepository resourceRepository;
    private final ConflictDetectionService conflictDetectionService;

    public MatrixService(
            ProjectRepository projectRepository,
            MilestoneRepository milestoneRepository,
            AllocationRepository allocationRepository,
            ResourceRepository resourceRepository,
            ConflictDetectionService conflictDetectionService
    ) {
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.allocationRepository = allocationRepository;
        this.resourceRepository = resourceRepository;
        this.conflictDetectionService = conflictDetectionService;
    }

    @Transactional(readOnly = true)
    public AllocationMatrixResponse getProjectMatrix(Long projectId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Project not found: " + projectId));

        List<MilestoneEntity> milestones = milestoneRepository.findByProjectIdOrderByStartDateAsc(projectId);
        List<AllocationSnapshot> allAllocations = conflictDetectionService.loadAllocationSnapshots();

        List<AllocationMatrixResponse.MatrixMilestoneHeader> milestoneHeaders = milestones.stream()
                .map(m -> new AllocationMatrixResponse.MatrixMilestoneHeader(
                        m.getId(),
                        m.getName(),
                        m.getStatus().name(),
                        m.getStartDate(),
                        m.getEndDate()
                ))
                .toList();

        // Get all active resources
        List<ResourceEntity> resources = resourceRepository.findAll().stream()
                .filter(r -> r.getStatus() == ResourceStatus.ACTIVE)
                .sorted(Comparator.comparing(ResourceEntity::getType).thenComparing(ResourceEntity::getName))
                .toList();

        List<AllocationMatrixResponse.MatrixResourceRow> resourceRows = new ArrayList<>();

        for (ResourceEntity r : resources) {
            Map<Long, Integer> allocMap = new HashMap<>();
            int totalProjectAllocPct = 0;

            for (MilestoneEntity m : milestones) {
                // Find allocation for this resource in this milestone
                int sumPct = allAllocations.stream()
                        .filter(a -> a.resourceId().equals(r.getId()) && a.milestoneId().equals(m.getId()))
                        .mapToInt(AllocationSnapshot::allocationPct)
                        .sum();
                if (sumPct > 0) {
                    allocMap.put(m.getId(), sumPct);
                    totalProjectAllocPct += sumPct;
                }
            }

            // Calculate peak load across the project's start and end date
            int peakInt = CapacityCalculator.calculatePeakLoad(
                    r.getId(),
                    project.getStartDate(),
                    project.getEndDate(),
                    allAllocations
            );

            String band;
            if (peakInt > r.getCapacityPct() || peakInt > 100) {
                band = "OVER";
            } else if (peakInt > 85) {
                band = "HIGH";
            } else if (peakInt >= 50) {
                band = "HEALTHY";
            } else {
                band = "LOW";
            }

            resourceRows.add(new AllocationMatrixResponse.MatrixResourceRow(
                    r.getId(),
                    r.getName(),
                    r.getType().name(),
                    allocMap,
                    totalProjectAllocPct,
                    peakInt,
                    band
            ));
        }

        return new AllocationMatrixResponse(
                project.getId(),
                project.getName(),
                milestoneHeaders,
                resourceRows
        );
    }
}
