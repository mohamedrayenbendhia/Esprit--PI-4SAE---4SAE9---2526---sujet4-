package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.dto.ComplaintEventDTO;
import com.smartfreelance.microservice.complaintservice.entity.ComplaintEvent;
import com.smartfreelance.microservice.complaintservice.notification.ComplaintNotificationEvent;
import com.smartfreelance.microservice.complaintservice.repository.ComplaintEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Service d'audit trail : persiste chaque événement métier d'une réclamation.
 * Appelé depuis ComplaintNotificationService après chaque action.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintEventService {

    private final ComplaintEventRepository eventRepository;

    // ── Labels français par eventType ───────────────────────────────────────────
    private static final Map<String, String> EVENT_LABELS = Map.ofEntries(
        Map.entry("COMPLAINT_CREATED",   "Réclamation créée"),
        Map.entry("STATUS_CHANGED",      "Statut modifié"),
        Map.entry("COMPLAINT_RESOLVED",  "Réclamation résolue"),
        Map.entry("COMPLAINT_CLOSED",    "Réclamation clôturée"),
        Map.entry("COMPLAINT_REOPENED",  "Réclamation rouverte"),
        Map.entry("COMPLAINT_ASSIGNED",  "Réclamation assignée"),
        Map.entry("COMPLAINT_ESCALATED", "Réclamation escaladée"),
        Map.entry("PRIORITY_CHANGED",    "Priorité modifiée"),
        Map.entry("REPORTED_INVOLVED",   "Partie mise en cause impliquée"),
        Map.entry("NEW_MESSAGE",         "Nouveau message"),
        Map.entry("MEDIATION_OPENED",    "Médiation ouverte"),
        Map.entry("MEDIATION_DECIDED",   "Décision de médiation"),
        Map.entry("NPS_SURVEY_READY",    "Sondage NPS envoyé"),
        Map.entry("SLA_BREACH",          "Dépassement SLA"),
        Map.entry("SANCTION_APPLIED",    "Sanction appliquée")
    );

    // ── Icônes Material par eventType ───────────────────────────────────────────
    private static final Map<String, String> EVENT_ICONS = Map.ofEntries(
        Map.entry("COMPLAINT_CREATED",   "add_circle_outline"),
        Map.entry("STATUS_CHANGED",      "swap_horiz"),
        Map.entry("COMPLAINT_RESOLVED",  "check_circle"),
        Map.entry("COMPLAINT_CLOSED",    "lock"),
        Map.entry("COMPLAINT_REOPENED",  "lock_open"),
        Map.entry("COMPLAINT_ASSIGNED",  "person_add"),
        Map.entry("COMPLAINT_ESCALATED", "trending_up"),
        Map.entry("PRIORITY_CHANGED",    "flag"),
        Map.entry("REPORTED_INVOLVED",   "group_add"),
        Map.entry("NEW_MESSAGE",         "chat_bubble"),
        Map.entry("MEDIATION_OPENED",    "gavel"),
        Map.entry("MEDIATION_DECIDED",   "verified"),
        Map.entry("NPS_SURVEY_READY",    "star_rate"),
        Map.entry("SLA_BREACH",          "timer_off"),
        Map.entry("SANCTION_APPLIED",    "block")
    );

    // ── API publique ──────────────────────────────────────────────────────────────

    /**
     * Persiste un événement à partir d'un ComplaintNotificationEvent.
     * Appelé de façon synchrone depuis ComplaintNotificationService.handle()
     * (avant la notification push, qui est @Async).
     */
    @Transactional
    public void record(ComplaintNotificationEvent e) {
        record(e, null, null);
    }

    @Transactional
    public void record(ComplaintNotificationEvent e, String oldValue, String newValue) {
        String eventType = e.getEventType().name();
        String comment   = buildComment(e);

        ComplaintEvent event = ComplaintEvent.builder()
                .complaintId(e.getComplaintId())
                .ticketNumber(e.getTicketNumber())
                .actorId(resolveActor(e))
                .actorRole(resolveActorRole(e))
                .eventType(eventType)
                .oldValue(oldValue != null ? oldValue : resolveOldValue(e))
                .newValue(newValue != null ? newValue : resolveNewValue(e))
                .comment(comment)
                .build();

        eventRepository.save(event);
        log.debug("[AuditTrail] {} enregistré pour réclamation {}", eventType, e.getComplaintId());
    }

    /** Retourne la timeline d'une réclamation, du plus ancien au plus récent. */
    @Transactional(readOnly = true)
    public List<ComplaintEventDTO> getTimeline(String complaintId) {
        return eventRepository
                .findByComplaintIdOrderByOccurredAtAsc(complaintId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private ComplaintEventDTO toDTO(ComplaintEvent e) {
        return ComplaintEventDTO.builder()
                .id(e.getId())
                .complaintId(e.getComplaintId())
                .ticketNumber(e.getTicketNumber())
                .actorId(e.getActorId())
                .actorRole(e.getActorRole())
                .eventType(e.getEventType())
                .oldValue(e.getOldValue())
                .newValue(e.getNewValue())
                .comment(e.getComment())
                .occurredAt(e.getOccurredAt())
                .eventLabel(EVENT_LABELS.getOrDefault(e.getEventType(), e.getEventType()))
                .icon(EVENT_ICONS.getOrDefault(e.getEventType(), "info_outline"))
                .build();
    }

    private String resolveActor(ComplaintNotificationEvent e) {
        // Pour les événements déclenchés par un agent/admin, l'acteur est assignedToId
        // Pour la création, c'est le reporter
        return switch (e.getEventType()) {
            case COMPLAINT_CREATED   -> e.getReporterId();
            case COMPLAINT_REOPENED  -> e.getReporterId();
            case COMPLAINT_RESOLVED,
                 STATUS_CHANGED,
                 PRIORITY_CHANGED,
                 COMPLAINT_ASSIGNED,
                 COMPLAINT_ESCALATED,
                 COMPLAINT_CLOSED,
                 REPORTED_INVOLVED   -> e.getAssignedToId() != null ? e.getAssignedToId() : "system";
            default                  -> "system";
        };
    }

    private String resolveActorRole(ComplaintNotificationEvent e) {
        return switch (e.getEventType()) {
            case COMPLAINT_CREATED, COMPLAINT_REOPENED -> "USER";
            case COMPLAINT_CLOSED                      -> "ADMIN";
            default                                    -> e.getAssignedToId() != null ? "SUPPORT_AGENT" : "SYSTEM";
        };
    }

    private String resolveOldValue(ComplaintNotificationEvent e) {
        if (e.getOldStatus()   != null) return e.getOldStatus();
        if (e.getOldPriority() != null) return e.getOldPriority();
        return null;
    }

    private String resolveNewValue(ComplaintNotificationEvent e) {
        if (e.getNewStatus()   != null) return e.getNewStatus();
        if (e.getNewPriority() != null) return e.getNewPriority();
        if (e.getAssignedToId() != null
                && e.getEventType() == ComplaintNotificationEvent.EventType.COMPLAINT_ASSIGNED) {
            return e.getAssignedToId();
        }
        return null;
    }

    private String buildComment(ComplaintNotificationEvent e) {
        return switch (e.getEventType()) {
            case COMPLAINT_REOPENED  -> e.getExtraContext();
            case COMPLAINT_RESOLVED  -> e.getExtraContext();
            case MEDIATION_DECIDED   -> e.getExtraContext();
            case SANCTION_APPLIED    -> e.getExtraContext();
            case REPORTED_INVOLVED   -> e.getInvitationMessage();
            default                  -> null;
        };
    }
}
