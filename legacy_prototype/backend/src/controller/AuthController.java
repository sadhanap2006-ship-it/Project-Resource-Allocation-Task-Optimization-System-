package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dto.LoginRequestDTO;
import dto.LoginResponseDTO;
import service.AuthService;
import util.JsonUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthController extends BaseController implements HttpHandler {

    private final AuthService authService = new AuthService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleCorsOptions(exchange)) return;

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("POST".equalsIgnoreCase(method)) {
                String body = readRequestBody(exchange);
                Map<String, Object> req = JsonUtil.parseObject(body);

                if (path.endsWith("/forgot-password")) {
                    handleForgotPassword(exchange, req);
                } else if (path.endsWith("/reset-password")) {
                    handleResetPassword(exchange, req);
                } else if (path.endsWith("/refresh")) {
                    handleRefreshToken(exchange, req);
                } else if (path.endsWith("/logout")) {
                    sendSuccess(exchange, 200, "Logout successful", null);
                } else if (path.endsWith("/onboarding")) {
                    handleOnboarding(exchange, req);
                } else {
                    // Standard Login
                    handleLogin(exchange, req);
                }
            } else {
                sendError(exchange, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            sendError(exchange, 500, "Authentication error: " + e.getMessage());
        }
    }

    private void handleLogin(HttpExchange exchange, Map<String, Object> req) throws Exception {
        String email = (String) req.get("email");
        String password = (String) req.get("password");

        if (email == null || password == null) {
            sendError(exchange, 400, "Email and password are required.");
            return;
        }

        LoginRequestDTO loginReq = new LoginRequestDTO(email, password);
        LoginResponseDTO authData = authService.login(loginReq);
        if (authData != null) {
            sendSuccess(exchange, 200, "Authentication successful", authData);
        } else {
            sendError(exchange, 401, "Invalid email or password.");
        }
    }

    private void handleForgotPassword(HttpExchange exchange, Map<String, Object> req) throws Exception {
        String email = (String) req.get("email");
        if (email == null || email.trim().isEmpty()) {
            sendError(exchange, 400, "Email address is required.");
            return;
        }

        String resetToken = authService.generatePasswordResetToken(email);
        if (resetToken != null) {
            Map<String, Object> res = new HashMap<>();
            res.put("email", email);
            res.put("resetToken", resetToken);
            res.put("simulationCode", resetToken);
            res.put("message", "Password reset token generated. Use this code to complete password reset.");
            sendSuccess(exchange, 200, "Reset token generated successfully", res);
        } else {
            sendError(exchange, 404, "No account associated with " + email);
        }
    }

    private void handleResetPassword(HttpExchange exchange, Map<String, Object> req) throws Exception {
        String resetToken = (String) req.get("resetToken");
        String newPassword = (String) req.get("newPassword");

        if (resetToken == null || newPassword == null || newPassword.trim().isEmpty()) {
            sendError(exchange, 400, "Reset token and new password are required.");
            return;
        }

        boolean reset = authService.resetPassword(resetToken, newPassword);
        if (reset) {
            sendSuccess(exchange, 200, "Password has been successfully updated. You may now log in.", null);
        } else {
            sendError(exchange, 400, "Invalid or expired reset token.");
        }
    }

    private void handleRefreshToken(HttpExchange exchange, Map<String, Object> req) throws Exception {
        String refreshToken = (String) req.get("refreshToken");
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            sendError(exchange, 400, "Refresh token is required.");
            return;
        }

        LoginResponseDTO authData = authService.refreshSession(refreshToken);
        if (authData != null) {
            sendSuccess(exchange, 200, "Session token refreshed", authData);
        } else {
            sendError(exchange, 401, "Invalid or expired refresh token. Please log in again.");
        }
    }

    private void handleOnboarding(HttpExchange exchange, Map<String, Object> req) throws Exception {
        Object userIdObj = req.get("userId");
        Object devIdObj = req.get("developerId");

        if (userIdObj == null || devIdObj == null) {
            sendError(exchange, 400, "userId and developerId are required.");
            return;
        }

        int userId = userIdObj instanceof Number ? ((Number) userIdObj).intValue() : Integer.parseInt(userIdObj.toString());
        int devId = devIdObj instanceof Number ? ((Number) devIdObj).intValue() : Integer.parseInt(devIdObj.toString());

        List<Map<String, Object>> skills = (List<Map<String, Object>>) req.get("skills");
        List<Map<String, Object>> certs = (List<Map<String, Object>>) req.get("certifications");

        boolean completed = authService.completeOnboarding(userId, devId, skills, certs);
        if (completed) {
            sendSuccess(exchange, 200, "Profile onboarding completed successfully.", null);
        } else {
            sendError(exchange, 500, "Failed to complete onboarding.");
        }
    }
}
