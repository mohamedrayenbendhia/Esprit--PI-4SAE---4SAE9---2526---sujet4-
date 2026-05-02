package tn.esprit.pi.nexlance.invitation.entities;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.pi.nexlance.invitation.enums.InvitationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invitation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String clientId;
    
    @Column(nullable = false)
    private String freelanceId;
    
    @Column(nullable = false)
    private String jobOfferId;
    
    @Column(length = 2000, nullable = false)
    private String message;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal proposedBudget;
    
    private LocalDateTime deadlineResponse;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InvitationStatus status = InvitationStatus.PENDING;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    private LocalDateTime respondedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
