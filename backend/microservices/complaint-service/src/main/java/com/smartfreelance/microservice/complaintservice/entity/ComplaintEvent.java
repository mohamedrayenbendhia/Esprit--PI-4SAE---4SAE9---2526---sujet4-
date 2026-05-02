package com.smartfreelance.microservice.complaintservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Audit trail immuable de toutes les transitions d'une réclamation.
 * Chaque changement d'état, de priorité, d'assignation, de résolution,
 * de clôture ou de réouverture produit un enregistrement ici.
 *
 * L'entité est intentionnellement append-only (pas de update/delete).
 */
@Entity
@Table(name = "complaint_events", indexes = {
    @Index(name = "idx_complaint_events_complaint_id", columnList = "complaint_id"),
    @Index(name = "idx_complaint_events_occurred_at", columnList = "occurred_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    /** Réclamation concernée */
    @Column(name = "complaint_id", nullable = false, length = 36)
    private String complaintId;

    /** Numéro de ticket (dénormalisé pour éviter une jointure) */
    @Column(name = "ticket_number", length = 50)
    private String ticketNumber;

    /** Utilisateur qui a déclenché l'action (null = système/scheduler) */
    @Column(name = "actor_id", length = 36)
    private String actorId;

    /** Rôle de l'acteur au moment de l'action */
    @Column(name = "actor_role", length = 30)
    private String actorRole;

    /**
     * Type d'événement.
     * Correspond aux EventType de ComplaintNotificationEvent.
     */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /** Valeur avant le changement (statut, priorité, agentId...) */
    @Column(name = "old_value", length = 255)
    private String oldValue;

    /** Valeur après le changement */
    @Column(name = "new_value", length = 255)
    private String newValue;

    /** Contexte libre : texte de résolution, raison de réouverture, note de sanction... */
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    protected void onCreate() {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}
