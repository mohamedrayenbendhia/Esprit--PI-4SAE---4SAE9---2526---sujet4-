package com.smartfreelance.microservice.organizationservice.notification;

import com.smartfreelance.microservice.organizationservice.client.NotificationFeignClient;
import com.smartfreelance.microservice.organizationservice.client.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Implémentation des notifications pour l'organization-service.
 *
 * Chaque méthode est @Async (fire-and-forget) et envoie une notification
 * in-app via Feign → notification-service (port 9090).
 * Les erreurs sont loggées et jamais propagées.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrgNotificationServiceImpl implements OrgNotificationService {

    private final NotificationFeignClient notificationClient;

    // -------------------------------------------------------------------------

    @Override
    @Async
    public void notifyMemberAdded(String orgId, String userId, String orgName) {
        push(userId, "ORG_MEMBER_ADDED",
                "Bienvenue dans l'organisation",
                "Vous avez rejoint l'organisation " + orgName + ".",
                orgId, "ORGANIZATION");
    }

    @Override
    @Async
    public void notifyMemberRemoved(String orgId, String userId, String orgName) {
        push(userId, "ORG_MEMBER_REMOVED",
                "Retrait de l'organisation",
                "Vous avez été retiré de l'organisation " + orgName + ".",
                orgId, "ORGANIZATION");
    }

    @Override
    @Async
    public void notifyInvitationSent(String orgId, String inviteeId, String orgName) {
        push(inviteeId, "ORG_INVITATION",
                "Invitation à rejoindre une organisation",
                "Vous avez reçu une invitation à rejoindre " + orgName + ".",
                orgId, "ORGANIZATION");
    }

    @Override
    @Async
    public void notifyApplicationReceived(String orgId, String ownerId, String applicantId) {
        push(ownerId, "ORG_APPLICATION_RECEIVED",
                "Nouvelle candidature",
                "Votre organisation a reçu une nouvelle candidature.",
                orgId, "ORGANIZATION");
    }

    @Override
    @Async
    public void notifyApplicationResponded(String applicantId, String orgName, boolean accepted) {
        String type    = accepted ? "ORG_APPLICATION_ACCEPTED" : "ORG_APPLICATION_REJECTED";
        String title   = accepted ? "Candidature acceptée" : "Candidature refusée";
        String message = accepted
                ? "Votre candidature pour " + orgName + " a été acceptée."
                : "Votre candidature pour " + orgName + " a été refusée.";
        push(applicantId, type, title, message, null, "ORGANIZATION");
    }

    @Override
    @Async
    public void notifyOrganizationVerified(String ownerId, String orgName) {
        push(ownerId, "ORG_VERIFIED",
                "Organisation vérifiée",
                "Votre organisation " + orgName + " a été vérifiée et est maintenant visible publiquement.",
                null, "ORGANIZATION");
    }

    @Override
    @Async
    public void notifyOrganizationSuspended(String ownerId, String orgName, String reason) {
        push(ownerId, "ORG_SUSPENDED",
                "Organisation suspendue",
                "Votre organisation " + orgName + " a été suspendue. Motif : " + reason,
                null, "ORGANIZATION");
    }

    @Override
    @Async
    public void notifyDormancyWarning(String ownerId, String orgName, long inactiveDays) {
        push(ownerId, "ORG_DORMANCY_WARNING",
                "Organisation inactive",
                "Votre organisation « " + orgName + " » est inactive depuis " + inactiveDays
                        + " jours. Reconnectez-vous pour la maintenir active.",
                null, "ORGANIZATION");
    }

    @Override
    @Async
    public void notifyCollabOfferAutoExpired(String ownerId, String orgName, String offerTitle) {
        push(ownerId, "COLLAB_OFFER_EXPIRED",
                "Offre de collaboration clôturée automatiquement",
                "L'offre « " + offerTitle + " » de " + orgName
                        + " a été clôturée automatiquement (aucune candidature en 90 jours).",
                null, "ORGANIZATION");
    }

    // -------------------------------------------------------------------------

    /**
     * Envoie une notification via Feign. Erreurs loggées, jamais propagées.
     */
    private void push(String recipientId, String type, String title,
                      String message, String referenceId, String referenceType) {
        if (recipientId == null || recipientId.isBlank()) return;
        try {
            notificationClient.send(NotificationRequest.builder()
                    .recipientId(recipientId)
                    .type(type)
                    .title(title)
                    .message(message)
                    .referenceId(referenceId)
                    .referenceType(referenceType)
                    .build());
            log.debug("[OrgNotif] Envoyé à {} : {}", recipientId, title);
        } catch (Exception ex) {
            log.warn("[OrgNotif] Service notifications indisponible pour {} ({}) : {}",
                    recipientId, type, ex.getMessage());
        }
    }
}
