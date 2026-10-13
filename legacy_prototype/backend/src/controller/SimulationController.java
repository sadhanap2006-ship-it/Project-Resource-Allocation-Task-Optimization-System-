package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dto.SimulationResponseDTO;
import model.WhatIfScenario;
import service.SimulationService;
import util.JsonUtil;
import util.PdfGenerator;

import java.io.IOException;
import java.util.Map;

public class SimulationController extends BaseController implements HttpHandler {

    private final SimulationService simulationService = new SimulationService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleCorsOptions(exchange)) return;

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath(); // /api/simulations, /api/simulations/{id}, /api/simulations/{id}/apply, /api/simulations/{id}/report
        String[] segments = path.split("/");

        try {
            if ("POST".equalsIgnoreCase(method)) {
                if (segments.length >= 5 && "apply".equalsIgnoreCase(segments[4])) {
                    // POST /api/simulations/{id}/apply
                    int id = Integer.parseInt(segments[3]);
                    boolean applied = simulationService.applySimulation(id);
                    if (applied) {
                        sendSuccess(exchange, 200, "Simulated swap applied to real project schedule", null);
                    } else {
                        sendError(exchange, 404, "Simulation record not found or could not be applied.");
                    }
                    return;
                }

                if (segments.length == 3 || segments.length == 4) {
                    // POST /api/simulations (Run in-memory swap simulation)
                    String body = readRequestBody(exchange);
                    Map<String, Object> req = JsonUtil.parseObject(body);
                    int mId = req.containsKey("milestoneId") ? ((Number) req.get("milestoneId")).intValue() : 0;
                    int curDevId = req.containsKey("currentDeveloperId") ? ((Number) req.get("currentDeveloperId")).intValue() : 0;
                    int propDevId = req.containsKey("proposedDeveloperId") ? ((Number) req.get("proposedDeveloperId")).intValue() : 0;
                    int allocPct = req.containsKey("allocationPercentage") ? ((Number) req.get("allocationPercentage")).intValue() : 60;

                    if (mId <= 0 || curDevId <= 0 || propDevId <= 0) {
                        sendError(exchange, 400, "Valid milestoneId, currentDeveloperId, and proposedDeveloperId are required.");
                        return;
                    }

                    if (curDevId == propDevId) {
                        sendError(exchange, 400, "Current and proposed developers must be different.");
                        return;
                    }

                    WhatIfScenario scenario = simulationService.simulateSwap(mId, curDevId, propDevId, allocPct);
                    if (scenario != null) {
                        SimulationResponseDTO dto = simulationService.mapToDTO(scenario);
                        sendSuccess(exchange, 200, "What-if simulation evaluated (Database NOT modified)", dto);
                    } else {
                        sendError(exchange, 404, "Target milestone or developer records not found.");
                    }
                } else {
                    sendError(exchange, 400, "Invalid simulation route: " + path);
                }

            } else if ("GET".equalsIgnoreCase(method)) {
                // GET /api/simulations/{id} or /api/simulations/{id}/report
                if (segments.length >= 4) {
                    int id = Integer.parseInt(segments[3]);

                    if (segments.length >= 5 && ("report".equalsIgnoreCase(segments[4]) || "pdf".equalsIgnoreCase(segments[4]))) {
                        // GET /api/simulations/{id}/report -> Binary PDF Download
                        WhatIfScenario scenario = simulationService.getSimulation(id);
                        if (scenario == null) {
                            sendError(exchange, 404, "Simulation scenario not found with id " + id);
                            return;
                        }
                        byte[] pdfBytes = PdfGenerator.generateSimulationReport(scenario);
                        sendBinaryResponse(exchange, 200, "application/pdf", "simulation-report-" + id + ".pdf", pdfBytes);
                        return;
                    }

                    SimulationResponseDTO dto = simulationService.getSimulationDTO(id);
                    if (dto != null) {
                        sendSuccess(exchange, 200, "Simulation scenario retrieved", dto);
                    } else {
                        sendError(exchange, 404, "Simulation scenario not found with id " + id);
                    }
                } else {
                    // A1 FIX: GET without simulation id explicitly returns 400 Bad Request
                    sendError(exchange, 400, "A simulation id is required, e.g. /api/simulations/{id}");
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
