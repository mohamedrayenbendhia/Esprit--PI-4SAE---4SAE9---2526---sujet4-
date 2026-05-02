package com.smartfreelance.microservice.complaintservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Enquête NPS (Net Promoter Score) envoyée 3 jours après la clôture
 * d'une réclamation.
 *
 * Score 0-10 :
 *   0-6  → Détracteur
 *   7-8  → Passif
 *   9-10 → Promoteur
 */
@Entity
@Table(name = "nps_surveys",
        uniqueConstraints = @UniqueConstraint(columnNames = "complaint_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NpsSurvey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name = "complaint_id", nullable = false, length = 36)
    private String complaintId;

    @Column(name = "respondent_id", nullable = false, length = 36)
    private String respondentId;

    /** Score NPS 0-10 (null = pas encore répondu). */
    @Column(name = "score")
    private Integer score;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    /** Date d'envoi de l'email NPS. */
    @Column(name = "sent_at", updatable = false)
    private LocalDateTime sentAt;

    /** Date de la réponse de l'utilisateur (null = pas encore répondu). */
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20)
    private NpsCategory category;

    @PrePersist
    protected void onCreate() { sentAt = LocalDateTime.now(); }

    public enum NpsCategory {
        DETRACTOR,  // 0-6
        PASSIVE,    // 7-8
        PROMOTER    // 9-10
    }

    /** Calcule la catégorie depuis le score. */
    public static NpsCategory computeCategory(int score) {
        if (score <= 6) return NpsCategory.DETRACTOR;
        if (score <= 8) return NpsCategory.PASSIVE;
        return NpsCategory.PROMOTER;
    }
}
