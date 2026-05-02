package com.smartfreelance.microservice.complaintservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Template de réponse pré-rédigé pour les agents de support.
 * Filtrable par catégorie de réclamation.
 */
@Entity
@Table(name = "response_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResponseTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Catégorie associée (null = template générique applicable à toutes).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50)
    private Complaint.ComplaintCategory category;

    @Column(name = "created_by_admin_id", length = 36)
    private String createdByAdminId;

    /** Nombre d'utilisations — incrémenté à chaque usage par un agent. */
    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private int usageCount = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

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
