package tn.esprit.pi.service_projects.module_project_Milestone.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "job_offer_id")
    private UUID jobOfferId;

    @Column(name = "title")
    private String title;

    @Column(name = "freelance_id")
    private UUID freelanceId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ProjectStatus status;

    @Column(name = "progress")
    private Integer progress; // 0-100

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<ProjectMilestone> milestones = new ArrayList<>();

    @Column(name = "requirements", columnDefinition = "TEXT")
    private String requirements;

    @Column(name = "deliverables", columnDefinition = "TEXT")
    private String deliverables; // JSON Array stored as String

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (progress == null) {
            progress = 0;
        }
        if (status == null) {
            status = ProjectStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper method to calculate progress based on approved milestones
    public void calculateProgress() {
        if (milestones == null || milestones.isEmpty()) {
            this.progress = 0;
            return;
        }
        
        long approvedCount = milestones.stream()
                .filter(m -> m.getStatus() == MilestoneStatus.APPROVED)
                .count();
        
        this.progress = (int) ((approvedCount * 100) / milestones.size());
    }
}
