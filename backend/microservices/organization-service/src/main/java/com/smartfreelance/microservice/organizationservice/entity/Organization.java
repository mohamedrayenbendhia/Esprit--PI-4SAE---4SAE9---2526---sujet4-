package com.smartfreelance.microservice.organizationservice.entity;

import com.smartfreelance.microservice.organizationservice.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "organizations", indexes = {
        @Index(name = "idx_org_owner",      columnList = "owner_id"),
        @Index(name = "idx_org_status",     columnList = "status"),
        @Index(name = "idx_org_type",       columnList = "type"),
        @Index(name = "idx_org_visibility", columnList = "visibility")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Organization {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String logoUrl;

    @Column(length = 255)
    private String website;

    @Column(name = "type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private OrganizationType type;

    @Convert(converter = JsonListConverter.class)
    @Column(name = "specialties", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> specialties = new ArrayList<>();

    @Column(length = 150)
    private String location;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(length = 14)
    private String siret;

    @Column(name = "size", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrganizationSize size = OrganizationSize.SMALL;

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrganizationStatus status = OrganizationStatus.PENDING_VERIFICATION;

    @Column(name = "visibility", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrganizationVisibility visibility = OrganizationVisibility.PUBLIC;

    @Column(name = "owner_id", nullable = false, length = 36)
    private String ownerId;

    @Column(name = "average_rating")
    @Builder.Default
    private Double averageRating = 0.0;

    @Column(name = "completed_projects_count")
    @Builder.Default
    private Integer completedProjectsCount = 0;

    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "trust_level")
    @Builder.Default
    private Integer trustLevel = 1;

    @Column(name = "badges", columnDefinition = "JSON")
    private String badges;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "dissolved_at")
    private LocalDateTime dissolvedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }
}
