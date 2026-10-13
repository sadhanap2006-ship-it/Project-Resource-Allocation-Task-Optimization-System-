package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dto.DeveloperRequestDTO;
import dto.DeveloperResponseDTO;
import service.DeveloperService;
import util.JsonUtil;
import util.ValidationUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DeveloperController extends BaseController implements HttpHandler {

    private final DeveloperService developerService = new DeveloperService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleCorsOptions(exchange)) return;

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath(); // /api/developers or /api/developers/{id}
        String[] segments = path.split("/");

        try {
            if ("GET".equalsIgnoreCase(method)) {
                if (segments.length == 3) {
                    // GET /api/developers
                    List<DeveloperResponseDTO> list = developerService.getAllDevelopers();
                    sendSuccess(exchange, 200, "Developers retrieved successfully", list);
                } else if (segments.length == 4) {
                    // GET /api/developers/{id}
                    int id = Integer.parseInt(segments[3]);
                    DeveloperResponseDTO dto = developerService.getDeveloperById(id);
                    if (dto != null) {
                        sendSuccess(exchange, 200, "Developer retrieved", dto);
                    } else {
                        sendError(exchange, 404, "Developer not found with id " + id);
                    }
                } else {
                    sendError(exchange, 400, "Invalid route for developers: " + path);
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                // POST /api/developers
                String body = readRequestBody(exchange);
                Map<String, Object> req = JsonUtil.parseObject(body);
                String name = (String) req.get("name");
                String email = (String) req.get("email");

                if (!ValidationUtil.isNotEmpty(name) || !ValidationUtil.isValidEmail(email)) {
                    sendError(exchange, 400, "Valid name and email are required.");
                    return;
                }

                DeveloperRequestDTO requestDTO = new DeveloperRequestDTO();
                requestDTO.setName(name);
                requestDTO.setEmail(email);
                requestDTO.setRoleTitle((String) req.get("roleTitle"));
                requestDTO.setDepartment((String) req.get("department"));
                if (req.containsKey("hourlyCost")) {
                    requestDTO.setHourlyCost(((Number) req.get("hourlyCost")).doubleValue());
                }
                if (req.containsKey("availabilityPercentage")) {
                    requestDTO.setAvailabilityPercentage(((Number) req.get("availabilityPercentage")).intValue());
                }
                if (req.containsKey("status")) {
                    requestDTO.setStatus((String) req.get("status"));
                }
                if (req.containsKey("skillIds")) {
                    List<?> rawList = (List<?>) req.get("skillIds");
                    List<Integer> sIds = new ArrayList<>();
                    for (Object item : rawList) {
                        if (item instanceof Number) sIds.add(((Number) item).intValue());
                    }
                    requestDTO.setSkillIds(sIds);
                }

                DeveloperResponseDTO created = developerService.createDeveloper(requestDTO);
                sendSuccess(exchange, 201, "Developer profile created", created);

            } else if ("PUT".equalsIgnoreCase(method)) {
                // PUT /api/developers/{id}
                if (segments.length == 4) {
                    int id = Integer.parseInt(segments[3]);
                    String body = readRequestBody(exchange);
                    Map<String, Object> req = JsonUtil.parseObject(body);

                    DeveloperRequestDTO requestDTO = new DeveloperRequestDTO();
                    if (req.containsKey("name")) requestDTO.setName((String) req.get("name"));
                    if (req.containsKey("email")) requestDTO.setEmail((String) req.get("email"));
                    if (req.containsKey("roleTitle")) requestDTO.setRoleTitle((String) req.get("roleTitle"));
                    if (req.containsKey("department")) requestDTO.setDepartment((String) req.get("department"));
                    if (req.containsKey("hourlyCost")) requestDTO.setHourlyCost(((Number) req.get("hourlyCost")).doubleValue());
                    if (req.containsKey("availabilityPercentage")) requestDTO.setAvailabilityPercentage(((Number) req.get("availabilityPercentage")).intValue());
                    if (req.containsKey("status")) requestDTO.setStatus((String) req.get("status"));

                    DeveloperResponseDTO updated = developerService.updateDeveloper(id, requestDTO);
                    if (updated != null) {
                        sendSuccess(exchange, 200, "Developer updated", updated);
                    } else {
                        sendError(exchange, 404, "Developer not found with id " + id);
                    }
                } else {
                    sendError(exchange, 400, "A developer id is required, e.g. /api/developers/{id}");
                }
            } else if ("DELETE".equalsIgnoreCase(method)) {
                // DELETE /api/developers/{id}
                if (segments.length == 4) {
                    int id = Integer.parseInt(segments[3]);
                    boolean deleted = developerService.deleteDeveloper(id);
                    if (deleted) {
                        sendSuccess(exchange, 200, "Developer deleted", null);
                    } else {
                        sendError(exchange, 404, "Developer not found with id " + id);
                    }
                } else {
                    sendError(exchange, 400, "A developer id is required, e.g. /api/developers/{id}");
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
