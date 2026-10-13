package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dto.SkillDTO;
import service.SkillService;
import util.JsonUtil;
import util.ValidationUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SkillController extends BaseController implements HttpHandler {

    private final SkillService skillService = new SkillService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleCorsOptions(exchange)) return;

        String method = exchange.getRequestMethod();
        try {
            if ("GET".equalsIgnoreCase(method)) {
                List<SkillDTO> skills = skillService.getAllSkills();
                sendSuccess(exchange, 200, "Skills retrieved", skills);
            } else if ("POST".equalsIgnoreCase(method)) {
                String body = readRequestBody(exchange);
                Map<String, Object> req = JsonUtil.parseObject(body);
                String name = (String) req.get("name");
                String cat = (String) req.get("category");

                if (!ValidationUtil.isNotEmpty(name)) {
                    sendError(exchange, 400, "Skill name is required.");
                    return;
                }

                SkillDTO s = skillService.createSkill(name, cat);
                sendSuccess(exchange, 201, "Skill created", s);
            } else {
                sendError(exchange, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            sendError(exchange, 500, "Error: " + e.getMessage());
        }
    }
}
