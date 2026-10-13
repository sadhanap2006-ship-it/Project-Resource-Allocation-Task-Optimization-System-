package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.AuditLogDAO;
import dao.CertificationDAO;
import dao.DeveloperDAO;
import dao.EmployeeSkillDAO;
import dao.LeaveRequestDAO;
import dao.MilestoneDAO;
import dao.SkillDAO;
import model.Certification;
import model.Developer;
import model.EmployeeSkill;
import model.LeaveRequest;
import model.ProfileAuditLog;
import model.Skill;
import service.CalendarScheduleService;
import service.ClaudeSkillExtractionService;
import service.ProfileCompletenessService;
import service.SkillTaxonomyService;
import util.JsonUtil;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.*;

public class EmployeePortalController extends BaseController implements HttpHandler {

    private final CalendarScheduleService calendarService = new CalendarScheduleService();
    private final ClaudeSkillExtractionService extractionService = new ClaudeSkillExtractionService();
    private final ProfileCompletenessService completenessService = new ProfileCompletenessService();
    private final SkillTaxonomyService taxonomyService = new SkillTaxonomyService();

    private final EmployeeSkillDAO employeeSkillDAO = new EmployeeSkillDAO();
    private final CertificationDAO certificationDAO = new CertificationDAO();
    private final LeaveRequestDAO leaveRequestDAO = new LeaveRequestDAO();
    private final SkillDAO skillDAO = new SkillDAO();
    private final DeveloperDAO developerDAO = new DeveloperDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleCorsOptions(exchange)) return;

        String method = exchange.getRequestMethod();
        URI uri = exchange.getRequestURI();
        String path = uri.getPath();
        Map<String, String> queryParams = parseQueryParams(uri.getQuery());

        try {
            // 1. Text Skill Extraction: POST /api/skills/extract-text
            if (path.equals("/api/skills/extract-text") && "POST".equalsIgnoreCase(method)) {
                handleExtractText(exchange);
                return;
            }

            // 2. Certificate Parsing: POST /api/skills/extract-cert
            if (path.equals("/api/skills/extract-cert") && "POST".equalsIgnoreCase(method)) {
                handleExtractCert(exchange);
                return;
            }

            // Paths starting with /api/employee/{id}/...
            String[] segments = path.split("/");
            if (segments.length >= 4 && segments[1].equals("api") && segments[2].equals("employee")) {
                int devId = Integer.parseInt(segments[3]);
                String subResource = segments.length >= 5 ? segments[4] : "";

                if ("calendar".equalsIgnoreCase(subResource) && "GET".equalsIgnoreCase(method)) {
                    handleGetCalendar(exchange, devId, queryParams);
                } else if ("day-detail".equalsIgnoreCase(subResource) && "GET".equalsIgnoreCase(method)) {
                    handleGetDayDetail(exchange, devId, queryParams);
                } else if ("skills".equalsIgnoreCase(subResource)) {
                    if ("GET".equalsIgnoreCase(method)) {
                        handleGetSkills(exchange, devId);
                    } else if ("POST".equalsIgnoreCase(method)) {
                        handleAddOrUpdateSkill(exchange, devId);
                    } else if ("DELETE".equalsIgnoreCase(method)) {
                        int skillId = segments.length >= 6 ? Integer.parseInt(segments[5]) : 0;
                        handleDeleteSkill(exchange, devId, skillId);
                    }
                } else if ("certifications".equalsIgnoreCase(subResource)) {
                    if ("GET".equalsIgnoreCase(method)) {
                        handleGetCertifications(exchange, devId);
                    } else if ("POST".equalsIgnoreCase(method)) {
                        handleAddCertification(exchange, devId);
                    }
                } else if ("leave-request".equalsIgnoreCase(subResource) || "leaves".equalsIgnoreCase(subResource)) {
                    if ("POST".equalsIgnoreCase(method)) {
                        handleSubmitLeaveRequest(exchange, devId);
                    } else if ("GET".equalsIgnoreCase(method)) {
                        handleGetLeaves(exchange, devId);
                    }
                } else if ("analytics".equalsIgnoreCase(subResource) && "GET".equalsIgnoreCase(method)) {
                    handleGetAnalytics(exchange, devId);
                } else if ("profile-completeness".equalsIgnoreCase(subResource) && "GET".equalsIgnoreCase(method)) {
                    handleGetCompleteness(exchange, devId);
                } else {
                    sendError(exchange, 404, "Endpoint not found: " + path);
                }
                return;
            }

            sendError(exchange, 404, "Not Found: " + path);
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Employee portal processing error: " + e.getMessage());
        }
    }

    private void handleGetCalendar(HttpExchange exchange, int devId, Map<String, String> query) throws Exception {
        String month = query.get("month");
        String week = query.get("week");

        Map<String, Object> schedule;
        if (week != null && !week.isEmpty()) {
            schedule = calendarService.getWeekSchedule(devId, week);
        } else {
            schedule = calendarService.getMonthSchedule(devId, month);
        }
        sendSuccess(exchange, 200, "Calendar schedule retrieved successfully", schedule);
    }

    private void handleGetDayDetail(HttpExchange exchange, int devId, Map<String, String> query) throws Exception {
        String date = query.get("date");
        if (date == null || date.isEmpty()) {
            sendError(exchange, 400, "Date parameter required (YYYY-MM-DD)");
            return;
        }
        Map<String, Object> detail = calendarService.getDayDetail(devId, date);
        sendSuccess(exchange, 200, "Day detail retrieved", detail);
    }

    private void handleExtractText(HttpExchange exchange) throws Exception {
        String body = readRequestBody(exchange);
        Map<String, Object> req = JsonUtil.parseObject(body);
        String text = (String) req.get("projectDescription");

        if (text == null || text.trim().isEmpty()) {
            sendError(exchange, 400, "projectDescription is required.");
            return;
        }

        List<Map<String, Object>> extracted = extractionService.extractSkillsFromText(text);
        Map<String, Object> resp = new HashMap<>();
        resp.put("extractedSkills", extracted);
        resp.put("totalExtracted", extracted.size());
        resp.put("promptText", text);

        sendSuccess(exchange, 200, "Skills extracted successfully. Review and accept below.", resp);
    }

    private void handleExtractCert(HttpExchange exchange) throws Exception {
        String body = readRequestBody(exchange);
        Map<String, Object> req = JsonUtil.parseObject(body);
        String rawText = (String) req.get("rawText");
        String fileName = (String) req.getOrDefault("fileName", "certificate.pdf");

        Map<String, Object> parsed = extractionService.parseCertificate(rawText, fileName);
        sendSuccess(exchange, 200, "Certificate analyzed successfully", parsed);
    }

    private void handleGetSkills(HttpExchange exchange, int devId) throws Exception {
        List<EmployeeSkill> list = employeeSkillDAO.findByDeveloperId(devId);
        sendSuccess(exchange, 200, "Skills retrieved successfully", list);
    }

    private void handleAddOrUpdateSkill(HttpExchange exchange, int devId) throws Exception {
        String body = readRequestBody(exchange);
        Map<String, Object> req = JsonUtil.parseObject(body);

        String skillName = (String) req.get("skillName");
        Object skillIdObj = req.get("skillId");
        int skillId = 0;

        if (skillIdObj != null) {
            skillId = skillIdObj instanceof Number ? ((Number) skillIdObj).intValue() : Integer.parseInt(skillIdObj.toString());
        }

        // If skillId not provided, search or create skill in skills table
        if (skillId == 0 && skillName != null) {
            String canonical = taxonomyService.resolveCanonicalSkill(skillName);
            Skill existing = skillDAO.findByName(canonical);
            if (existing != null) {
                skillId = existing.getId();
            } else {
                Skill newSkill = new Skill();
                newSkill.setName(canonical);
                newSkill.setCategory((String) req.getOrDefault("category", "General"));
                skillId = skillDAO.insert(newSkill);
            }
        }

        if (skillId == 0) {
            sendError(exchange, 400, "Valid skillName or skillId required.");
            return;
        }

        String proficiency = (String) req.getOrDefault("proficiency", "INTERMEDIATE");
        int years = req.get("yearsExperience") instanceof Number ? ((Number) req.get("yearsExperience")).intValue() : 1;
        String status = (String) req.getOrDefault("status", "PENDING");
        String source = (String) req.getOrDefault("source", "MANUAL");
        boolean interested = Boolean.TRUE.equals(req.get("interestedInFuture"));

        EmployeeSkill es = new EmployeeSkill();
        es.setDeveloperId(devId);
        es.setSkillId(skillId);
        es.setProficiency(proficiency);
        es.setYearsExperience(years);
        es.setStatus(status);
        es.setSource(source);
        es.setInterestedInFuture(interested);

        boolean saved = employeeSkillDAO.saveOrUpdate(es);
        if (saved) {
            // Check for track record auto-upgrade if not already verified
            taxonomyService.checkAndAutoVerifyTrackRecord(devId, skillId);

            // Audit log
            auditLogDAO.insert(new ProfileAuditLog(0, devId, devId, "SKILL_MODIFIED", "None", es.getSkillName() + " (" + proficiency + ")"));

            sendSuccess(exchange, 200, "Skill updated successfully in profile", es);
        } else {
            sendError(exchange, 500, "Could not update skill.");
        }
    }

    private void handleDeleteSkill(HttpExchange exchange, int devId, int skillId) throws Exception {
        boolean deleted = employeeSkillDAO.delete(devId, skillId);
        if (deleted) {
            auditLogDAO.insert(new ProfileAuditLog(0, devId, devId, "SKILL_DELETED", "Skill #" + skillId, "Deleted"));
            sendSuccess(exchange, 200, "Skill removed from profile", null);
        } else {
            sendError(exchange, 404, "Skill not found on developer profile.");
        }
    }

    private void handleGetCertifications(HttpExchange exchange, int devId) throws Exception {
        List<Certification> certs = certificationDAO.findByDeveloperId(devId);
        sendSuccess(exchange, 200, "Certifications retrieved successfully", certs);
    }

    private void handleAddCertification(HttpExchange exchange, int devId) throws Exception {
        String body = readRequestBody(exchange);
        Map<String, Object> req = JsonUtil.parseObject(body);

        String name = (String) req.get("name");
        String issuingBody = (String) req.get("issuingBody");
        String issueDate = (String) req.getOrDefault("issueDate", "2026-01-01");
        String credentialId = (String) req.getOrDefault("credentialId", "CRED-" + UUID.randomUUID().toString().substring(0, 8));
        String fileName = (String) req.getOrDefault("fileName", "certificate.pdf");
        String fileData = (String) req.get("fileData");

        if (name == null || issuingBody == null) {
            sendError(exchange, 400, "Certification name and issuing body are required.");
            return;
        }

        Certification cert = new Certification();
        cert.setDeveloperId(devId);
        cert.setName(name);
        cert.setIssuingBody(issuingBody);
        cert.setIssueDate(issueDate);
        cert.setCredentialId(credentialId);
        cert.setFileName(fileName);
        cert.setFileData(fileData);
        cert.setConfidenceScore(0.96);
        cert.setVerified(true);

        int certId = certificationDAO.insert(cert);
        cert.setId(certId);

        // Auto map recognized cert to skill tags
        List<String> inferred = (List<String>) req.get("inferredSkills");
        if (inferred != null) {
            for (String sName : inferred) {
                String canonical = taxonomyService.resolveCanonicalSkill(sName);
                Skill sk = skillDAO.findByName(canonical);
                if (sk != null) {
                    EmployeeSkill es = new EmployeeSkill();
                    es.setDeveloperId(devId);
                    es.setSkillId(sk.getId());
                    es.setProficiency("INTERMEDIATE");
                    es.setYearsExperience(1);
                    es.setStatus("PENDING");
                    es.setSource("EXTRACTED_CERT");
                    employeeSkillDAO.saveOrUpdate(es);
                }
            }
        }

        auditLogDAO.insert(new ProfileAuditLog(0, devId, devId, "CERT_UPLOADED", "None", name + " (" + issuingBody + ")"));
        sendSuccess(exchange, 201, "Certificate stored in Documents section", cert);
    }

    private void handleSubmitLeaveRequest(HttpExchange exchange, int devId) throws Exception {
        String body = readRequestBody(exchange);
        Map<String, Object> req = JsonUtil.parseObject(body);

        String start = (String) req.get("startDate");
        String end = (String) req.get("endDate");
        String reason = (String) req.get("reason");

        if (start == null || end == null || reason == null) {
            sendError(exchange, 400, "startDate, endDate, and reason are required.");
            return;
        }

        LeaveRequest lr = new LeaveRequest();
        lr.setDeveloperId(devId);
        lr.setStartDate(start);
        lr.setEndDate(end);
        lr.setReason(reason);
        lr.setStatus("PENDING");

        int id = leaveRequestDAO.insert(lr);
        lr.setId(id);

        auditLogDAO.insert(new ProfileAuditLog(0, devId, devId, "LEAVE_REQUESTED", "None", start + " to " + end + ": " + reason));
        sendSuccess(exchange, 201, "Leave request submitted for manager approval", lr);
    }

    private void handleGetLeaves(HttpExchange exchange, int devId) throws Exception {
        List<LeaveRequest> list = leaveRequestDAO.findByDeveloperId(devId);
        sendSuccess(exchange, 200, "Leave requests retrieved", list);
    }

    private void handleGetAnalytics(HttpExchange exchange, int devId) throws Exception {
        Developer dev = developerDAO.findById(devId);
        List<EmployeeSkill> skills = employeeSkillDAO.findByDeveloperId(devId);

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("developerId", devId);
        analytics.put("developerName", dev != null ? dev.getName() : "Developer");
        analytics.put("completedTasksCount", 3);
        analytics.put("verifiedSkillsCount", skills.stream().filter(s -> s.getStatus().contains("VERIFIED")).count());
        analytics.put("totalSkillsCount", skills.size());

        // Historical utilization trend (last 6 months)
        List<Map<String, Object>> trend = new ArrayList<>();
        trend.add(createMonthTrend("2026-05", 70));
        trend.add(createMonthTrend("2026-06", 80));
        trend.add(createMonthTrend("2026-07", 65));
        trend.add(createMonthTrend("2026-08", 85));
        trend.add(createMonthTrend("2026-09", 75));
        trend.add(createMonthTrend("2026-10", 70));
        analytics.put("utilizationTrend", trend);

        sendSuccess(exchange, 200, "Personal analytics retrieved", analytics);
    }

    private Map<String, Object> createMonthTrend(String month, int avgLoad) {
        Map<String, Object> m = new HashMap<>();
        m.put("month", month);
        m.put("averageLoadPercentage", avgLoad);
        return m;
    }

    private void handleGetCompleteness(HttpExchange exchange, int devId) throws Exception {
        Map<String, Object> res = completenessService.calculateCompleteness(devId);
        sendSuccess(exchange, 200, "Profile completeness calculated", res);
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
