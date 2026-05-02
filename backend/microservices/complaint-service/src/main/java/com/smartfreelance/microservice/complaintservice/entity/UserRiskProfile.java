package com.smartfreelance.microservice.complaintservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Profil de risque calculé pour chaque utilisateur.
 * Score 0-100 (100 = risque maximal).
 *
 * Facteurs de calcul :
 *  - Nb réclamations reçues (pondéré par catégorie)
 *  - Nb résolutions défavorables
 *  - Nb SCAM / HARASSMENT
 *  - Récence des incidents
 */
@Entity
@Table(name = "user_risk_profiles",
        uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserRiskProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    /** Score de risque global (0 = aucun risque, 100 = risque maximum). */
    @Column(name = "risk_score", nullable = false)
    @Builder.Default
    private int riskScore = 0;

    @Column(name = "total_complaints_against", nullable = false)
    @Builder.Default
    private int totalComplaintsAgainst = 0;

    @Column(name = "resolved_against", nullable = false)
    @Builder.Default
    private int resolvedAgainst = 0;

    @Column(name = "scam_count", nullable = false)
    @Builder.Default
    private int scamCount = 0;

    @Column(name = "harassment_count", nullable = false)
    @Builder.Default
    private int harassmentCount = 0;

    @Column(name = "payment_issue_count", nullable = false)
    @Builder.Default
    private int paymentIssueCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    @Builder.Default
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Column(name = "last_calculated_at")
    private LocalDateTime lastCalculatedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum RiskLevel {
        LOW,      // score 0-24
        MODERATE, // score 25-49
        HIGH,     // score 50-74
        CRITICAL  // score 75-100
    }
}
