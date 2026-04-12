package tn.esprit.pi.nexlance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.pi.nexlance.entities.KYCVerification;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitKYCRequest {
    
    @NotNull(message = "Document type is required")
    private KYCVerification.DocumentType documentType;
    
    @NotNull(message = "Document file is required")
    private MultipartFile documentFile;
    
    private LocalDateTime expiryDate;
}
