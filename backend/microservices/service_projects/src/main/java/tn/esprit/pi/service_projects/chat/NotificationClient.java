package tn.esprit.pi.service_projects.chat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * HTTP client to send notifications via the job-offers notification microservice (port 9090).
 */
@Component
@Slf4j
public class NotificationClient {

    private static final String NOTIFICATION_API = "http://localhost:9090/api/notifications";
    private final RestClient restClient;

    public NotificationClient() {
        this.restClient = RestClient.builder()
                .baseUrl(NOTIFICATION_API)
                .build();
    }

    /**
     * Send a notification to a user via the notification microservice.
     * Fire-and-forget — errors are logged but never propagated to the caller.
     */
    public void sendNotification(String recipientId, String type, String title,
                                  String message, String referenceId, String referenceType) {
        try {
            Map<String, String> body = Map.of(
                    "recipientId", recipientId,
                    "type", type,
                    "title", title,
                    "message", message != null ? message : "",
                    "referenceId", referenceId != null ? referenceId : "",
                    "referenceType", referenceType != null ? referenceType : ""
            );

            restClient.post()
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Notification sent to {} : {}", recipientId, title);
        } catch (Exception e) {
            log.warn("Failed to send notification to {}: {}", recipientId, e.getMessage());
        }
    }
}
