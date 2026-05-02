package com.smartfreelance.microservice.complaintservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Preuve soumise par une partie dans le cadre d'une médiation.
 */
@Entity
@Table(name = "mediation_evidences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MediationEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Column(name = "submitted_by_user_id", nullable = false, length = 36)
    private String submittedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "party_type", nullable = false, length = 20)
    private PartyType partyType;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Convert(converter = JsonListConverter.class)
    @Column(name = "attachments", columnDefinition = "LONGTEXT")
    private List<String> attachments;

    @Column(name = "submitted_at", updatable = false)
    private LocalDateTime submittedAt;

    @PrePersist
    protected void onCreate() { submittedAt = LocalDateTime.now(); }

    public enum PartyType {
        COMPLAINANT, REPORTED
    }
}
