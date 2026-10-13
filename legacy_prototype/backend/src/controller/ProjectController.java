package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dto.BudgetResponseDTO;
import dto.MilestoneResponseDTO;
import dto.ProjectRequestDTO;
import dto.ProjectResponseDTO;
import service.BudgetService;
import service.MilestoneService;
import service.ProjectService;
import util.JsonUtil;
import util.ValidationUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProjectController extends BaseController implements HttpHandler {

    private final ProjectService projectService = new ProjectService();
    private final MilestoneService milestoneService = new MilestoneService();
    private final BudgetService budgetService = new BudgetService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleCorsOptions(exchange)) return;

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath(); // /api/projects, /api/projects/{id}, /api/projects/{id}/budget, /api/projects/{id}/milestones
        String[] segments = path.split("/");

        try {
            if ("GET".equalsIgnoreCase(method)) {
                if (segments.length == 3) {
                    // GET /api/projects
                    List<ProjectResponseDTO> list = projectService.getAllProjects();
                    sendSuccess(exchange, 200, "Projects retrieved", list);
                } else if (segments.length == 4) {
                    // GET /api/projects/{id}
                    int id = Integer.parseInt(segments[3]);
                    ProjectResponseDTO p = projectService.getProjectById(id);
                    if (p != null) {
                        sendSuccess(exchange, 200, "Project retrieved", p);
                    } else {
                        sendError(exchange, 404, "Project not found with id " + id);
                    }
                } else if (segments.length >= 5 && "budget".equalsIgnoreCase(segments[4])) {
                    // GET /api/projects/{id}/budget
                    int id = Integer.parseInt(segments[3]);
                    BudgetResponseDTO budget = budgetService.calculateProjectBudget(id);
                    if (budget != null) {
                        sendSuccess(exchange, 200, "Project budget analysis calculated", budget);
                    } else {
                        sendError(exchange, 404, "Project not found with id " + id);
                    }
                } else if (segments.length >= 5 && "milestones".equalsIgnoreCase(segments[4])) {
                    // GET /api/projects/{id}/milestones
                    int id = Integer.parseInt(segments[3]);
                    List<MilestoneResponseDTO> list = milestoneService.getMilestonesByProject(id);
                    sendSuccess(exchange, 200, "Project milestones retrieved", list);
                } else {
                    sendError(exchange, 400, "Invalid route for projects: " + path);
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                String body = readRequestBody(exchange);
                Map<String, Object> req = JsonUtil.parseObject(body);
                String name = (String) req.get("name");
                String start = (String) req.get("startDate");
                String end = (String) req.get("endDate");
                double budget = req.containsKey("totalBudget") ? ((Number) req.get("totalBudget")).doubleValue() : 0.0;

                if (!ValidationUtil.isNotEmpty(name) || !ValidationUtil.isValidDateRange(start, end)) {
                    sendError(exchange, 400, "Valid project name and date range (start <= end) are required.");
                    return;
                }

                ProjectRequestDTO requestDTO = new ProjectRequestDTO();
                requestDTO.setName(name);
                requestDTO.setDescription((String) req.get("description"));
                requestDTO.setStartDate(start);
                requestDTO.setEndDate(end);
                requestDTO.setTotalBudget(budget);
                requestDTO.setCurrentSpend(req.containsKey("currentSpend") ? ((Number) req.get("currentSpend")).doubleValue() : 0.0);
                requestDTO.setStatus(req.containsKey("status") ? (String) req.get("status") : "PLANNED");

                if (req.containsKey("requiredSkillIds")) {
                    List<?> rawList = (List<?>) req.get("requiredSkillIds");
                    List<Integer> sIds = new ArrayList<>();
                    for (Object item : rawList) {
                        if (item instanceof Number) sIds.add(((Number) item).intValue());
                    }
                    requestDTO.setRequiredSkillIds(sIds);
                }

                ProjectResponseDTO created = projectService.createProject(requestDTO);
                sendSuccess(exchange, 201, "Project created successfully", created);

            } else if ("PUT".equalsIgnoreCase(method)) {
                if (segments.length == 4) {
                    int id = Integer.parseInt(segments[3]);
                    String body = readRequestBody(exchange);
                    Map<String, Object> req = JsonUtil.parseObject(body);

                    ProjectRequestDTO requestDTO = new ProjectRequestDTO();
                    if (req.containsKey("name")) requestDTO.setName((String) req.get("name"));
                    if (req.containsKey("description")) requestDTO.setDescription((String) req.get("description"));
                    if (req.containsKey("startDate")) requestDTO.setStartDate((String) req.get("startDate"));
                    if (req.containsKey("endDate")) requestDTO.setEndDate((String) req.get("endDate"));
                    if (req.containsKey("totalBudget")) requestDTO.setTotalBudget(((Number) req.get("totalBudget")).doubleValue());
                    if (req.containsKey("currentSpend")) requestDTO.setCurrentSpend(((Number) req.get("currentSpend")).doubleValue());
                    if (req.containsKey("status")) requestDTO.setStatus((String) req.get("status"));

                    if (req.containsKey("requiredSkillIds")) {
                        List<?> rawList = (List<?>) req.get("requiredSkillIds");
                        List<Integer> sIds = new ArrayList<>();
                        for (Object item : rawList) {
                            if (item instanceof Number) sIds.add(((Number) item).intValue());
                        }
                        requestDTO.setRequiredSkillIds(sIds);
                    }

                    ProjectResponseDTO updated = projectService.updateProject(id, requestDTO);
                    if (updated != null) {
                        sendSuccess(exchange, 200, "Project updated", updated);
                    } else {
                        sendError(exchange, 404, "Project not found with id " + id);
                    }
                } else {
                    sendError(exchange, 400, "A project id is required, e.g. /api/projects/{id}");
                }
            } else if ("DELETE".equalsIgnoreCase(method)) {
                if (segments.length == 4) {
                    int id = Integer.parseInt(segments[3]);
                    boolean deleted = projectService.deleteProject(id);
                    if (deleted) {
                        sendSuccess(exchange, 200, "Project deleted", null);
                    } else {
                        sendError(exchange, 404, "Project not found with id " + id);
                    }
                } else {
                    sendError(exchange, 400, "A project id is required, e.g. /api/projects/{id}");
                }
            } else {
                sendError(exchange, 405, "Method Not Allowed");
            }
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Invalid numeric ID parameter");
        } catch (SQLException e) {
            sendError(exchange, 500, "Database error: " + e.getMessage());
        } catch (Exception e) {
            sendError(exchange, 500, "Internal error: " + e.getMessage());
        }
    }
}
