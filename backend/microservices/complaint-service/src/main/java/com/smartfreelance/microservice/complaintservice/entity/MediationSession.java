package com.smartfreelance.microservice.complaintservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Session de médiation structurée liée à une réclamation.
 *
 * Workflow :
 *   OPEN → EVIDENCE_PHASE (les deux parties soumettent des preuves)
 *        → DELIBERATION   (l'admin examine)
 *        → CLOSED         (décision rendue)
 */
@Entity
@Table(name = "mediation_sessions",
        uniqueConstraints = @UniqueConstraint(columnNames = "complaint_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MediationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name = "complaint_id", nullable = false, length = 36)
    private String complaintId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private MediationStatus status = MediationStatus.OPEN;

    /** Date limite pour soumission des preuves par les deux parties. */
    @Column(name = "evidence_deadline")
    private LocalDateTime evidenceDeadline;

    /** Date limite pour la décision de l'admin. */
    @Column(name = "decision_deadline")
    private LocalDateTime decisionDeadline;

    /** ID de l'admin qui a ouvert la session. */
    @Column(name = "opened_by_admin_id", length = 36)
    private String openedByAdminId;

    /** ID de l'admin qui a rendu la décision finale. */
    @Column(name = "decided_by_admin_id", length = 36)
    private String decidedByAdminId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", length = 30)
    private MediationOutcome outcome;

    @Column(name = "admin_reasoning", columnDefinition = "TEXT")
    private String adminReasoning;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    // ── Enums ────────────────────────────────────────────────────────────

    public enum MediationStatus {
        OPEN, EVIDENCE_PHASE, DELIBERATION, CLOSED
    }

    public enum MediationOutcome {
        FAVOR_COMPLAINANT,
        FAVOR_REPORTED,
        MUTUAL_AGREEMENT,
        NO_FAULT_FOUND
    }
}
