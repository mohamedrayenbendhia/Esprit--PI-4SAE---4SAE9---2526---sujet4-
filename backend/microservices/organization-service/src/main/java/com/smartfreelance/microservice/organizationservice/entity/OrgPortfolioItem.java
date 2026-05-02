package com.smartfreelance.microservice.organizationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "org_portfolio_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrgPortfolioItem {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "organization_id", nullable = false, length = 36)
    private String organizationId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "project_url", length = 512)
    private String projectUrl;

    @Column(name = "client_name", length = 200)
    private String clientName;

    @Convert(converter = JsonListConverter.class)
    @Column(columnDefinition = "JSON")
    @Builder.Default
    private List<String> tags = new java.util.ArrayList<>();

    @Column(name = "completed_at")
    private LocalDate completedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }
}
