package com.supermarket.web;

import com.supermarket.document.Notification;
import com.supermarket.security.AuthContext;
import com.supermarket.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Notifications produced asynchronously via RabbitMQ, stored in MongoDB. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthContext authContext;

    public NotificationController(NotificationService notificationService, AuthContext authContext) {
        this.notificationService = notificationService;
        this.authContext = authContext;
    }

    @GetMapping
    public List<Notification> list() {
        AuthContext.AuthUser user = authContext.require();
        return notificationService.recentForUser(user.id(), user.branchId());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        AuthContext.AuthUser user = authContext.require();
        return Map.of("unread", notificationService.unreadCount(user.id()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Notification> markRead(@PathVariable String id) {
        AuthContext.AuthUser user = authContext.require();
        Notification notification = notificationService.markRead(id, user.id());
        return notification == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(notification);
    }
}
