package edu.sist.prs.service;

import edu.sist.prs.dto.response.ProjectTimelineResponse;
import edu.sist.prs.dto.response.ResourceTimelineResponse;
import edu.sist.prs.engine.DeliveryForecaster;
import edu.sist.prs.engine.model.AllocationSnapshot;
import edu.sist.prs.engine.model.MilestoneDeliveryForecast;
import edu.sist.prs.engine.model.MilestoneSnapshot;
import edu.sist.prs.engine.model.ResourceSnapshot;
import edu.sist.prs.engine.model.UnavailabilitySnapshot;
import edu.sist.prs.entity.*;
import edu.sist.prs.exception.ApiException;
import edu.sist.prs.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TimelineService {

    private final ProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final AllocationRepository allocationRepository;
    private final ResourceRepository resourceRepository;
    private final ConflictRepository conflictRepository;
    private final ConflictDetectionService conflictDetectionService;
    private final SettingsService settingsService;

    public TimelineService(
            ProjectRepository projectRepository,
            MilestoneRepository milestoneRepository,
            AllocationRepository allocationRepository,
            ResourceRepository resourceRepository,
            ConflictRepository conflictRepository,
            ConflictDetectionService conflictDetectionService,
            SettingsService settingsService
    ) {
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.allocationRepository = allocationRepository;
        this.resourceRepository = resourceRepository;
        this.conflictRepository = conflictRepository;
        this.conflictDetectionService = conflictDetectionService;
        this.settingsService = settingsService;
    }

    @Transactional(readOnly = true)
    public ProjectTimelineResponse getProjectTimeline(Long projectId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Project not found: " + projectId));

        List<MilestoneEntity> milestoneEntities = milestoneRepository.findByProjectIdOrderByStartDateAsc(projectId);

        List<ResourceSnapshot> resources = conflictDetectionService.loadResourceSnapshots();
        List<MilestoneSnapshot> milestones = conflictDetectionService.loadMilestoneSnapshots();
        List<AllocationSnapshot> allocations = conflictDetectionService.loadAllocationSnapshots();
        List<UnavailabilitySnapshot> unavailabilities = conflictDetectionService.loadUnavailabilitySnapshots();

        Map<Long, MilestoneSnapshot> milestoneSnapshotMap = milestones.stream()
                .collect(Collectors.toMap(MilestoneSnapshot::id, m -> m));
        Map<Long, String> resourceNameMap = resources.stream()
                .collect(Collectors.toMap(ResourceSnapshot::id, ResourceSnapshot::name));

        LocalDate today = LocalDate.now();
        int hoursPerDay = settingsService.getInt("hours.per.day", 8);

        List<ProjectTimelineResponse.TimelineMilestoneItem> items = new ArrayList<>();

        for (MilestoneEntity me : milestoneEntities) {
            MilestoneSnapshot ms = milestoneSnapshotMap.get(me.getId());
            LocalDate forecastFinish = null;
            int delayDays = 0;

            if (ms != null && me.getStatus() != MilestoneStatus.COMPLETED) {
                MilestoneDeliveryForecast df = DeliveryForecaster.forecastMilestone(
                        ms, allocations, resources, unavailabilities, today, hoursPerDay, 365
                );
                forecastFinish = df.forecastFinishDate();
                delayDays = (int) df.delayWorkingDays();
            }

            // Get assigned resource names
            List<String> assignedResourceNames = allocations.stream()
                    .filter(a -> a.milestoneId().equals(me.getId()))
                    .map(a -> resourceNameMap.getOrDefault(a.resourceId(), "Unknown Resource"))
                    .distinct()
                    .toList();

            items.add(new ProjectTimelineResponse.TimelineMilestoneItem(
                    me.getId(),
                    me.getName(),
                    me.getStatus().name(),
                    me.getStartDate(),
                    me.getEndDate(),
                    me.getProgressPct().doubleValue(),
                    forecastFinish,
                    delayDays,
                    assignedResourceNames
            ));
        }

        return new ProjectTimelineResponse(
                project.getId(),
                project.getName(),
                items
        );
    }

    @Transactional(readOnly = true)
    public ResourceTimelineResponse getResourceTimeline(LocalDate from, LocalDate to, ResourceType type) {
        LocalDate startDate = from != null ? from : LocalDate.now().minusDays(14);
        LocalDate endDate = to != null ? to : LocalDate.now().plusDays(60);

        List<ResourceEntity> resources = resourceRepository.findAll().stream()
                .filter(r -> r.getStatus() == ResourceStatus.ACTIVE)
                .filter(r -> type == null || r.getType() == type)
                .sorted(Comparator.comparing(ResourceEntity::getName))
                .toList();

        List<AllocationEntity> allAllocations = allocationRepository.findAll();
        Map<Long, MilestoneEntity> milestoneMap = milestoneRepository.findAll().stream()
                .collect(Collectors.toMap(MilestoneEntity::getId, m -> m));
        Map<Long, ProjectEntity> projectMap = projectRepository.findAll().stream()
                .collect(Collectors.toMap(ProjectEntity::getId, p -> p));

        List<ConflictEntity> allConflicts = conflictRepository.findAll().stream()
                .filter(c -> c.getStatus() == ConflictStatus.OPEN || c.getStatus() == ConflictStatus.ACKNOWLEDGED)
                .toList();

        List<ResourceTimelineResponse.ResourceTimelineLane> lanes = new ArrayList<>();

        for (ResourceEntity r : resources) {
            // Find allocations intersecting [startDate, endDate]
            List<ResourceTimelineResponse.TimelineAllocationBar> bars = allAllocations.stream()
                    .filter(a -> a.getResourceId().equals(r.getId()))
                    .filter(a -> !a.getEndDate().isBefore(startDate) && !a.getStartDate().isAfter(endDate))
                    .map(a -> {
                        MilestoneEntity m = milestoneMap.get(a.getMilestoneId());
                        ProjectEntity p = m != null ? projectMap.get(m.getProjectId()) : null;
                        return new ResourceTimelineResponse.TimelineAllocationBar(
                                a.getId(),
                                p != null ? p.getId() : null,
                                p != null ? p.getName() : "Unknown",
                                m != null ? m.getId() : null,
                                m != null ? m.getName() : "Unknown",
                                a.getAllocationPct(),
                                a.getStartDate(),
                                a.getEndDate()
                        );
                    })
                    .sorted(Comparator.comparing(ResourceTimelineResponse.TimelineAllocationBar::startDate))
                    .toList();

            // Find conflicts intersecting [startDate, endDate] for this resource
            List<ResourceTimelineResponse.TimelineConflictBand> conflictBands = allConflicts.stream()
                    .filter(c -> r.getId().equals(c.getResourceId()))
                    .filter(c -> !c.getEndDate().isBefore(startDate) && !c.getStartDate().isAfter(endDate))
                    .map(c -> new ResourceTimelineResponse.TimelineConflictBand(
                            c.getId(),
                            c.getType(),
                            c.getSeverity().name(),
                            c.getDescription(),
                            c.getStartDate(),
                            c.getEndDate()
                    ))
                    .sorted(Comparator.comparing(ResourceTimelineResponse.TimelineConflictBand::startDate))
                    .toList();

            lanes.add(new ResourceTimelineResponse.ResourceTimelineLane(
                    r.getId(),
                    r.getName(),
                    bars,
                    conflictBands
            ));
        }

        return new ResourceTimelineResponse(lanes);
    }
}
