package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dto.NotificationResponseDTO;
import service.NotificationService;

import java.io.IOException;
import java.util.List;

public class NotificationController extends BaseController implements HttpHandler {

    private final NotificationService notificationService = new NotificationService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleCorsOptions(exchange)) return;

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath(); // /api/notifications, /api/notifications/{id}/read, /api/notifications/read-all
        String[] segments = path.split("/");

        try {
            if ("GET".equalsIgnoreCase(method)) {
                List<NotificationResponseDTO> list = notificationService.getNotifications(null);
                sendSuccess(exchange, 200, "Notifications retrieved", list);
            } else if ("PUT".equalsIgnoreCase(method)) {
                if (path.endsWith("/read-all")) {
                    notificationService.markAllAsRead();
                    sendSuccess(exchange, 200, "All notifications marked as read", null);
                } else if (segments.length >= 5 && "read".equalsIgnoreCase(segments[4])) {
                    int id = Integer.parseInt(segments[3]);
                    boolean ok = notificationService.markAsRead(id);
                    if (ok) {
                        sendSuccess(exchange, 200, "Notification marked as read", null);
                    } else {
                        sendError(exchange, 404, "Notification not found with id " + id);
                    }
                } else {
                    sendError(exchange, 400, "Invalid notification action: " + path);
                }
            } else {
                sendError(exchange, 405, "Method Not Allowed");
            }
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Invalid numeric notification id");
        } catch (Exception e) {
            sendError(exchange, 500, "Server error: " + e.getMessage());
        }
    }
}
