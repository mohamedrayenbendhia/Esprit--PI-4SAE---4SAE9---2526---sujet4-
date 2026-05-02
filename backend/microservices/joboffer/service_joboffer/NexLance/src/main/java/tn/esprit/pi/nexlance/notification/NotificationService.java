package tn.esprit.pi.nexlance.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final String NOTIFICATION_API = "http://localhost:9090/api/notifications";

    private final SimpMessagingTemplate messagingTemplate;
    private final RestClient restClient = RestClient.builder()
            .baseUrl(NOTIFICATION_API)
            .build();

    public void sendNotification(String userId, Object notification) {
        messagingTemplate.convertAndSendToUser(userId, "/topic/notifications", notification);
    }

    public void sendNotification(String userId, String title, String message, String type, String priority, String extra) {
        sendCentralNotification(userId, type, title, message, extra, type);
    }

    public void notifyNewInvitation(String userId, String sender, String jobTitle, String invitationId) {
        sendCentralNotification(
                userId,
                "INVITATION",
                "Nouvelle invitation",
                "Invitation de " + sender + " pour " + jobTitle,
                invitationId,
                "INVITATION"
        );
    }

    public void notifyNewRecommendation(String userId, String sender, String jobTitle, String recommendationId) {
        sendCentralNotification(
                userId,
                "RECOMMENDATION",
                "Nouvelle recommandation",
                sender + " vous recommande pour " + jobTitle,
                recommendationId,
                "RECOMMENDATION"
        );
    }

    public void notifyRecommendationResponse(String userId, String responder, String status, String recommendationId) {
        sendCentralNotification(
                userId,
                "RECOMMENDATION",
                "Recommandation " + status.toLowerCase(),
                responder + " a repondu a votre recommandation: " + status,
                recommendationId,
                "RECOMMENDATION"
        );
    }

    public void notifyDeadlineApproaching(String userId, String jobTitle, int daysLeft, String priority, String status) {
        sendCentralNotification(
                userId,
                "DEADLINE",
                "Deadline proche",
                "Deadline proche pour " + jobTitle + " (" + daysLeft + " jours)",
                null,
                "JOB_OFFER"
        );
    }

    public void notifyApplicationCreated(String clientId, String jobOfferId, String jobTitle, String freelancerId) {
        sendCentralNotification(
                clientId,
                "APPLICATION",
                "Nouvelle candidature",
                "Un freelance a postule pour " + jobTitle,
                jobOfferId,
                "JOB_OFFER"
        );
    }

    public void notifyApplicationStatusChanged(String freelancerId, String applicationId, String jobTitle, String status) {
        sendCentralNotification(
                freelancerId,
                "APPLICATION",
                "Candidature " + status.toLowerCase(),
                "Votre candidature pour " + jobTitle + " est " + status,
                applicationId,
                "APPLICATION"
        );
    }

    public void notifyJobOfferStatusChanged(String clientId, String jobOfferId, String jobTitle, String status) {
        sendCentralNotification(
                clientId,
                "JOB_OFFER",
                "Offre " + status.toLowerCase(),
                "Votre offre \"" + jobTitle + "\" est maintenant " + status,
                jobOfferId,
                "JOB_OFFER"
        );
    }

    public void sendCentralNotification(String recipientId, String type, String title,
                                        String message, String referenceId, String referenceType) {
        if (recipientId == null || recipientId.isBlank()) {
            return;
        }

        try {
            Map<String, String> body = new HashMap<>();
            body.put("recipientId", recipientId);
            body.put("type", type);
            body.put("title", title);
            body.put("message", message != null ? message : "");
            if (referenceId != null && !referenceId.isBlank()) {
                body.put("referenceId", referenceId);
            }
            if (referenceType != null && !referenceType.isBlank()) {
                body.put("referenceType", referenceType);
            }

            restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Notification sent to {}: {}", recipientId, title);
        } catch (Exception e) {
            log.warn("Failed to send notification to {}: {}", recipientId, e.getMessage());
        }
    }
}
