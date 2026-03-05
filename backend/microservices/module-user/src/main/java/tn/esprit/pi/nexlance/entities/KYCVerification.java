package tn.esprit.pi.nexlance.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "kyc_verifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KYCVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @ManyToOne
    @JoinColumn(name = "userId", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    @Column(nullable = false)
    private String documentUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status = VerificationStatus.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    private UUID reviewedBy;

    @ManyToOne
    @JoinColumn(name = "reviewedBy", referencedColumnName = "id", insertable = false, updatable = false)
    private User reviewer;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    private LocalDateTime expiryDate;

    public enum DocumentType {
        IDENTITY_CARD,
        PASSPORT,
        DRIVER_LICENSE,
        PROOF_ADDRESS,
        BANK_STATEMENT
    }

    public enum VerificationStatus {
        PENDING,
        APPROVED,
        REJECTED,
        EXPIRED
    }
}
