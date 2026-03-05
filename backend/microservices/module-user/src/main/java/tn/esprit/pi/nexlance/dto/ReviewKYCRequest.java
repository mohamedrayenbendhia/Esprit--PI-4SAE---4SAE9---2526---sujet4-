package tn.esprit.pi.nexlance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.pi.nexlance.entities.KYCVerification;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewKYCRequest {
    
    @NotNull(message = "Status is required")
    private KYCVerification.VerificationStatus status;
    
    private String rejectionReason;
    private LocalDateTime expiryDate;
    private String notes;
}
