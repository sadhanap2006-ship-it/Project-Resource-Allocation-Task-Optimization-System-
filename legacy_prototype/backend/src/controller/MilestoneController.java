package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dto.MilestoneRequestDTO;
import dto.MilestoneResponseDTO;
import service.MilestoneService;
import util.JsonUtil;
import util.ValidationUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MilestoneController extends BaseController implements HttpHandler {

    private final MilestoneService milestoneService = new MilestoneService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleCorsOptions(exchange)) return;

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath(); // /api/milestones or /api/milestones/{id}
        String[] segments = path.split("/");

        try {
            if ("GET".equalsIgnoreCase(method)) {
                if (segments.length == 3) {
                    List<MilestoneResponseDTO> list = milestoneService.getAllMilestones();
                    sendSuccess(exchange, 200, "Milestones retrieved", list);
                } else if (segments.length == 4) {
                    int id = Integer.parseInt(segments[3]);
                    MilestoneResponseDTO m = milestoneService.getMilestoneById(id);
                    if (m != null) {
                        sendSuccess(exchange, 200, "Milestone retrieved", m);
                    } else {
                        sendError(exchange, 404, "Milestone not found with id " + id);
                    }
                } else {
                    sendError(exchange, 400, "Invalid route for milestones: " + path);
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                String body = readRequestBody(exchange);
                Map<String, Object> req = JsonUtil.parseObject(body);
                int projectId = req.containsKey("projectId") ? ((Number) req.get("projectId")).intValue() : 0;
                String name = (String) req.get("name");
                String desc = (String) req.get("description");
                String start = (String) req.get("startDate");
                String end = (String) req.get("endDate");
                double effort = req.containsKey("estimatedEffortHours") ? ((Number) req.get("estimatedEffortHours")).doubleValue() : 40.0;
                int progress = req.containsKey("progressPercentage") ? ((Number) req.get("progressPercentage")).intValue() : 0;
                String status = req.containsKey("status") ? (String) req.get("status") : "NOT_STARTED";

                if (projectId <= 0 || !ValidationUtil.isNotEmpty(name) || !ValidationUtil.isValidDateRange(start, end)) {
                    sendError(exchange, 400, "Valid project ID, milestone name, and date range are required.");
                    return;
                }

                MilestoneRequestDTO requestDTO = new MilestoneRequestDTO();
                requestDTO.setProjectId(projectId);
                requestDTO.setName(name);
                requestDTO.setDescription(desc);
                requestDTO.setStartDate(start);
                requestDTO.setEndDate(end);
                requestDTO.setEstimatedEffortHours(effort);
                requestDTO.setProgressPercentage(progress);
                requestDTO.setStatus(status);

                if (req.containsKey("requiredSkillIds")) {
                    List<?> rawList = (List<?>) req.get("requiredSkillIds");
                    List<Integer> sIds = new ArrayList<>();
                    for (Object item : rawList) {
                        if (item instanceof Number) sIds.add(((Number) item).intValue());
                    }
                    requestDTO.setRequiredSkillIds(sIds);
                }

                MilestoneResponseDTO created = milestoneService.createMilestone(requestDTO);
                sendSuccess(exchange, 201, "Milestone created successfully", created);

            } else if ("PUT".equalsIgnoreCase(method)) {
                if (segments.length == 4) {
                    int id = Integer.parseInt(segments[3]);
                    String body = readRequestBody(exchange);
                    Map<String, Object> req = JsonUtil.parseObject(body);

                    MilestoneRequestDTO requestDTO = new MilestoneRequestDTO();
                    if (req.containsKey("name")) requestDTO.setName((String) req.get("name"));
                    if (req.containsKey("description")) requestDTO.setDescription((String) req.get("description"));
                    if (req.containsKey("startDate")) requestDTO.setStartDate((String) req.get("startDate"));
                    if (req.containsKey("endDate")) requestDTO.setEndDate((String) req.get("endDate"));
                    if (req.containsKey("estimatedEffortHours")) requestDTO.setEstimatedEffortHours(((Number) req.get("estimatedEffortHours")).doubleValue());
                    if (req.containsKey("progressPercentage")) requestDTO.setProgressPercentage(((Number) req.get("progressPercentage")).intValue());
                    if (req.containsKey("status")) requestDTO.setStatus((String) req.get("status"));

                    if (req.containsKey("requiredSkillIds")) {
                        List<?> rawList = (List<?>) req.get("requiredSkillIds");
                        List<Integer> sIds = new ArrayList<>();
                        for (Object item : rawList) {
                            if (item instanceof Number) sIds.add(((Number) item).intValue());
                        }
                        requestDTO.setRequiredSkillIds(sIds);
                    }

                    MilestoneResponseDTO updated = milestoneService.updateMilestone(id, requestDTO);
                    if (updated != null) {
                        sendSuccess(exchange, 200, "Milestone updated", updated);
                    } else {
                        sendError(exchange, 404, "Milestone not found with id " + id);
                    }
                } else {
                    sendError(exchange, 400, "A milestone id is required, e.g. /api/milestones/{id}");
                }
            } else if ("DELETE".equalsIgnoreCase(method)) {
                if (segments.length >= 4) {
                    int id = Integer.parseInt(segments[3]);
                    boolean deleted = milestoneService.deleteMilestone(id);
                    if (deleted) {
                        sendSuccess(exchange, 200, "Milestone deleted", null);
                    } else {
                        sendError(exchange, 404, "Milestone not found with id " + id);
                    }
                } else {
                    sendError(exchange, 400, "A milestone id is required, e.g. /api/milestones/{id}");
                }
            } else {
                sendError(exchange, 405, "Method Not Allowed");
            }
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Invalid numeric ID parameter");
        } catch (Exception e) {
            sendError(exchange, 500, "Server error: " + e.getMessage());
        }
    }
}
