package com.sist.rms.service;

import com.sist.rms.dto.MilestoneResponse;
import com.sist.rms.dto.TimelineResponse;
import com.sist.rms.entity.Milestone;
import com.sist.rms.entity.Project;
import com.sist.rms.repository.MilestoneRepository;
import com.sist.rms.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the data the timeline page draws:
 *
 *   Project
 *   |-- Milestone 1
 *   |-- Milestone 2
 *   +-- Milestone 3
 *
 * Each milestone already carries its dates, progress, status and the
 * developers assigned to it, so the JavaScript only has to size the bars.
 */
@Service
public class TimelineService {

    private final ProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final MilestoneService milestoneService;

    public TimelineService(ProjectRepository projectRepository,
                           MilestoneRepository milestoneRepository,
                           MilestoneService milestoneService) {
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.milestoneService = milestoneService;
    }

    public List<TimelineResponse> getFullTimeline() {
        List<TimelineResponse> timeline = new ArrayList<>();
        for (Project project : projectRepository.findAllByOrderByStartDateAsc()) {
            timeline.add(buildFor(project));
        }
        return timeline;
    }

    public TimelineResponse getTimelineForProject(Long projectId) {
        return buildFor(projectRepository.findById(projectId)
                .orElseThrow(() -> new com.sist.rms.exception.ResourceNotFoundException("Project", projectId)));
    }

    private TimelineResponse buildFor(Project project) {
        TimelineResponse response = new TimelineResponse();
        response.setProjectId(project.getId());
        response.setProjectName(project.getName());
        response.setProjectStartDate(project.getStartDate());
        response.setProjectEndDate(project.getEndDate());
        response.setProjectStatus(project.getStatus().name());

        List<MilestoneResponse> milestones = new ArrayList<>();
        for (Milestone milestone : milestoneRepository.findByProjectIdOrderByStartDateAsc(project.getId())) {
            milestones.add(milestoneService.toResponse(milestone));
        }
        response.setMilestones(milestones);
        return response;
    }
}
