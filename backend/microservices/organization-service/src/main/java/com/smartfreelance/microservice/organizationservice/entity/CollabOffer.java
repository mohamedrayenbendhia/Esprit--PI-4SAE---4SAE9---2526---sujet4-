package com.smartfreelance.microservice.organizationservice.entity;

import com.smartfreelance.microservice.organizationservice.enums.CollabOfferStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Offre de collaboration ponctuelle publiée par une organisation.
 * Elle cible des freelances extérieurs pour une mission interne limitée dans le temps.
 */
@Entity
@Table(name = "collab_offers", indexes = {
        @Index(name = "idx_co_org",    columnList = "organization_id"),
        @Index(name = "idx_co_status", columnList = "status"),
        @Index(name = "idx_co_owner",  columnList = "created_by")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CollabOffer {

    @Id
    @Column(length = 36)
    private String id;

    /** Organisation qui publie l'offre */
    @Column(name = "organization_id", nullable = false, length = 36)
    private String organizationId;

    /** Membre (OWNER ou MANAGER) qui a créé l'offre */
    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /** Compétences requises, stockées en JSON */
    @Convert(converter = JsonListConverter.class)
    @Column(name = "required_skills", columnDefinition = "TEXT")
    private java.util.List<String> requiredSkills;

    /** Durée indicative (ex: "3 semaines", "2 mois") */
    @Column(name = "duration_label", length = 100)
    private String durationLabel;

    /** Budget estimé (optionnel, en euros) */
    @Column(name = "budget_estimate")
    private Double budgetEstimate;

    /** Nombre maximum de collaborateurs acceptés (null = illimité) */
    @Column(name = "max_applicants")
    private Integer maxApplicants;

    /** Date limite de candidature */
    @Column(name = "deadline_date")
    private LocalDate deadlineDate;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CollabOfferStatus status = CollabOfferStatus.OPEN;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }
}
