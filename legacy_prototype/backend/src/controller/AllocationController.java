package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dto.AllocationRequestDTO;
import dto.AllocationResponseDTO;
import service.AllocationService;
import util.JsonUtil;
import util.ValidationUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AllocationController extends BaseController implements HttpHandler {

    private final AllocationService allocationService = new AllocationService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleCorsOptions(exchange)) return;

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath(); // /api/allocations or /api/allocations/{id}
        String[] segments = path.split("/");

        try {
            if ("GET".equalsIgnoreCase(method)) {
                if (segments.length == 3) {
                    List<AllocationResponseDTO> list = allocationService.getAllAllocations();
                    sendSuccess(exchange, 200, "Allocations retrieved", list);
                } else if (segments.length == 4) {
                    int id = Integer.parseInt(segments[3]);
                    AllocationResponseDTO dto = allocationService.getAllocationById(id);
                    if (dto != null) {
                        sendSuccess(exchange, 200, "Allocation retrieved", dto);
                    } else {
                        sendError(exchange, 404, "Allocation not found with id " + id);
                    }
                } else {
                    sendError(exchange, 400, "Invalid route for allocations: " + path);
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                String body = readRequestBody(exchange);
                Map<String, Object> req = JsonUtil.parseObject(body);
                int devId = req.containsKey("developerId") ? ((Number) req.get("developerId")).intValue() : 0;
                int milestoneId = req.containsKey("milestoneId") ? ((Number) req.get("milestoneId")).intValue() : 0;
                int pct = req.containsKey("allocationPercentage") ? ((Number) req.get("allocationPercentage")).intValue() : 0;

                if (devId <= 0 || milestoneId <= 0 || !ValidationUtil.isValidPercentage(pct)) {
                    sendError(exchange, 400, "Valid developerId, milestoneId, and allocation percentage (1-100%) are required.");
                    return;
                }

                AllocationRequestDTO requestDTO = new AllocationRequestDTO(devId, milestoneId, pct);
                AllocationResponseDTO created = allocationService.createAllocation(requestDTO);
                if (created != null) {
                    sendSuccess(exchange, 201, "Resource allocated successfully", created);
                } else {
                    sendError(exchange, 400, "Failed to create allocation. Check if developer and milestone exist.");
                }
            } else if ("DELETE".equalsIgnoreCase(method)) {
                if (segments.length >= 4) {
                    int id = Integer.parseInt(segments[3]);
                    boolean deleted = allocationService.deleteAllocation(id);
                    if (deleted) {
                        sendSuccess(exchange, 200, "Allocation removed", null);
                    } else {
                        sendError(exchange, 404, "Allocation not found with id " + id);
                    }
                } else {
                    sendError(exchange, 400, "An allocation id is required, e.g. /api/allocations/{id}");
                }
            } else {
                sendError(exchange, 405, "Method Not Allowed");
            }
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Invalid numeric allocation id");
        } catch (Exception e) {
            sendError(exchange, 500, "Server error: " + e.getMessage());
        }
    }
}
