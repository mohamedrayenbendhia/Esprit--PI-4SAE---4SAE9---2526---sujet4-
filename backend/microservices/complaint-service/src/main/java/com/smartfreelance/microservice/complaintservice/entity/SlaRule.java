package com.smartfreelance.microservice.complaintservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Règle SLA configurable par priorité.
 * Exemple : CRITICAL → 2h première réponse, 24h résolution.
 */
@Entity
@Table(name = "sla_rules",
        uniqueConstraints = @UniqueConstraint(columnNames = "priority"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SlaRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Complaint.Priority priority;

    /** Délai max pour la première réponse de l'agent (en heures). */
    @Column(name = "max_first_response_hours", nullable = false)
    private int maxFirstResponseHours;

    /** Délai max pour la résolution complète (en heures). */
    @Column(name = "max_resolution_hours", nullable = false)
    private int maxResolutionHours;

    /** Heures avant la deadline pour déclencher une alerte préventive. */
    @Column(name = "warning_threshold_hours", nullable = false)
    private int warningThresholdHours;

    /**
     * Si true, la réclamation est automatiquement escaladée vers l'admin
     * (status → ESCALATED, assignedToId → null) quand le délai de résolution est dépassé.
     */
    @Column(name = "auto_escalate", nullable = false)
    @Builder.Default
    private boolean autoEscalate = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt  = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
