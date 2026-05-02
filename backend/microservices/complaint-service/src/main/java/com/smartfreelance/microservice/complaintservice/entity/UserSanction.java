package com.smartfreelance.microservice.complaintservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Sanction appliquée à un utilisateur à risque.
 *
 * Graduation automatique :
 *   1ère fois  → WARNING
 *   2ème fois  → TEMP_SUSPENSION (7 jours)
 *   3ème fois  → PERMANENT_SUSPENSION
 */
@Entity
@Table(name = "user_sanctions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserSanction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private SanctionType type;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    /** ID de la réclamation déclencheuse (peut être null si sanction manuelle). */
    @Column(name = "trigger_complaint_id", length = 36)
    private String triggerComplaintId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /** null = sanction permanente ou avertissement sans expiration. */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "applied_at", updatable = false)
    private LocalDateTime appliedAt;

    /** true = déclenchée par le moteur automatique. false = manuelle (admin). */
    @Column(name = "applied_by_system", nullable = false)
    @Builder.Default
    private boolean appliedBySystem = false;

    @Column(name = "applied_by_admin_id", length = 36)
    private String appliedByAdminId;

    @Column(name = "lifted_at")
    private LocalDateTime liftedAt;

    @Column(name = "lifted_by_admin_id", length = 36)
    private String liftedByAdminId;

    @PrePersist
    protected void onCreate() { appliedAt = LocalDateTime.now(); }

    public enum SanctionType {
        WARNING,
        TEMP_SUSPENSION,
        PERMANENT_SUSPENSION
    }
}
