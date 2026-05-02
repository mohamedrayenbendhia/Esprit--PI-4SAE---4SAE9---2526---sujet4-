package com.smartfreelance.microservice.complaintservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "complaints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "ticket_number", unique = true, nullable = false, length = 50)
    private String ticketNumber;

    @Column(name = "reporter_id", nullable = false, length = 36)
    private String reporterId;

    @Column(name = "reported_user_id", length = 36)
    private String reportedUserId;

    @Column(name = "project_id", length = 36)
    private String projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ComplaintCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.OPEN;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Convert(converter = JsonListConverter.class)
    @Column(name = "attachments", columnDefinition = "LONGTEXT")
    private List<String> attachments;

    @Column(name = "assigned_to_id", length = 36)
    private String assignedToId;

    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_type")
    private ResolutionType resolutionType;

    @Column(name = "satisfaction_rating")
    private Integer satisfactionRating;

    // ── Réouverture contrôlée ─────────────────────────────────────────────
    @Column(name = "reopen_count", nullable = false)
    @Builder.Default
    private int reopenCount = 0;

    @Column(name = "last_reopened_at")
    private LocalDateTime lastReopenedAt;

    @Column(name = "reopen_reason", columnDefinition = "TEXT")
    private String reopenReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "first_response_at")
    private LocalDateTime firstResponseAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // ticketNumber est généré dans ComplaintServiceImpl.generateUniqueTicketNumber()
        // avec ThreadLocalRandom + retry anti-collision avant l'appel à save().
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ComplaintCategory {
        PAYMENT_ISSUE,
        QUALITY_DISPUTE,
        COMMUNICATION_PROBLEM,
        HARASSMENT,
        SCAM,
        TECHNICAL_ISSUE,
        OTHER
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum Status {
        OPEN,
        IN_PROGRESS,
        PENDING_USER,
        RESOLVED,
        CLOSED,
        ESCALATED
    }

    public enum ResolutionType {
        REFUND,
        WARNING,
        ACCOUNT_SUSPENSION,
        NO_ACTION,
        MEDIATION
    }
}