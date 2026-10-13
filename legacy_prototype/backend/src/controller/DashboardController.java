package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.AllocationDAO;
import dao.DeveloperDAO;
import dao.MilestoneDAO;
import dto.*;
import model.Allocation;
import model.Developer;
import model.Milestone;
import service.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DashboardController extends BaseController implements HttpHandler {

    private final ProjectService projectService = new ProjectService();
    private final DeveloperDAO developerDAO = new DeveloperDAO();
    private final MilestoneDAO milestoneDAO = new MilestoneDAO();
    private final AllocationDAO allocationDAO = new AllocationDAO();
    private final AvailabilityService availabilityService = new AvailabilityService();
    private final NotificationService notificationService = new NotificationService();
    private final ConflictDetectionService conflictService = new ConflictDetectionService();
    private final SkillGapService skillGapService = new SkillGapService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleCorsOptions(exchange)) return;

        String path = exchange.getRequestURI().getPath();

        try {
            if (path.endsWith("/conflicts")) {
                // GET /api/conflicts
                List<ConflictResponseDTO> conflicts = conflictService.detectAllConflictDTOs();
                sendSuccess(exchange, 200, "System conflicts evaluated", conflicts);
                return;
            }

            if (path.endsWith("/skill-gaps")) {
                // GET /api/skill-gaps
                List<SkillGapResponseDTO> gaps = skillGapService.detectAllSkillGapDTOs();
                sendSuccess(exchange, 200, "Skill gaps evaluated", gaps);
                return;
            }

            if (path.contains("/dashboard/manager")) {
                // GET /api/dashboard/manager
                List<ProjectResponseDTO> projects = projectService.getAllProjects();
                List<Developer> developers = developerDAO.findAll();
                List<ConflictResponseDTO> conflicts = conflictService.detectAllConflictDTOs();
                List<SkillGapResponseDTO> skillGaps = skillGapService.detectAllSkillGapDTOs();

                double totalBudget = 0;
                double totalSpend = 0;
                int activeProjects = 0;
                int atRiskProjects = 0;

                for (ProjectResponseDTO p : projects) {
                    totalBudget += p.getTotalBudget();
                    totalSpend += p.getCurrentSpend();
                    if ("ACTIVE".equalsIgnoreCase(p.getStatus()) || "IN_PROGRESS".equalsIgnoreCase(p.getStatus())) {
                        activeProjects++;
                    }
                    if ("AT_RISK".equalsIgnoreCase(p.getStatus())) {
                        atRiskProjects++;
                    }
                }

                int totalDevLoad = 0;
                for (Developer d : developers) {
                    List<Allocation> allocs = allocationDAO.findByDeveloperId(d.getId());
                    int devLoad = 0;
                    for (Allocation a : allocs) devLoad += a.getAllocationPercentage();
                    totalDevLoad += devLoad;
                }
                int avgUtil = developers.isEmpty() ? 0 : (totalDevLoad / developers.size());

                int criticalConflicts = 0;
                int warningConflicts = 0;
                for (ConflictResponseDTO c : conflicts) {
                    if ("CRITICAL".equalsIgnoreCase(c.getSeverity())) criticalConflicts++;
                    else warningConflicts++;
                }

                DashboardManagerResponseDTO dashboard = new DashboardManagerResponseDTO();
                dashboard.setActiveProjects(activeProjects);
                dashboard.setTotalProjects(projects.size());
                dashboard.setAtRiskProjects(atRiskProjects);
                dashboard.setTotalDevelopers(developers.size());
                dashboard.setAverageDeveloperUtilization(avgUtil);
                dashboard.setTotalSpend(totalSpend);
                dashboard.setTotalBudget(totalBudget);
                dashboard.setTotalRemainingBudget(Math.max(0, totalBudget - totalSpend));
                dashboard.setCriticalConflictCount(criticalConflicts);
                dashboard.setWarningConflictCount(warningConflicts);
                dashboard.setSkillGapCount(skillGaps.size());
                dashboard.setRecentConflicts(conflicts);
                dashboard.setProjectSummaries(projects);

                sendSuccess(exchange, 200, "Manager dashboard loaded", dashboard);
                return;
            }

            if (path.contains("/dashboard/developer/")) {
                // GET /api/dashboard/developer/{id}
                String[] segments = path.split("/");
                int devId = Integer.parseInt(segments[segments.length - 1]);
                Developer dev = developerDAO.findById(devId);
                if (dev == null) {
                    sendError(exchange, 404, "Developer not found.");
                    return;
                }

                List<Allocation> allocs = allocationDAO.findByDeveloperId(devId);
                int totalAlloc = 0;
                List<DashboardDeveloperResponseDTO.AssignedMilestoneDTO> myMilestones = new ArrayList<>();
                for (Allocation a : allocs) {
                    totalAlloc += a.getAllocationPercentage();
                    Milestone m = milestoneDAO.findById(a.getMilestoneId());
                    if (m != null) {
                        DashboardDeveloperResponseDTO.AssignedMilestoneDTO mData = new DashboardDeveloperResponseDTO.AssignedMilestoneDTO();
                        mData.setMilestoneId(m.getId());
                        mData.setName(m.getName());
                        mData.setProjectName(m.getProjectName());
                        mData.setStartDate(m.getStartDate());
                        mData.setEndDate(m.getEndDate());
                        mData.setProgressPercentage(m.getProgressPercentage());
                        mData.setStatus(m.getStatus());
                        mData.setMyAllocationPercentage(a.getAllocationPercentage());
                        myMilestones.add(mData);
                    }
                }

                DashboardDeveloperResponseDTO portalData = new DashboardDeveloperResponseDTO();
                portalData.setDeveloperId(dev.getId());
                portalData.setName(dev.getName());
                portalData.setEmail(dev.getEmail());
                portalData.setRoleTitle(dev.getRoleTitle());
                portalData.setDepartment(dev.getDepartment());
                portalData.setHourlyCost(dev.getHourlyCost());
                portalData.setSkills(dev.getSkills() != null ? dev.getSkills() : new ArrayList<>());
                portalData.setCurrentAllocationPercentage(totalAlloc);
                portalData.setRemainingCapacityPercentage(Math.max(0, dev.getAvailabilityPercentage() - totalAlloc));
                portalData.setAssignedMilestones(myMilestones);
                portalData.setPlannedLeaves(availabilityService.getAvailabilityForDeveloper(devId));
                portalData.setNotifications(notificationService.getNotifications(dev.getUserId()));

                sendSuccess(exchange, 200, "Developer portal loaded", portalData);
                return;
            }

            sendError(exchange, 404, "Endpoint not found: " + path);
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Invalid numeric ID parameter");
        } catch (Exception e) {
            sendError(exchange, 500, "Server error: " + e.getMessage());
        }
    }
}
