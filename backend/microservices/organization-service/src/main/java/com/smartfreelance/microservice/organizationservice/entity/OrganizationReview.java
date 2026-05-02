package com.smartfreelance.microservice.organizationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "organization_reviews", indexes = {
        @Index(name = "idx_review_org",      columnList = "organization_id"),
        @Index(name = "idx_review_reviewer", columnList = "reviewer_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrganizationReview {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "organization_id", nullable = false, length = 36)
    private String organizationId;

    @Column(name = "reviewer_id", nullable = false, length = 36)
    private String reviewerId;

    @Column(name = "project_id", length = 36)
    private String projectId;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "reply", columnDefinition = "TEXT")
    private String reply;

    @Column(name = "reply_at")
    private LocalDateTime replyAt;

    @Column(name = "reported", nullable = false)
    @Builder.Default
    private boolean reported = false;

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
