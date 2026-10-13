package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dto.AvailabilityRequestDTO;
import dto.AvailabilityResponseDTO;
import service.AvailabilityService;
import util.JsonUtil;
import util.ValidationUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AvailabilityController extends BaseController implements HttpHandler {

    private final AvailabilityService availabilityService = new AvailabilityService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleCorsOptions(exchange)) return;

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath(); // /api/availability or /api/availability/{id}
        String[] segments = path.split("/");

        try {
            if ("GET".equalsIgnoreCase(method)) {
                if (segments.length == 3) {
                    List<AvailabilityResponseDTO> list = availabilityService.getAllAvailabilityRecords();
                    sendSuccess(exchange, 200, "Availability records retrieved", list);
                } else if (segments.length == 4) {
                    int devId = Integer.parseInt(segments[3]);
                    List<AvailabilityResponseDTO> list = availabilityService.getAvailabilityForDeveloper(devId);
                    sendSuccess(exchange, 200, "Developer availability records retrieved", list);
                } else {
                    sendError(exchange, 400, "Invalid route for availability: " + path);
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                String body = readRequestBody(exchange);
                Map<String, Object> req = JsonUtil.parseObject(body);
                int devId = req.containsKey("developerId") ? ((Number) req.get("developerId")).intValue() : 0;
                String start = (String) req.get("startDate");
                String end = (String) req.get("endDate");
                String reason = (String) req.get("reason");
                String status = req.containsKey("status") ? (String) req.get("status") : "APPROVED";

                if (devId <= 0 || !ValidationUtil.isValidDateRange(start, end)) {
                    sendError(exchange, 400, "Valid developer ID and date range are required.");
                    return;
                }

                AvailabilityRequestDTO requestDTO = new AvailabilityRequestDTO();
                requestDTO.setDeveloperId(devId);
                requestDTO.setStartDate(start);
                requestDTO.setEndDate(end);
                requestDTO.setReason(reason != null ? reason : "Planned Vacation");
                requestDTO.setStatus(status);

                AvailabilityResponseDTO created = availabilityService.recordPlannedLeave(requestDTO);
                sendSuccess(exchange, 201, "Leave scheduled successfully", created);

            } else if ("DELETE".equalsIgnoreCase(method)) {
                if (segments.length >= 4) {
                    int id = Integer.parseInt(segments[3]);
                    boolean deleted = availabilityService.deleteAvailabilityRecord(id);
                    if (deleted) {
                        sendSuccess(exchange, 200, "Leave cancelled", null);
                    } else {
                        sendError(exchange, 404, "Availability record not found with id " + id);
                    }
                } else {
                    sendError(exchange, 400, "An availability id is required, e.g. /api/availability/{id}");
                }
            } else {
                sendError(exchange, 405, "Method Not Allowed");
            }
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Invalid numeric id parameter");
        } catch (Exception e) {
            sendError(exchange, 500, "Server error: " + e.getMessage());
        }
    }
}
