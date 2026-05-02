package tn.esprit.pi.nexlance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.pi.nexlance.entities.KYCVerification;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KYCVerificationDto {
    private UUID id;
    private UUID userId;
    private KYCVerification.DocumentType documentType;
    private String documentUrl;
    private KYCVerification.VerificationStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private UUID reviewedBy;
    private String rejectionReason;
    private LocalDateTime expiryDate;
    
    // User information
    private String userFirstName;
    private String userLastName;
    private String userEmail;
    
    // Reviewer information
    private String reviewerFirstName;
    private String reviewerLastName;
}
