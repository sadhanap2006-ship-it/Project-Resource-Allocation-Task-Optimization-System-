package edu.sist.prs.service;

import edu.sist.prs.dto.request.MilestoneRequest;
import edu.sist.prs.dto.request.MilestoneSkillRequirementInput;
import edu.sist.prs.dto.response.MilestoneResponse;
import edu.sist.prs.dto.response.MilestoneSkillRequirementDto;
import edu.sist.prs.entity.*;
import edu.sist.prs.exception.BadRequestException;
import edu.sist.prs.exception.ConflictException;
import edu.sist.prs.exception.ResourceNotFoundException;
import edu.sist.prs.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final ProjectRepository projectRepository;
    private final MilestoneSkillRequirementRepository skillRequirementRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;
    private final AllocationRepository allocationRepository;

    private final ConflictDetectionService conflictDetectionService;
    private final SettingsService settingsService;

    public MilestoneService(
            MilestoneRepository milestoneRepository,
            ProjectRepository projectRepository,
            MilestoneSkillRequirementRepository skillRequirementRepository,
            SkillRepository skillRepository,
            UserRepository userRepository,
            AllocationRepository allocationRepository,
            ConflictDetectionService conflictDetectionService,
            SettingsService settingsService) {
        this.milestoneRepository = milestoneRepository;
        this.projectRepository = projectRepository;
        this.skillRequirementRepository = skillRequirementRepository;
        this.skillRepository = skillRepository;
        this.userRepository = userRepository;
        this.allocationRepository = allocationRepository;
        this.conflictDetectionService = conflictDetectionService;
        this.settingsService = settingsService;
    }

    @Transactional(readOnly = true)
    public List<MilestoneResponse> getAllMilestones() {
        return milestoneRepository.findAll().stream()
            .map(this::toMilestoneResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MilestoneResponse> getMilestonesByProjectId(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project", projectId);
        }
        return milestoneRepository.findByProjectId(projectId).stream()
            .map(this::toMilestoneResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MilestoneResponse getMilestoneById(Long id) {
        MilestoneEntity milestone = milestoneRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Milestone", id));
        return toMilestoneResponse(milestone);
    }

    @Transactional
    public MilestoneResponse createMilestone(Long projectId, MilestoneRequest request) {
        ProjectEntity project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        validateDates(project, request.startDate(), request.endDate());

        MilestoneEntity milestone = new MilestoneEntity(
            projectId,
            request.name(),
            request.description(),
            request.startDate(),
            request.endDate(),
            request.status() != null ? request.status() : MilestoneStatus.PLANNED,
            request.progressPct() != null ? request.progressPct() : 0,
            request.estimatedEffortHours()
        );

        MilestoneEntity saved = milestoneRepository.save(milestone);
        return toMilestoneResponse(saved);
    }

    @Transactional
    public MilestoneResponse updateMilestone(Long id, MilestoneRequest request) {
        MilestoneEntity milestone = milestoneRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Milestone", id));
        ProjectEntity project = projectRepository.findById(milestone.getProjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Project", milestone.getProjectId()));

        validateDates(project, request.startDate(), request.endDate());

        milestone.setName(request.name());
        milestone.setDescription(request.description());
        milestone.setStartDate(request.startDate());
        milestone.setEndDate(request.endDate());
        if (request.status() != null) {
            milestone.setStatus(request.status());
        }
        if (request.progressPct() != null) {
            milestone.setProgressPct(request.progressPct());
        }
        milestone.setEstimatedEffortHours(request.estimatedEffortHours());

        return toMilestoneResponse(milestone);
    }

    @Transactional
    public void deleteMilestone(Long id) {
        MilestoneEntity milestone = milestoneRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Milestone", id));
        milestoneRepository.delete(milestone);
    }

    @Transactional
    public MilestoneResponse updateProgress(Long id, Integer progressPct, Authentication authentication) {
        MilestoneEntity milestone = milestoneRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Milestone", id));

        UserEntity user = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == UserRole.DEVELOPER) {
            if (user.getResourceId() == null) {
                throw new AccessDeniedException("Developer user has no associated resource");
            }
            // Verify developer is allocated to this milestone
            boolean isAllocated = allocationRepository.findByMilestoneId(id).stream()
                .anyMatch(a -> a.getResourceId().equals(user.getResourceId()));
            if (!isAllocated) {
                throw new AccessDeniedException("Developers may only update progress on milestones they are allocated to");
            }
        }

        milestone.setProgressPct(progressPct);
        if (progressPct >= 100) {
            milestone.setStatus(MilestoneStatus.COMPLETED);
        } else if (progressPct > 0 && milestone.getStatus() == MilestoneStatus.PLANNED) {
            milestone.setStatus(MilestoneStatus.IN_PROGRESS);
        }

        return toMilestoneResponse(milestone);
    }

    @Transactional
    public List<MilestoneSkillRequirementDto> updateSkills(Long id, List<MilestoneSkillRequirementInput> requirements) {
        MilestoneEntity milestone = milestoneRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Milestone", id));

        skillRequirementRepository.deleteByIdMilestoneId(id);

        List<MilestoneSkillRequirementEntity> entities = new ArrayList<>();
        for (MilestoneSkillRequirementInput req : requirements) {
            SkillEntity skill = skillRepository.findById(req.skillId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill", req.skillId()));
            MilestoneSkillRequirementEntity msre = new MilestoneSkillRequirementEntity(
                milestone,
                skill,
                req.minProficiency(),
                req.mandatory() != null ? req.mandatory() : true
            );
            entities.add(msre);
        }

        skillRequirementRepository.saveAll(entities);

        return entities.stream()
            .map(e -> new MilestoneSkillRequirementDto(
                e.getSkill().getId(),
                e.getSkill().getName(),
                e.getSkill().getCategory(),
                e.getMinProficiency(),
                e.getMandatory()))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MilestoneSkillRequirementDto> getMilestoneSkills(Long id) {
        if (!milestoneRepository.existsById(id)) {
            throw new ResourceNotFoundException("Milestone", id);
        }
        return skillRequirementRepository.findByIdMilestoneId(id).stream()
            .map(e -> new MilestoneSkillRequirementDto(
                e.getSkill().getId(),
                e.getSkill().getName(),
                e.getSkill().getCategory(),
                e.getMinProficiency(),
                e.getMandatory()))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public edu.sist.prs.engine.model.MilestoneDeliveryForecast getMilestoneForecast(Long id) {
        MilestoneEntity m = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", id));

        List<edu.sist.prs.engine.model.MilestoneSnapshot> milestones = conflictDetectionService.loadMilestoneSnapshots();
        edu.sist.prs.engine.model.MilestoneSnapshot ms = milestones.stream()
                .filter(snap -> snap.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", id));

        List<edu.sist.prs.engine.model.AllocationSnapshot> allocations = conflictDetectionService.loadAllocationSnapshots();
        List<edu.sist.prs.engine.model.ResourceSnapshot> resources = conflictDetectionService.loadResourceSnapshots();
        List<edu.sist.prs.engine.model.UnavailabilitySnapshot> unavailabilities = conflictDetectionService.loadUnavailabilitySnapshots();

        int hoursPerDay = settingsService.getInt("hours.per.day", 8);
        int maxDays = settingsService.getInt("forecast.max.days", 1825);

        return edu.sist.prs.engine.DeliveryForecaster.forecastMilestone(
                ms, allocations, resources, unavailabilities, java.time.LocalDate.now(), hoursPerDay, maxDays
        );
    }

    @Transactional(readOnly = true)
    public List<edu.sist.prs.engine.model.CandidateRank> getCandidates(Long id, int targetPct) {
        MilestoneEntity m = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", id));

        List<edu.sist.prs.engine.model.MilestoneSnapshot> milestones = conflictDetectionService.loadMilestoneSnapshots();
        edu.sist.prs.engine.model.MilestoneSnapshot ms = milestones.stream()
                .filter(snap -> snap.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", id));

        List<edu.sist.prs.engine.model.AllocationSnapshot> allocations = conflictDetectionService.loadAllocationSnapshots();
        List<edu.sist.prs.engine.model.ResourceSnapshot> resources = conflictDetectionService.loadResourceSnapshots();
        List<edu.sist.prs.engine.model.UnavailabilitySnapshot> unavailabilities = conflictDetectionService.loadUnavailabilitySnapshots();

        double wSkill = settingsService.getDouble("ranking.weight.skill", 0.5);
        double wFit = settingsService.getDouble("ranking.weight.fit", 0.3);
        double wCost = settingsService.getDouble("ranking.weight.cost", 0.2);

        return edu.sist.prs.engine.SkillMatcher.rankCandidates(
                ms,
                Math.max(1, Math.min(100, targetPct)),
                resources,
                allocations,
                unavailabilities,
                wSkill,
                wFit,
                wCost,
                java.time.LocalDate.now()
        );
    }

    private void validateDates(ProjectEntity project, java.time.LocalDate start, java.time.LocalDate end) {
        if (end.isBefore(start)) {
            throw new BadRequestException("INVALID_DATE_RANGE", "Milestone end date must be on or after start date");
        }
        // Section 7: milestone dates inside project dates
        if (start.isBefore(project.getStartDate()) || end.isAfter(project.getEndDate())) {
            throw new BadRequestException("INVALID_DATE_RANGE", "Milestone dates [" + start + " to " + end + "] must be within project dates [" + project.getStartDate() + " to " + project.getEndDate() + "]");
        }
    }

    public MilestoneResponse toMilestoneResponse(MilestoneEntity m) {
        List<MilestoneSkillRequirementDto> skills = skillRequirementRepository.findByIdMilestoneId(m.getId()).stream()
            .map(e -> new MilestoneSkillRequirementDto(
                e.getSkill().getId(),
                e.getSkill().getName(),
                e.getSkill().getCategory(),
                e.getMinProficiency(),
                e.getMandatory()))
            .collect(Collectors.toList());

        return new MilestoneResponse(
            m.getId(),
            m.getProjectId(),
            m.getName(),
            m.getDescription(),
            m.getStartDate(),
            m.getEndDate(),
            m.getStatus(),
            m.getProgressPct(),
            m.getEstimatedEffortHours(),
            skills,
            null, // Forecast finish date, calculated in Phase 6
            0,
            m.getCreatedAt(),
            m.getUpdatedAt()
        );
    }
}
