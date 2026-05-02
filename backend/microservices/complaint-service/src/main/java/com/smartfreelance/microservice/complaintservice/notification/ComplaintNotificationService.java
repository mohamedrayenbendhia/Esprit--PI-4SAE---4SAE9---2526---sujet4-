package com.smartfreelance.microservice.complaintservice.notification;

import com.smartfreelance.microservice.complaintservice.client.NotificationFeignClient;
import com.smartfreelance.microservice.complaintservice.client.NotificationRequest;
import com.smartfreelance.microservice.complaintservice.email.ComplaintEmailService;
import com.smartfreelance.microservice.complaintservice.service.ComplaintEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Orchestrateur des notifications liées aux réclamations.
 *
 * Responsabilités :
 *  1. Envoyer une notification push (in-app) via Feign → notification-service (port 9090)
 *  2. Déclencher l'envoi d'email via ComplaintEmailService
 *
 * Architecture : fire-and-forget (@Async) — les erreurs de notification ne bloquent
 * jamais l'opération métier principale.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintNotificationService {

    private final ComplaintEmailService       emailService;
    private final NotificationFeignClient     notificationClient;
    private final ComplaintEventService       eventService;

    @Value("${frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    // =========================================================================
    // POINT D'ENTRÉE PRINCIPAL
    // =========================================================================

    /**
     * Traite un événement et déclenche toutes les notifications appropriées.
     * @Async : fire-and-forget — le thread appelant n'attend pas la fin des notifications.
     */
    @Async
    public void handle(ComplaintNotificationEvent event) {
        // ── Audit trail (synchrone dans le thread @Async) ───────────────────────
        // NEW_MESSAGE n'est pas tracé individuellement (trop de volume).
        // COMPLAINT_REOPENED est tracé côté ReopenController directement.
        if (event.getEventType() != ComplaintNotificationEvent.EventType.NEW_MESSAGE) {
            try {
                eventService.record(event);
            } catch (Exception auditEx) {
                log.warn("[AuditTrail] Erreur persistance événement {} : {}",
                        event.getEventType(), auditEx.getMessage());
            }
        }

        try {
            switch (event.getEventType()) {
                case COMPLAINT_CREATED     -> handleCreated(event);
                case STATUS_CHANGED        -> handleStatusChanged(event);
                case COMPLAINT_RESOLVED    -> handleResolved(event);
                case COMPLAINT_CLOSED      -> handleClosed(event);
                case NEW_MESSAGE           -> handleNewMessage(event);
                case REPORTED_INVOLVED     -> handleReportedInvolved(event);
                case COMPLAINT_ASSIGNED    -> handleAssigned(event);
                case COMPLAINT_ESCALATED   -> handleEscalated(event);
                case PRIORITY_CHANGED      -> handlePriorityChanged(event);
                case COMPLAINT_REOPENED    -> handleReopened(event);
                case MEDIATION_OPENED      -> handleMediationOpened(event);
                case MEDIATION_DECIDED     -> handleMediationDecided(event);
                case NPS_SURVEY_READY      -> handleNpsSurveyReady(event);
                case SLA_BREACH            -> handleSlaBreach(event);
                case SANCTION_APPLIED      -> handleSanctionApplied(event);
            }
        } catch (Exception e) {
            log.error("[Notifications] Erreur lors du traitement de l'événement {} : {}",
                    event.getEventType(), e.getMessage());
        }
    }

    // =========================================================================
    // HANDLERS PAR TYPE D'ÉVÉNEMENT
    // =========================================================================

    /** 1. Accusé de réception — création d'une réclamation */
    private void handleCreated(ComplaintNotificationEvent e) {
        String url = buildComplaintUrl(e.getReporterId(), e.getComplaintId());
        pushNotification(e.getReporterId(), "COMPLAINT_CREATED",
                "Réclamation créée — " + e.getTicketNumber(),
                "Votre réclamation \"" + e.getComplaintSubject() + "\" a bien été enregistrée.",
                e.getComplaintId(), "COMPLAINT");
        emailService.sendComplaintCreatedEmail(e.getReporterId(), e.getTicketNumber(),
                e.getComplaintSubject(), url);
    }

    /** 2. Changement de statut */
    private void handleStatusChanged(ComplaintNotificationEvent e) {
        if ("ESCALATED".equals(e.getNewStatus()) ||
            "RESOLVED".equals(e.getNewStatus())  ||
            "CLOSED".equals(e.getNewStatus())) {
            return;
        }
        String url     = buildComplaintUrl(e.getReporterId(), e.getComplaintId());
        String title   = "Statut mis à jour — " + e.getTicketNumber();
        String message = "Votre réclamation est maintenant : " + formatStatus(e.getNewStatus());
        pushNotification(e.getReporterId(), "COMPLAINT_STATUS", title, message,
                e.getComplaintId(), "COMPLAINT");
        emailService.sendStatusChangedEmail(e.getReporterId(), e.getTicketNumber(),
                e.getComplaintSubject(), e.getNewStatus(), url);
        if (e.getAssignedToId() != null && !e.getAssignedToId().equals(e.getReporterId())) {
            pushNotification(e.getAssignedToId(), "COMPLAINT_STATUS",
                    title, message, e.getComplaintId(), "COMPLAINT");
        }
    }

    /** 3. Résolution — reporter + agent (confirmation) */
    private void handleResolved(ComplaintNotificationEvent e) {
        String url = buildComplaintUrl(e.getReporterId(), e.getComplaintId());
        // Reporter : notification principale
        pushNotification(e.getReporterId(), "COMPLAINT_RESOLVED",
                "Réclamation résolue — " + e.getTicketNumber(),
                "Votre réclamation \"" + e.getComplaintSubject() + "\" a été résolue.",
                e.getComplaintId(), "COMPLAINT");
        emailService.sendComplaintResolvedEmail(e.getReporterId(), e.getTicketNumber(),
                e.getComplaintSubject(), url);
        // GAP #7 — Agent : confirmation d'action pour son audit trail
        if (e.getAssignedToId() != null && !e.getAssignedToId().equals(e.getReporterId())) {
            pushNotification(e.getAssignedToId(), "COMPLAINT_RESOLVED",
                    "Résolution confirmée — " + e.getTicketNumber(),
                    "Vous avez marqué \"" + e.getComplaintSubject() + "\" comme résolue.",
                    e.getComplaintId(), "COMPLAINT");
        }
    }

    /** 4. Clôture — reporter + agent (confirmation) */
    private void handleClosed(ComplaintNotificationEvent e) {
        String url = buildComplaintUrl(e.getReporterId(), e.getComplaintId());
        pushNotification(e.getReporterId(), "COMPLAINT_CLOSED",
                "Réclamation clôturée — " + e.getTicketNumber(),
                "Votre réclamation a été clôturée. Vous pouvez maintenant la noter.",
                e.getComplaintId(), "COMPLAINT");
        emailService.sendComplaintClosedEmail(e.getReporterId(), e.getTicketNumber(),
                e.getComplaintSubject(), url);
        // GAP #7 — Agent : confirmation de clôture
        if (e.getAssignedToId() != null && !e.getAssignedToId().equals(e.getReporterId())) {
            pushNotification(e.getAssignedToId(), "COMPLAINT_CLOSED",
                    "Clôture confirmée — " + e.getTicketNumber(),
                    "La réclamation \"" + e.getComplaintSubject() + "\" a été clôturée.",
                    e.getComplaintId(), "COMPLAINT");
        }
    }

    /** 5. Nouveau message */
    private void handleNewMessage(ComplaintNotificationEvent e) {
        if (e.isUnassigned()) {
            // GAP #8 — avant on ignorait silencieusement. Si un admin est fourni
            // en secondaryUserId (queue admin), on le prévient pour triage.
            if (e.getSecondaryUserId() != null && !e.getSecondaryUserId().isBlank()) {
                pushNotification(e.getSecondaryUserId(), "COMPLAINT_QUEUE",
                        "Message en attente — " + e.getTicketNumber(),
                        "Un message a été posté sur une réclamation non assignée.",
                        e.getComplaintId(), "COMPLAINT");
            } else {
                log.info("[Notify] NEW_MESSAGE sur réclamation non assignée {} — aucune cible admin fournie",
                        e.getComplaintId());
            }
            return;
        }
        String recipientId = e.getRecipientId();
        if (recipientId == null) {
            log.warn("[Notify] NEW_MESSAGE : recipientId null pour complaintId={}", e.getComplaintId());
            return;
        }
        String urlPath = buildComplaintUrl(recipientId, e.getComplaintId());
        pushNotification(recipientId, "COMPLAINT_MESSAGE",
                "Nouveau message — " + e.getTicketNumber(),
                e.getSenderName() + " : " + truncate(e.getMessageExcerpt(), 100),
                e.getComplaintId(), "COMPLAINT");
        emailService.sendNewMessageEmailIfInactive(recipientId, e.getTicketNumber(),
                e.getComplaintSubject(), e.getSenderName(),
                truncate(e.getMessageExcerpt(), 200), urlPath);
    }

    /** 6. Implication de la partie mise en cause */
    private void handleReportedInvolved(ComplaintNotificationEvent e) {
        if (e.getReportedUserId() == null) return;
        String url = buildComplaintUrl(e.getReportedUserId(), e.getComplaintId());
        pushNotification(e.getReportedUserId(), "COMPLAINT_INVOLVED",
                "Vous êtes impliqué dans une réclamation",
                "Vous avez été invité à répondre à la réclamation " + e.getTicketNumber(),
                e.getComplaintId(), "COMPLAINT");
        emailService.sendInvolvedEmail(e.getReportedUserId(), e.getTicketNumber(),
                e.getComplaintSubject(), e.getInvitationMessage(), url);
    }

    /** 7. Assignation — agent + reporter (GAP #2) */
    private void handleAssigned(ComplaintNotificationEvent e) {
        // GAP #5 — pas de notification à l'agent s'il s'est auto-assigné
        if (e.getAssignedToId() != null && !e.isSelfAssignment()) {
            pushNotification(e.getAssignedToId(), "COMPLAINT_ASSIGNED",
                    "Réclamation assignée — " + e.getTicketNumber(),
                    "La réclamation \"" + e.getComplaintSubject() + "\" vous a été assignée.",
                    e.getComplaintId(), "COMPLAINT");
        }
        // GAP #2 — prévenir le reporter que sa plainte est prise en charge
        if (e.getReporterId() != null
                && !e.getReporterId().equals(e.getAssignedToId())) {
            pushNotification(e.getReporterId(), "COMPLAINT_ASSIGNED",
                    "Votre réclamation est prise en charge — " + e.getTicketNumber(),
                    "Un agent du support traite désormais votre réclamation \"" +
                    e.getComplaintSubject() + "\".",
                    e.getComplaintId(), "COMPLAINT");
        }
    }

    /** 8. Escalade — agent + reporter (push + email) (GAP #3) */
    private void handleEscalated(ComplaintNotificationEvent e) {
        String title   = "Réclamation escaladée — " + e.getTicketNumber();
        String message = "La réclamation \"" + e.getComplaintSubject() +
                         "\" a été transmise à un superviseur.";
        if (e.getAssignedToId() != null) {
            pushNotification(e.getAssignedToId(), "COMPLAINT_ESCALATED",
                    title, message, e.getComplaintId(), "COMPLAINT");
            emailService.sendEscalatedAdminEmail(
                    e.getAssignedToId(), e.getTicketNumber(), e.getComplaintSubject(),
                    buildComplaintUrl(e.getAssignedToId(), e.getComplaintId()));
        }
        // GAP #3 — le reporter reçoit désormais un push en plus de l'email
        if (e.getReporterId() != null) {
            pushNotification(e.getReporterId(), "COMPLAINT_ESCALATED",
                    "Votre réclamation est escaladée — " + e.getTicketNumber(),
                    "Un superviseur va reprendre votre réclamation \"" +
                    e.getComplaintSubject() + "\".",
                    e.getComplaintId(), "COMPLAINT");
            emailService.sendEscalatedUserEmail(
                    e.getReporterId(), e.getTicketNumber(), e.getComplaintSubject(),
                    buildComplaintUrl(e.getReporterId(), e.getComplaintId()));
        }
    }

    // =========================================================================
    // NOUVEAUX HANDLERS (combler les gaps #1 & #6)
    // =========================================================================

    /** 9. Changement de priorité (GAP #6) */
    private void handlePriorityChanged(ComplaintNotificationEvent e) {
        String title   = "Priorité mise à jour — " + e.getTicketNumber();
        String message = "La priorité de la réclamation \"" + e.getComplaintSubject() +
                         "\" est désormais " + formatPriority(e.getNewPriority()) + ".";
        // Reporter
        if (e.getReporterId() != null) {
            pushNotification(e.getReporterId(), "COMPLAINT_PRIORITY",
                    title, message, e.getComplaintId(), "COMPLAINT");
        }
        // Agent assigné
        if (e.getAssignedToId() != null && !e.getAssignedToId().equals(e.getReporterId())) {
            pushNotification(e.getAssignedToId(), "COMPLAINT_PRIORITY",
                    title, message, e.getComplaintId(), "COMPLAINT");
        }
    }

    /** 10. Réouverture (GAP #1b) */
    private void handleReopened(ComplaintNotificationEvent e) {
        String reason  = e.getExtraContext();
        String title   = "Réclamation rouverte — " + e.getTicketNumber();
        String message = "Le plaignant a rouvert la réclamation \"" + e.getComplaintSubject() + "\"."
                + (reason != null && !reason.isBlank() ? " Motif : " + truncate(reason, 120) : "");
        // Agent assigné (prioritaire) — sinon admin via secondaryUserId
        String target = e.getAssignedToId() != null ? e.getAssignedToId() : e.getSecondaryUserId();
        if (target != null) {
            pushNotification(target, "COMPLAINT_REOPENED",
                    title, message, e.getComplaintId(), "COMPLAINT");
        }
        // Confirmation au reporter
        if (e.getReporterId() != null) {
            pushNotification(e.getReporterId(), "COMPLAINT_REOPENED",
                    "Réouverture confirmée — " + e.getTicketNumber(),
                    "Votre demande de réouverture de \"" + e.getComplaintSubject() + "\" a été enregistrée.",
                    e.getComplaintId(), "COMPLAINT");
        }
    }

    /** 11. Médiation ouverte (GAP #1a) */
    private void handleMediationOpened(ComplaintNotificationEvent e) {
        String title   = "Médiation ouverte — " + e.getTicketNumber();
        String message = "Une médiation a été ouverte pour la réclamation \"" +
                         e.getComplaintSubject() + "\". Vous pouvez soumettre vos preuves.";
        // Les deux parties + l'agent
        notifyParticipants(e, "MEDIATION_OPENED", title, message);
    }

    /** 12. Médiation — décision rendue (GAP #1a) */
    private void handleMediationDecided(ComplaintNotificationEvent e) {
        String outcome = e.getExtraContext();
        String title   = "Décision de médiation — " + e.getTicketNumber();
        String message = "La décision de médiation pour \"" + e.getComplaintSubject() + "\" a été rendue."
                + (outcome != null && !outcome.isBlank() ? " Issue : " + truncate(outcome, 100) : "");
        notifyParticipants(e, "MEDIATION_DECIDED", title, message);
    }

    /** 13. Sondage NPS disponible (GAP #1c) */
    private void handleNpsSurveyReady(ComplaintNotificationEvent e) {
        if (e.getReporterId() == null) return;
        pushNotification(e.getReporterId(), "NPS_SURVEY",
                "Notez votre expérience — " + e.getTicketNumber(),
                "Votre réclamation est clôturée. Partagez votre avis sur notre service.",
                e.getComplaintId(), "COMPLAINT");
    }

    /** 14. Dépassement SLA (GAP #1d) */
    private void handleSlaBreach(ComplaintNotificationEvent e) {
        String title   = "Dépassement SLA — " + e.getTicketNumber();
        String message = "La réclamation \"" + e.getComplaintSubject() +
                         "\" dépasse son délai contractuel.";
        if (e.getAssignedToId() != null) {
            pushNotification(e.getAssignedToId(), "SLA_BREACH",
                    title, message, e.getComplaintId(), "COMPLAINT");
        }
        // Admin ou superviseur (secondaryUserId)
        if (e.getSecondaryUserId() != null && !e.getSecondaryUserId().equals(e.getAssignedToId())) {
            pushNotification(e.getSecondaryUserId(), "SLA_BREACH",
                    title, message, e.getComplaintId(), "COMPLAINT");
        }
    }

    /** 15. Sanction appliquée (GAP #1e) */
    private void handleSanctionApplied(ComplaintNotificationEvent e) {
        if (e.getReportedUserId() == null) return;
        String sanctionType = e.getExtraContext();
        pushNotification(e.getReportedUserId(), "SANCTION_APPLIED",
                "Sanction appliquée",
                "Une sanction a été appliquée suite à la réclamation " + e.getTicketNumber() +
                (sanctionType != null && !sanctionType.isBlank() ? " (" + sanctionType + ")." : "."),
                e.getComplaintId(), "COMPLAINT");
    }

    /** Helper : notifie reporter + reported + agent assigné (médiation, etc.) */
    private void notifyParticipants(ComplaintNotificationEvent e, String type,
                                    String title, String message) {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String uid : new String[]{e.getReporterId(), e.getReportedUserId(), e.getAssignedToId()}) {
            if (uid != null && !uid.isBlank() && seen.add(uid)) {
                pushNotification(uid, type, title, message, e.getComplaintId(), "COMPLAINT");
            }
        }
    }

    // =========================================================================
    // MÉTHODES UTILITAIRES
    // =========================================================================

    /**
     * Envoie une notification push via Feign → notification-service.
     * Les erreurs sont loggées et jamais propagées (fire-and-forget).
     */
    private void pushNotification(String recipientId, String type, String title,
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
            log.debug("[Push] Envoyé à {} : {}", recipientId, title);
        } catch (Exception ex) {
            log.warn("[Push] Service notifications indisponible pour {} ({}) : {}",
                    recipientId, type, ex.getMessage());
        }
    }

    private String buildComplaintUrl(String userId, String complaintId) {
        return frontendBaseUrl + "/frontoffice/my-complaints/" + complaintId;
    }

    private String formatStatus(String status) {
        if (status == null) return "";
        return switch (status) {
            case "OPEN"         -> "Ouverte";
            case "IN_PROGRESS"  -> "En cours de traitement";
            case "PENDING_USER" -> "En attente de votre réponse";
            case "RESOLVED"     -> "Résolue";
            case "CLOSED"       -> "Clôturée";
            case "ESCALATED"    -> "Escaladée";
            default             -> status;
        };
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    private String formatPriority(String priority) {
        if (priority == null) return "";
        return switch (priority) {
            case "LOW"      -> "Basse";
            case "MEDIUM"   -> "Moyenne";
            case "HIGH"     -> "Haute";
            case "CRITICAL" -> "Critique";
            default         -> priority;
        };
    }
}
