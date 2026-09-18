package com.sist.rms.controller;

import com.sist.rms.dto.NotificationResponse;
import com.sist.rms.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** REST API for the in-app notifications. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> getAll(@RequestParam(required = false) Long developerId) {
        return developerId == null
                ? notificationService.findAll()
                : notificationService.findForDeveloper(developerId);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount() {
        return Map.of("unread", notificationService.countUnread());
    }

    /** Re-scans the whole system and stores any new alerts. */
    @PostMapping("/refresh")
    public List<NotificationResponse> refresh() {
        return notificationService.refresh();
    }

    @PutMapping("/{id}/read")
    public NotificationResponse markAsRead(@PathVariable Long id) {
        return notificationService.markAsRead(id);
    }

    @DeleteMapping
    public ResponseEntity<Void> clearAll() {
        notificationService.deleteAll();
        return ResponseEntity.noContent().build();
    }
}
