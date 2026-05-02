package tn.esprit.pi.service_projects.module_project_Milestone.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deliverable_versions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeliverableVersion {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID milestoneId;

    @Column(nullable = false)
    private int versionNumber;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String filePath;

    private Long fileSize;

    private String contentType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String changeNotes;

    private UUID uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VersionStatus status;

    @Column(columnDefinition = "TEXT")
    private String reviewComment;

    private UUID reviewedBy;

    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = VersionStatus.PENDING;
    }

    public enum VersionStatus {
        PENDING,
        APPROVED,
        REJECTED,
        SUPERSEDED
    }
}
