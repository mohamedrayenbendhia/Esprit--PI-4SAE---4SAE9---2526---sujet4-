package tn.esprit.pi.nexlance.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Get all notifications for a user
     */
    @GetMapping("/{userId}")
    public ResponseEntity<List<Notification>> getNotifications(@PathVariable String userId) {
        return ResponseEntity.ok(notificationService.getNotifications(userId));
    }

    /**
     * Get unread notifications for a user
     */
    @GetMapping("/{userId}/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@PathVariable String userId) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    /**
     * Get unread count for a user
     */
    @GetMapping("/{userId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable String userId) {
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Mark a notification as read
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable UUID notificationId) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId));
    }

    /**
     * Mark all notifications as read for a user
     */
    @PatchMapping("/{userId}/read-all")
    public ResponseEntity<Void> markAllAsRead(@PathVariable String userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Send a test notification (for testing WebSocket push)
     */
    @PostMapping("/test/{userId}")
    public ResponseEntity<Notification> sendTestNotification(@PathVariable String userId) {
        Notification notification = notificationService.sendNotification(
                userId,
                "JOB_OFFER",
                "🔔 Test Notification",
                "This is a test notification to verify WebSocket is working correctly!",
                null,
                null
        );
        return ResponseEntity.ok(notification);
    }

    /**
     * Create a notification (used by other microservices, e.g. chat)
     */
    @PostMapping
    public ResponseEntity<Notification> createNotification(@RequestBody Map<String, String> body) {
        String recipientId = body.get("recipientId");
        String type = body.getOrDefault("type", "PROJECT");
        String title = body.getOrDefault("title", "Notification");
        String message = body.getOrDefault("message", "");
        String referenceId = body.get("referenceId");
        String referenceType = body.get("referenceType");

        if (recipientId == null || recipientId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Notification notification = notificationService.sendNotification(
                recipientId, type, title, message, referenceId, referenceType
        );
        return ResponseEntity.ok(notification);
    }
}
