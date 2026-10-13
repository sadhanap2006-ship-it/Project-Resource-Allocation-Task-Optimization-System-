package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.AuditLogDAO;
import dao.DeveloperDAO;
import dao.EmployeeSkillDAO;
import dao.FeedbackDAO;
import dao.LeaveRequestDAO;
import dao.NotificationDAO;
import model.Developer;
import model.EmployeeSkill;
import model.LeaveRequest;
import model.Notification;
import model.ProfileAuditLog;
import model.TaskFeedback;
import service.SkillTaxonomyService;
import util.JsonUtil;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.*;

public class SkillVerificationController extends BaseController implements HttpHandler {

    private final EmployeeSkillDAO employeeSkillDAO = new EmployeeSkillDAO();
    private final SkillTaxonomyService taxonomyService = new SkillTaxonomyService();
    private final LeaveRequestDAO leaveRequestDAO = new LeaveRequestDAO();
    private final DeveloperDAO developerDAO = new DeveloperDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private final FeedbackDAO feedbackDAO = new FeedbackDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleCorsOptions(exchange)) return;

        String method = exchange.getRequestMethod();
        URI uri = exchange.getRequestURI();
        String path = uri.getPath();
        Map<String, String> queryParams = parseQueryParams(uri.getQuery());

        try {
            // Manager Skill Review Queue
            if (path.equals("/api/skills/verification-queue") && "GET".equalsIgnoreCase(method)) {
                handleGetPendingQueue(exchange);
                return;
            }

            // Manager Approve/Reject/Clarify Skill
            if (path.equals("/api/skills/verify") && "POST".equalsIgnoreCase(method)) {
                handleVerifySkill(exchange);
                return;
            }

            // Skill Endorsements
            if (path.equals("/api/skills/endorse") && "POST".equalsIgnoreCase(method)) {
                handleEndorseSkill(exchange);
                return;
            }

            // Audit Logs
            if (path.startsWith("/api/skills/audit-logs") && "GET".equalsIgnoreCase(method)) {
                handleGetAuditLogs(exchange, queryParams);
                return;
            }

            // Taxonomy Search
            if (path.startsWith("/api/skills/taxonomy/search") && "GET".equalsIgnoreCase(method)) {
                handleSearchTaxonomy(exchange, queryParams);
                return;
            }

            // Post-Task Feedback
            if (path.contains("/feedback") && "POST".equalsIgnoreCase(method)) {
                handlePostTaskFeedback(exchange);
                return;
            }

            // Team Directory
            if (path.equals("/api/team/directory") && "GET".equalsIgnoreCase(method)) {
                handleGetTeamDirectory(exchange, queryParams);
                return;
            }

            // Leave Requests Management
            if (path.equals("/api/leaves/pending") && "GET".equalsIgnoreCase(method)) {
                handleGetPendingLeaves(exchange);
                return;
            }

            if (path.matches("/api/leaves/\\d+/approve") && "POST".equalsIgnoreCase(method)) {
                int leaveId = Integer.parseInt(path.split("/")[3]);
                handleApproveLeave(exchange, leaveId);
                return;
            }

            if (path.matches("/api/leaves/\\d+/reject") && "POST".equalsIgnoreCase(method)) {
                int leaveId = Integer.parseInt(path.split("/")[3]);
                handleRejectLeave(exchange, leaveId);
                return;
            }

            sendError(exchange, 404, "Skill verification endpoint not found: " + path);
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Verification processing error: " + e.getMessage());
        }
    }

    private void handleGetPendingQueue(HttpExchange exchange) throws Exception {
        List<EmployeeSkill> pending = employeeSkillDAO.findAllPending();
        sendSuccess(exchange, 200, "Pending skill verification queue retrieved", pending);
    }

    private void handleVerifySkill(HttpExchange exchange) throws Exception {
        String body = readRequestBody(exchange);
        Map<String, Object> req = JsonUtil.parseObject(body);

        int devId = req.get("developerId") instanceof Number ? ((Number) req.get("developerId")).intValue() : Integer.parseInt(req.get("developerId").toString());
        int skillId = req.get("skillId") instanceof Number ? ((Number) req.get("skillId")).intValue() : Integer.parseInt(req.get("skillId").toString());
        String action = (String) req.get("action"); // "APPROVE", "REJECT", "REQUEST_CLARIFICATION"
        String comments = (String) req.getOrDefault("comments", "");
        int managerId = req.get("managerId") instanceof Number ? ((Number) req.get("managerId")).intValue() : 1;

        String newStatus;
        if ("APPROVE".equalsIgnoreCase(action)) {
            newStatus = "VERIFIED_MANAGER";
        } else if ("REJECT".equalsIgnoreCase(action)) {
            newStatus = "REJECTED";
        } else {
            newStatus = "PENDING"; // Keep pending, request clarification
        }

        boolean ok = employeeSkillDAO.updateStatus(devId, skillId, newStatus);
        if (ok) {
            // Log audit
            ProfileAuditLog log = new ProfileAuditLog(0, devId, managerId, "SKILL_" + action.toUpperCase(), "PENDING", newStatus + ": " + comments);
            auditLogDAO.insert(log);

            // Send system notification to employee
            Notification notif = new Notification();
            notif.setUserId(devId);
            notif.setTitle("Skill Verification: " + action.toUpperCase());
            notif.setMessage("Manager reviewed your skill: " + (action.equals("APPROVE") ? "Verified by manager!" : comments));
            notif.setType("SYSTEM_ALERT");
            notif.setSeverity(action.equals("APPROVE") ? "INFO" : "WARNING");
            notificationDAO.insert(notif);

            sendSuccess(exchange, 200, "Skill verification status updated to " + newStatus, null);
        } else {
            sendError(exchange, 500, "Failed to update skill status.");
        }
    }

    private void handleEndorseSkill(HttpExchange exchange) throws Exception {
        String body = readRequestBody(exchange);
        Map<String, Object> req = JsonUtil.parseObject(body);

        int devId = req.get("developerId") instanceof Number ? ((Number) req.get("developerId")).intValue() : Integer.parseInt(req.get("developerId").toString());
        int skillId = req.get("skillId") instanceof Number ? ((Number) req.get("skillId")).intValue() : Integer.parseInt(req.get("skillId").toString());
        int endorserId = req.get("endorserId") instanceof Number ? ((Number) req.get("endorserId")).intValue() : 1;

        boolean ok = employeeSkillDAO.incrementEndorsement(devId, skillId);
        if (ok) {
            auditLogDAO.insert(new ProfileAuditLog(0, devId, endorserId, "SKILL_ENDORSED", "Count", "+1 Endorsement"));
            sendSuccess(exchange, 200, "Skill endorsed successfully", null);
        } else {
            sendError(exchange, 500, "Failed to endorse skill.");
        }
    }

    private void handleGetAuditLogs(HttpExchange exchange, Map<String, String> query) throws Exception {
        String devIdStr = query.get("developerId");
        int devId = devIdStr != null ? Integer.parseInt(devIdStr) : 1;
        List<ProfileAuditLog> logs = auditLogDAO.findByDeveloperId(devId);
        sendSuccess(exchange, 200, "Audit logs retrieved", logs);
    }

    private void handleSearchTaxonomy(HttpExchange exchange, Map<String, String> query) throws Exception {
        String q = query.get("q");
        List<Map<String, String>> results = taxonomyService.searchTaxonomy(q);
        sendSuccess(exchange, 200, "Taxonomy search results", results);
    }

    private void handlePostTaskFeedback(HttpExchange exchange) throws Exception {
        String body = readRequestBody(exchange);
        Map<String, Object> req = JsonUtil.parseObject(body);

        int milestoneId = req.get("milestoneId") instanceof Number ? ((Number) req.get("milestoneId")).intValue() : Integer.parseInt(req.get("milestoneId").toString());
        int devId = req.get("developerId") instanceof Number ? ((Number) req.get("developerId")).intValue() : Integer.parseInt(req.get("developerId").toString());
        int managerId = req.get("managerId") instanceof Number ? ((Number) req.get("managerId")).intValue() : 1;
        int rating = req.get("rating") instanceof Number ? ((Number) req.get("rating")).intValue() : 5;
        String comment = (String) req.getOrDefault("comment", "Great delivery.");

        TaskFeedback tf = new TaskFeedback(0, milestoneId, devId, managerId, rating, comment);
        int id = feedbackDAO.insert(tf);
        tf.setId(id);

        auditLogDAO.insert(new ProfileAuditLog(0, devId, managerId, "TASK_FEEDBACK", "None", rating + " Stars: " + comment));
        sendSuccess(exchange, 201, "Feedback recorded successfully", tf);
    }

    private void handleGetTeamDirectory(HttpExchange exchange, Map<String, String> query) throws Exception {
        String skillFilter = query.get("skill");
        String roleFilter = query.get("role");
        String minAvailStr = query.get("minAvailability");
        int minAvail = minAvailStr != null ? Integer.parseInt(minAvailStr) : 0;

        List<Developer> allDevs = developerDAO.findAll();
        List<Map<String, Object>> directory = new ArrayList<>();

        for (Developer d : allDevs) {
            if (d.getAvailabilityPercentage() < minAvail) continue;
            if (roleFilter != null && !roleFilter.isEmpty() && !d.getRoleTitle().toLowerCase().contains(roleFilter.toLowerCase())) continue;

            List<EmployeeSkill> skills = employeeSkillDAO.findByDeveloperId(d.getId());
            if (skillFilter != null && !skillFilter.isEmpty()) {
                boolean match = false;
                for (EmployeeSkill s : skills) {
                    if (s.getSkillName().toLowerCase().contains(skillFilter.toLowerCase())) {
                        match = true;
                        break;
                    }
                }
                if (!match) continue;
            }

            Map<String, Object> item = new HashMap<>();
            item.put("id", d.getId());
            item.put("name", d.getName());
            item.put("email", d.getEmail());
            item.put("roleTitle", d.getRoleTitle());
            item.put("department", d.getDepartment());
            item.put("hourlyCost", d.getHourlyCost());
            item.put("availabilityPercentage", d.getAvailabilityPercentage());
            item.put("status", d.getStatus());
            item.put("skills", skills);
            directory.add(item);
        }

        sendSuccess(exchange, 200, "Team directory retrieved", directory);
    }

    private void handleGetPendingLeaves(HttpExchange exchange) throws Exception {
        List<LeaveRequest> list = leaveRequestDAO.findAllPending();
        sendSuccess(exchange, 200, "Pending leave requests retrieved", list);
    }

    private void handleApproveLeave(HttpExchange exchange, int leaveId) throws Exception {
        String body = readRequestBody(exchange);
        Map<String, Object> req = JsonUtil.parseObject(body);
        int managerId = req.get("managerId") instanceof Number ? ((Number) req.get("managerId")).intValue() : 1;
        String comments = (String) req.getOrDefault("comments", "Approved.");

        boolean ok = leaveRequestDAO.updateStatus(leaveId, "APPROVED", managerId, comments);
        if (ok) {
            sendSuccess(exchange, 200, "Leave request approved successfully.", null);
        } else {
            sendError(exchange, 500, "Could not approve leave request.");
        }
    }

    private void handleRejectLeave(HttpExchange exchange, int leaveId) throws Exception {
        String body = readRequestBody(exchange);
        Map<String, Object> req = JsonUtil.parseObject(body);
        int managerId = req.get("managerId") instanceof Number ? ((Number) req.get("managerId")).intValue() : 1;
        String comments = (String) req.getOrDefault("comments", "Declined due to critical milestone commitments.");

        boolean ok = leaveRequestDAO.updateStatus(leaveId, "REJECTED", managerId, comments);
        if (ok) {
            sendSuccess(exchange, 200, "Leave request rejected.", null);
        } else {
            sendError(exchange, 500, "Could not reject leave request.");
        }
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) {
                params.put(pair[0], pair[1]);
            } else if (pair.length == 1) {
                params.put(pair[0], "");
            }
        }
        return params;
    }
}
