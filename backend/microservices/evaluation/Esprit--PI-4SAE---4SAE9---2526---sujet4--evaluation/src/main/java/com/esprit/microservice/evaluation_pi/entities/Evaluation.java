package com.esprit.microservice.evaluation_pi.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Evaluation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String projectId;
    private String evaluatorId;
    private String evaluatedId;

    private Double ratingGlobal;
    private Double qualityScore;
    private Double deadlineScore;
    private Double communicationScore;
    private Double professionalismScore;

    private String comment;
    private String responseText;
    private LocalDateTime responseDate;

    private Integer helpfulCount;
    private Integer notHelpfulCount;

    @Enumerated(EnumType.STRING)
    private EvaluationStatus status;

    @Enumerated(EnumType.STRING)
    private ReportReason reportReason;

    @Enumerated(EnumType.STRING)
    private ReportStatus reportStatus;

    private Boolean lockedAfter48h;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = EvaluationStatus.PUBLISHED;
        if (helpfulCount == null) helpfulCount = 0;
        if (notHelpfulCount == null) notHelpfulCount = 0;
        if (lockedAfter48h == null) lockedAfter48h = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}