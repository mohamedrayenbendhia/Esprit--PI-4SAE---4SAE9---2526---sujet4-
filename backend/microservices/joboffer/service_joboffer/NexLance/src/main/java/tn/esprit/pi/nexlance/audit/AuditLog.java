package tn.esprit.pi.nexlance.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String action; // CREATE, UPDATE, DELETE, STATUS_CHANGE, etc.

    @Column(nullable = false)
    private String entityType; // JOB_OFFER, APPLICATION, RECOMMENDATION, INVITATION

    @Column(nullable = false)
    private String entityId;

    private String userId; // User who performed the action

    private String userRole; // CLIENT, FREELANCER, ADMIN

    @Column(length = 2000)
    private String details; // JSON details of the change

    private String oldValue;

    private String newValue;

    private String ipAddress;

    @CreationTimestamp
    private LocalDateTime timestamp;
}
