package edu.sist.prs.service;

import edu.sist.prs.dto.request.AllocationRequest;
import edu.sist.prs.dto.response.AllocationResponse;
import edu.sist.prs.engine.CapacityCalculator;
import edu.sist.prs.engine.SkillMatcher;
import edu.sist.prs.engine.model.AllocationSnapshot;
import edu.sist.prs.engine.model.MilestoneSnapshot;
import edu.sist.prs.engine.model.OverAllocationPeriod;
import edu.sist.prs.engine.model.ResourceSkillSnapshot;
import edu.sist.prs.engine.model.ResourceSnapshot;
import edu.sist.prs.engine.model.SkillRequirementSnapshot;
import edu.sist.prs.engine.model.UnavailabilitySnapshot;
import edu.sist.prs.entity.*;
import edu.sist.prs.exception.BadRequestException;
import edu.sist.prs.exception.ConflictException;
import edu.sist.prs.exception.ResourceNotFoundException;
import edu.sist.prs.repository.AllocationRepository;
import edu.sist.prs.repository.MilestoneRepository;
import edu.sist.prs.repository.MilestoneSkillRequirementRepository;
import edu.sist.prs.repository.ProjectRepository;
import edu.sist.prs.repository.ResourceRepository;
import edu.sist.prs.repository.ResourceSkillRepository;
import edu.sist.prs.repository.UnavailabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AllocationService {

    private final AllocationRepository allocationRepository;
    private final ResourceRepository resourceRepository;
    private final MilestoneRepository milestoneRepository;
    private final MilestoneSkillRequirementRepository milestoneSkillRequirementRepository;
    private final ResourceSkillRepository resourceSkillRepository;
    private final UnavailabilityRepository unavailabilityRepository;
    private final ProjectRepository projectRepository;
    private final ConflictDetectionService conflictDetectionService;
    private final SettingsService settingsService;

    public AllocationService(
            AllocationRepository allocationRepository,
            ResourceRepository resourceRepository,
            MilestoneRepository milestoneRepository,
            MilestoneSkillRequirementRepository milestoneSkillRequirementRepository,
            ResourceSkillRepository resourceSkillRepository,
            UnavailabilityRepository unavailabilityRepository,
            ProjectRepository projectRepository,
            ConflictDetectionService conflictDetectionService,
            SettingsService settingsService
    ) {
        this.allocationRepository = allocationRepository;
        this.resourceRepository = resourceRepository;
        this.milestoneRepository = milestoneRepository;
        this.milestoneSkillRequirementRepository = milestoneSkillRequirementRepository;
        this.resourceSkillRepository = resourceSkillRepository;
        this.unavailabilityRepository = unavailabilityRepository;
        this.projectRepository = projectRepository;
        this.conflictDetectionService = conflictDetectionService;
        this.settingsService = settingsService;
    }

    @Transactional(readOnly = true)
    public List<AllocationResponse> getAllocations(Long projectId, Long resourceId, Long milestoneId) {
        List<AllocationEntity> list;
        if (projectId != null) {
            list = allocationRepository.findByProjectId(projectId);
        } else if (resourceId != null) {
            list = allocationRepository.findByResourceId(resourceId);
        } else if (milestoneId != null) {
            list = allocationRepository.findByMilestoneId(milestoneId);
        } else {
            list = allocationRepository.findAll();
        }

        return list.stream().map(this::toAllocationResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AllocationResponse getAllocationById(Long id) {
        AllocationEntity allocation = allocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation", id));
        return toAllocationResponse(allocation);
    }

    @Transactional
    public AllocationResponse createAllocation(AllocationRequest request) {
        ResourceEntity resource = resourceRepository.findById(request.resourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource", request.resourceId()));

        if (resource.getStatus() != ResourceStatus.ACTIVE) {
            throw new BadRequestException("RESOURCE_INACTIVE", "Cannot allocate to an INACTIVE resource: " + resource.getName());
        }

        MilestoneEntity milestone = milestoneRepository.findById(request.milestoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", request.milestoneId()));

        validateAllocationDates(milestone, request.startDate(), request.endDate());

        Optional<AllocationEntity> existing = allocationRepository.findByResourceIdAndMilestoneIdAndStartDate(
                request.resourceId(), request.milestoneId(), request.startDate()
        );
        if (existing.isPresent()) {
            throw new ConflictException("DUPLICATE_ALLOCATION", "An allocation already exists for this resource, milestone, and start date");
        }

        // Rule R6: Skill mismatch verification
        validateSkillMatch(resource, milestone, request.confirmOverride());

        // Rule R2: Over-allocation verification
        validateOverAllocation(null, resource, request.startDate(), request.endDate(), request.allocationPct(), request.confirmOverride());

        AllocationEntity allocation = new AllocationEntity(
                request.resourceId(),
                request.milestoneId(),
                request.startDate(),
                request.endDate(),
                request.allocationPct(),
                Boolean.TRUE.equals(request.confirmOverride())
        );

        AllocationEntity saved = allocationRepository.save(allocation);

        // Re-evaluate conflicts for affected resource and project
        conflictDetectionService.reevaluateScope(Set.of(resource.getId()), Set.of(milestone.getProjectId()));

        return toAllocationResponse(saved);
    }

    @Transactional
    public AllocationResponse updateAllocation(Long id, AllocationRequest request) {
        AllocationEntity allocation = allocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation", id));

        ResourceEntity resource = resourceRepository.findById(request.resourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource", request.resourceId()));

        if (resource.getStatus() != ResourceStatus.ACTIVE) {
            throw new BadRequestException("RESOURCE_INACTIVE", "Cannot allocate to an INACTIVE resource: " + resource.getName());
        }

        MilestoneEntity milestone = milestoneRepository.findById(request.milestoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", request.milestoneId()));

        validateAllocationDates(milestone, request.startDate(), request.endDate());

        // Rule R6: Skill mismatch verification
        validateSkillMatch(resource, milestone, request.confirmOverride());

        // Rule R2: Over-allocation verification
        validateOverAllocation(id, resource, request.startDate(), request.endDate(), request.allocationPct(), request.confirmOverride());

        Long oldResourceId = allocation.getResourceId();
        Long oldProjectId = allocation.getMilestone() != null ? allocation.getMilestone().getProjectId() : null;

        allocation.setResourceId(request.resourceId());
        allocation.setMilestoneId(request.milestoneId());
        allocation.setStartDate(request.startDate());
        allocation.setEndDate(request.endDate());
        allocation.setAllocationPct(request.allocationPct());
        if (request.confirmOverride() != null) {
            allocation.setOverrideConfirmed(request.confirmOverride());
        }

        AllocationEntity updated = allocationRepository.save(allocation);

        // Re-evaluate conflicts for affected resources and projects
        Set<Long> resIds = oldResourceId.equals(resource.getId()) ? Set.of(resource.getId()) : Set.of(oldResourceId, resource.getId());
        Set<Long> projIds = oldProjectId != null ? Set.of(oldProjectId, milestone.getProjectId()) : Set.of(milestone.getProjectId());
        conflictDetectionService.reevaluateScope(resIds, projIds);

        return toAllocationResponse(updated);
    }

    @Transactional
    public void deleteAllocation(Long id) {
        AllocationEntity allocation = allocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation", id));

        Long resourceId = allocation.getResourceId();
        Long projectId = allocation.getMilestone() != null ? allocation.getMilestone().getProjectId() : null;

        allocationRepository.delete(allocation);

        // Rule R10: Deleting an allocation re-runs conflict detection; conflicts no longer found are set RESOLVED
        Set<Long> projIds = projectId != null ? Set.of(projectId) : Set.of();
        conflictDetectionService.reevaluateScope(Set.of(resourceId), projIds);
    }

    private void validateSkillMatch(ResourceEntity resource, MilestoneEntity milestone, Boolean confirmOverride) {
        if (resource.getType() != ResourceType.DEVELOPER) {
            return;
        }

        List<ResourceSkillEntity> skills = resourceSkillRepository.findByIdResourceId(resource.getId());
        List<ResourceSkillSnapshot> skillSnapshots = skills.stream()
                .map(s -> new ResourceSkillSnapshot(s.getId().getSkillId(), s.getSkill() != null ? s.getSkill().getName() : "", s.getProficiency()))
                .toList();

        ResourceSnapshot resSnap = new ResourceSnapshot(
                resource.getId(), resource.getType(), resource.getName(), resource.getStatus(),
                resource.getCapacityPct(), resource.getCostRate(), resource.getCostUnit(),
                null, null, skillSnapshots
        );

        List<MilestoneSkillRequirementEntity> reqs = milestoneSkillRequirementRepository.findByIdMilestoneId(milestone.getId());
        List<SkillRequirementSnapshot> reqSnapshots = reqs.stream()
                .map(r -> new SkillRequirementSnapshot(r.getId().getSkillId(), r.getSkill() != null ? r.getSkill().getName() : "", r.getMinProficiency(), r.getMandatory()))
                .toList();

        MilestoneSnapshot msSnap = new MilestoneSnapshot(
                milestone.getId(), milestone.getProjectId(), milestone.getName(),
                milestone.getStartDate(), milestone.getEndDate(), milestone.getStatus(),
                milestone.getProgressPct(), milestone.getEstimatedEffortHours(), reqSnapshots
        );

        double matchPct = SkillMatcher.calculateMatchPercentage(resSnap, msSnap);
        double mismatchThreshold = settingsService.getDouble("skill.mismatch.pct", 50.0);

        if (matchPct < mismatchThreshold && !Boolean.TRUE.equals(confirmOverride)) {
            throw new ConflictException(
                    "SKILL_MISMATCH",
                    String.format("%s has only %.1f%% skill match for milestone '%s' (threshold %.0f%%). Confirm override to proceed.",
                            resource.getName(), matchPct, milestone.getName(), mismatchThreshold)
            );
        }
    }

    private void validateOverAllocation(
            Long existingAllocationId,
            ResourceEntity resource,
            LocalDate startDate,
            LocalDate endDate,
            int allocationPct,
            Boolean confirmOverride
    ) {
        if (Boolean.TRUE.equals(confirmOverride)) {
            return;
        }

        List<AllocationEntity> all = allocationRepository.findByResourceId(resource.getId());
        List<AllocationSnapshot> snapshots = new ArrayList<>();

        for (AllocationEntity a : all) {
            if (existingAllocationId != null && a.getId().equals(existingAllocationId)) {
                continue;
            }
            snapshots.add(new AllocationSnapshot(
                    a.getId(), a.getResourceId(), a.getMilestoneId(),
                    a.getMilestone() != null ? a.getMilestone().getProjectId() : null,
                    a.getStartDate(), a.getEndDate(), a.getAllocationPct(), a.getOverrideConfirmed(),
                    a.getMilestone() != null ? a.getMilestone().getStatus() : MilestoneStatus.IN_PROGRESS,
                    a.getMilestone() != null && a.getMilestone().getProject() != null ? a.getMilestone().getProject().getStatus() : ProjectStatus.ACTIVE
            ));
        }

        // Add prospective allocation
        snapshots.add(new AllocationSnapshot(
                -1L, resource.getId(), -1L, -1L,
                startDate, endDate, allocationPct, false,
                MilestoneStatus.IN_PROGRESS, ProjectStatus.ACTIVE
        ));

        List<UnavailabilityEntity> unavails = unavailabilityRepository.findByResourceId(resource.getId());
        List<UnavailabilitySnapshot> unavailSnapshots = unavails.stream()
                .map(u -> new UnavailabilitySnapshot(u.getId(), u.getResourceId(), u.getStartDate(), u.getEndDate(), u.getType(), u.getStatus()))
                .toList();

        ResourceSnapshot resSnap = new ResourceSnapshot(
                resource.getId(), resource.getType(), resource.getName(), resource.getStatus(),
                resource.getCapacityPct(), resource.getCostRate(), resource.getCostUnit(),
                null, null, List.of()
        );

        List<OverAllocationPeriod> periods = CapacityCalculator.findOverAllocationPeriods(
                resSnap, startDate, endDate, snapshots, unavailSnapshots
        );

        if (!periods.isEmpty()) {
            OverAllocationPeriod p = periods.get(0);
            throw new ConflictException(
                    "RESOURCE_OVERALLOCATION",
                    String.format("%s would be at %d%% between %s and %s (capacity %d%%). Confirm override to proceed.",
                            resource.getName(), p.peakLoad(), p.startDate(), p.endDate(), resource.getCapacityPct())
            );
        }
    }

    private void validateAllocationDates(MilestoneEntity milestone, LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new BadRequestException("INVALID_DATE_RANGE", "Allocation end date must be on or after start date");
        }
        if (start.isBefore(milestone.getStartDate()) || end.isAfter(milestone.getEndDate())) {
            throw new BadRequestException(
                    "INVALID_DATE_RANGE",
                    "Allocation dates [" + start + " to " + end + "] must be within milestone dates [" + milestone.getStartDate() + " to " + milestone.getEndDate() + "]"
            );
        }
    }

    public AllocationResponse toAllocationResponse(AllocationEntity a) {
        ResourceEntity r = resourceRepository.findById(a.getResourceId()).orElse(null);
        MilestoneEntity m = milestoneRepository.findById(a.getMilestoneId()).orElse(null);
        ProjectEntity p = m != null ? projectRepository.findById(m.getProjectId()).orElse(null) : null;

        return new AllocationResponse(
                a.getId(),
                a.getResourceId(),
                r != null ? r.getName() : null,
                r != null ? r.getType() : null,
                a.getMilestoneId(),
                m != null ? m.getName() : null,
                p != null ? p.getId() : null,
                p != null ? p.getCode() : null,
                a.getStartDate(),
                a.getEndDate(),
                a.getAllocationPct(),
                a.getOverrideConfirmed(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
