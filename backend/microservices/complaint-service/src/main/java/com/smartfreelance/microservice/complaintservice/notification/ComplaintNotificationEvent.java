package com.smartfreelance.microservice.complaintservice.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Événement interne décrivant une notification à envoyer.
 * Produit par les services métier, consommé par ComplaintNotificationService.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintNotificationEvent {

    public enum EventType {
        COMPLAINT_CREATED,       // Accusé de réception au plaignant
        STATUS_CHANGED,          // Changement de statut (tous les participants)
        COMPLAINT_RESOLVED,      // Réclamation résolue
        COMPLAINT_CLOSED,        // Réclamation clôturée
        NEW_MESSAGE,             // Nouveau message dans une conversation
        REPORTED_INVOLVED,       // La partie mise en cause est impliquée
        COMPLAINT_ASSIGNED,      // Réclamation assignée à un agent
        COMPLAINT_ESCALATED,     // Réclamation escaladée
        PRIORITY_CHANGED,        // Changement de priorité (reporter + agent)
        COMPLAINT_REOPENED,      // Réclamation rouverte par le plaignant
        MEDIATION_OPENED,        // Session de médiation ouverte
        MEDIATION_DECIDED,       // Décision de médiation rendue
        NPS_SURVEY_READY,        // Sondage NPS disponible pour le plaignant
        SLA_BREACH,              // Dépassement du SLA détecté
        SANCTION_APPLIED         // Sanction appliquée à un utilisateur
    }

    private EventType eventType;

    // Identifiants de la réclamation
    private String complaintId;
    private String ticketNumber;
    private String complaintSubject;

    // Participants
    private String reporterId;       // Plaignant
    private String reportedUserId;   // Partie mise en cause (peut être null)
    private String assignedToId;     // Agent assigné (peut être null)

    // Destinataire réel calculé par le controller — évite le recalcul dans le handler
    private String recipientId;

    // true si la réclamation n'est pas encore assignée (message sans destinataire agent)
    @Builder.Default
    private boolean unassigned = false;

    // GAP #5 — true quand un agent s'auto-assigne (/take). Évite de lui renvoyer
    // une notification "Réclamation assignée" puisqu'il vient d'effectuer l'action.
    @Builder.Default
    private boolean selfAssignment = false;

    // Pour STATUS_CHANGED
    private String oldStatus;
    private String newStatus;

    // Pour PRIORITY_CHANGED
    private String oldPriority;
    private String newPriority;

    // Pour MEDIATION_*  / REOPENED / NPS / SLA / SANCTION
    private String extraContext;      // texte libre à inclure dans le message (raison, type de sanction, etc.)
    private String secondaryUserId;   // 2e destinataire (agent assigné par ex.)

    // Pour NEW_MESSAGE
    private String senderName;
    private String messageExcerpt;   // 150 premiers caractères
    private String conversationType; // COMPLAINANT | REPORTED

    // Pour REPORTED_INVOLVED
    private String invitationMessage;

    // Métadonnées
    private String complaintUrl;     // Lien vers la réclamation côté frontend
}