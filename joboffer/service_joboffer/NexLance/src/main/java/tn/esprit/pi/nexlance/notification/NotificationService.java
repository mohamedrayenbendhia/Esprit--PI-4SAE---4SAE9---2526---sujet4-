package tn.esprit.pi.nexlance.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Create and send a real-time notification via WebSocket
     */
    public Notification sendNotification(String recipientId, String type, String title, String message,
                                          String referenceId, String referenceType) {
        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .read(false)
                .build();

        notification = notificationRepository.save(notification);

        // Send via WebSocket to user-specific destination
        messagingTemplate.convertAndSend(
                "/topic/notifications/" + recipientId,
                notification
        );

        log.info("Notification sent to user {}: {} - {}", recipientId, type, title);
        return notification;
    }

    /**
     * Notify when a new application is submitted
     */
    public void notifyNewApplication(String clientId, String jobTitle, String freelancerName, String applicationId) {
        sendNotification(clientId, "APPLICATION",
                "New Application Received",
                freelancerName + " applied for your job offer: " + jobTitle,
                applicationId, "APPLICATION");
    }

    /**
     * Notify when an application status changes
     */
    public void notifyApplicationStatusChange(String freelancerId, String jobTitle, String status, String applicationId) {
        sendNotification(freelancerId, "APPLICATION",
                "Application " + status,
                "Your application for \"" + jobTitle + "\" has been " + status.toLowerCase(),
                applicationId, "APPLICATION");
    }

    /**
     * Notify when a new recommendation is created
     */
    public void notifyNewRecommendation(String freelancerId, String clientName, String jobTitle, String recommendationId) {
        sendNotification(freelancerId, "RECOMMENDATION",
                "New Recommendation",
                clientName + " recommended you for: " + jobTitle,
                recommendationId, "RECOMMENDATION");
    }

    /**
     * Notify when a recommendation is accepted/rejected
     */
    public void notifyRecommendationResponse(String clientId, String freelancerName, String status, String recommendationId) {
        sendNotification(clientId, "RECOMMENDATION",
                "Recommendation " + status,
                freelancerName + " has " + status.toLowerCase() + " your recommendation",
                recommendationId, "RECOMMENDATION");
    }

    /**
     * Notify when an invitation is received
     */
    public void notifyNewInvitation(String freelancerId, String clientName, String jobTitle, String invitationId) {
        sendNotification(freelancerId, "INVITATION",
                "New Invitation",
                clientName + " invited you to apply for: " + jobTitle,
                invitationId, "INVITATION");
    }

    /**
     * Notify when a milestone is submitted for review
     */
    public void notifyMilestoneSubmitted(String clientId, String projectTitle, String milestoneName, String milestoneId) {
        sendNotification(clientId, "MILESTONE",
                "Milestone Submitted for Review",
                "A milestone \"" + milestoneName + "\" in project \"" + projectTitle + "\" has been submitted",
                milestoneId, "MILESTONE");
    }

    /**
     * Notify when a milestone is approved/rejected
     */
    public void notifyMilestoneReview(String freelancerId, String milestoneName, String status, String milestoneId) {
        sendNotification(freelancerId, "MILESTONE",
                "Milestone " + status,
                "Your milestone \"" + milestoneName + "\" has been " + status.toLowerCase(),
                milestoneId, "MILESTONE");
    }

    /**
     * Notify about deadline approaching
     */
    public void notifyDeadlineApproaching(String userId, String entityName, int daysRemaining, String referenceId, String referenceType) {
        sendNotification(userId, "DEADLINE",
                "Deadline Approaching",
                entityName + " is due in " + daysRemaining + " day(s)",
                referenceId, referenceType);
    }

    /**
     * Get all notifications for a user
     */
    public List<Notification> getNotifications(String recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
    }

    /**
     * Get unread notifications for a user
     */
    public List<Notification> getUnreadNotifications(String recipientId) {
        return notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(recipientId);
    }

    /**
     * Get unread count
     */
    public long getUnreadCount(String recipientId) {
        return notificationRepository.countByRecipientIdAndReadFalse(recipientId);
    }

    /**
     * Mark notification as read
     */
    public Notification markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    /**
     * Mark all notifications as read for a user
     */
    public void markAllAsRead(String recipientId) {
        List<Notification> unread = notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(recipientId);
        unread.forEach(n -> {
            n.setRead(true);
            n.setReadAt(LocalDateTime.now());
        });
        notificationRepository.saveAll(unread);
    }
}
