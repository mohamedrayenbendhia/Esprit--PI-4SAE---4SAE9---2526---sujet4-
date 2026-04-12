package tn.esprit.pi.nexlance.notification;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String recipientId;

    @Column(nullable = false)
    private String type; // JOB_OFFER, APPLICATION, RECOMMENDATION, INVITATION, PROJECT, MILESTONE

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String message;

    private String referenceId; // ID of the related entity (job offer, application, etc.)

    private String referenceType; // Entity type for navigation

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime readAt;
}
