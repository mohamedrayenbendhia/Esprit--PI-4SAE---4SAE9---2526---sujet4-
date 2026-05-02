package tn.esprit.pi.nexlance.recommendation.entities;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.pi.nexlance.recommendation.enums.RecommendationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recommendations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recommendation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String clientId;
    
    @Transient
    private String clientName;
    
    @Column(nullable = false)
    private String freelanceId;
    
    @Column(nullable = false)
    private String jobOfferId;
    
    @Column(length = 1000)
    private String message;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal proposedBudget;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RecommendationStatus status = RecommendationStatus.PENDING;
    
    private LocalDateTime sentDate;
    
    private LocalDateTime responseDate;
    
    private LocalDateTime expirationDate;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer viewCount = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isReminderSent = false;
    
    private LocalDateTime reminderSentDate;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String metadata;
    private String cancellationReason;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        sentDate = LocalDateTime.now();
        if (metadata == null) {
            metadata = "{}";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Helper methods
    public void incrementViews() {
        this.viewCount++;
    }
    
    public boolean canBeAccepted() {
        return status == RecommendationStatus.PENDING && 
               (expirationDate == null || LocalDateTime.now().isBefore(expirationDate));
    }
    
    public boolean isExpired() {
        return expirationDate != null && LocalDateTime.now().isAfter(expirationDate);
    }
    
    public void accept() {
        if (!canBeAccepted()) {
            throw new IllegalStateException("Recommendation cannot be accepted in current state");
        }
        this.status = RecommendationStatus.ACCEPTED;
        this.responseDate = LocalDateTime.now();
    }
    
    public void reject(String reason) {
        if (status != RecommendationStatus.PENDING) {
            throw new IllegalStateException("Can only reject pending recommendations");
        }
        this.status = RecommendationStatus.REJECTED;
        this.responseDate = LocalDateTime.now();
        this.cancellationReason = reason;
    }
    
    public void cancel(String reason) {
        if (status != RecommendationStatus.PENDING) {
            throw new IllegalStateException("Can only cancel pending recommendations");
        }
        this.status = RecommendationStatus.CANCELLED;
        this.cancellationReason = reason;
    }
}
