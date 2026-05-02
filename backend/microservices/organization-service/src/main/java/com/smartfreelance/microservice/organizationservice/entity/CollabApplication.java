package com.smartfreelance.microservice.organizationservice.entity;

import com.smartfreelance.microservice.organizationservice.enums.CollabApplicationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Candidature d'un freelance à une offre de collaboration ponctuelle.
 */
@Entity
@Table(name = "collab_applications", indexes = {
        @Index(name = "idx_ca_offer",     columnList = "offer_id"),
        @Index(name = "idx_ca_applicant", columnList = "applicant_id"),
        @Index(name = "idx_ca_status",    columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CollabApplication {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "offer_id", nullable = false, length = 36)
    private String offerId;

    /** Organisation concernée (dénormalisée pour les requêtes rapides) */
    @Column(name = "organization_id", nullable = false, length = 36)
    private String organizationId;

    @Column(name = "applicant_id", nullable = false, length = 36)
    private String applicantId;

    /** Lettre de motivation / message du candidat */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /** Lien vers portfolio ou profil externe (optionnel) */
    @Column(name = "portfolio_url", length = 512)
    private String portfolioUrl;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CollabApplicationStatus status = CollabApplicationStatus.PENDING;

    /** Raison du refus (optionnel) */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }
}
