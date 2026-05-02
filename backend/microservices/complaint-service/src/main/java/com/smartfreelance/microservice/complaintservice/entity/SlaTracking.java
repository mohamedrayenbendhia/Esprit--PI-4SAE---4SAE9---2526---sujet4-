package com.smartfreelance.microservice.complaintservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Suivi SLA en temps réel pour chaque réclamation.
 * Créé automatiquement à l'assignation de la réclamation.
 */
@Entity
@Table(name = "sla_tracking",
        uniqueConstraints = @UniqueConstraint(columnNames = "complaint_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SlaTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name = "complaint_id", nullable = false, length = 36)
    private String complaintId;

    @Column(name = "first_response_deadline")
    private LocalDateTime firstResponseDeadline;

    @Column(name = "resolution_deadline")
    private LocalDateTime resolutionDeadline;

    @Column(name = "first_response_breached", nullable = false)
    @Builder.Default
    private boolean firstResponseBreached = false;

    @Column(name = "resolution_breached", nullable = false)
    @Builder.Default
    private boolean resolutionBreached = false;

    /** Timestamp réel de la première réponse (null = pas encore répondu). */
    @Column(name = "first_response_at")
    private LocalDateTime firstResponseAt;

    /** Timestamp réel de la résolution (null = pas encore résolu). */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
