package tn.esprit.pi.nexlance.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.nexlance.entities.KYCVerification;
import tn.esprit.pi.nexlance.repositories.KYCVerificationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KYCVerificationService {

    private final KYCVerificationRepository kycVerificationRepository;

    @Transactional
    public KYCVerification submitDocument(KYCVerification verification) {
        verification.setStatus(KYCVerification.VerificationStatus.PENDING);
        return kycVerificationRepository.save(verification);
    }

    public KYCVerification getVerificationById(UUID id) {
        return kycVerificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Verification not found with id: " + id));
    }

    public List<KYCVerification> getVerificationsByUserId(UUID userId) {
        return kycVerificationRepository.findByUserId(userId);
    }

    public List<KYCVerification> getVerificationsByUserIdAndStatus(UUID userId, KYCVerification.VerificationStatus status) {
        return kycVerificationRepository.findByUserIdAndStatus(userId, status);
    }

    public Page<KYCVerification> getVerificationsByStatus(KYCVerification.VerificationStatus status, Pageable pageable) {
        return kycVerificationRepository.findByStatus(status, pageable);
    }

    public Page<KYCVerification> getAllVerifications(Pageable pageable) {
        return kycVerificationRepository.findAll(pageable);
    }

    @Transactional
    public KYCVerification approveVerification(UUID id, UUID reviewerId) {
        KYCVerification verification = getVerificationById(id);
        
        if (verification.getStatus() != KYCVerification.VerificationStatus.PENDING) {
            throw new RuntimeException("Only pending verifications can be approved");
        }
        
        verification.setStatus(KYCVerification.VerificationStatus.APPROVED);
        verification.setReviewedAt(LocalDateTime.now());
        verification.setReviewedBy(reviewerId);
        verification.setRejectionReason(null);
        
        return kycVerificationRepository.save(verification);
    }

    @Transactional
    public KYCVerification rejectVerification(UUID id, UUID reviewerId, String reason) {
        KYCVerification verification = getVerificationById(id);
        
        if (verification.getStatus() != KYCVerification.VerificationStatus.PENDING) {
            throw new RuntimeException("Only pending verifications can be rejected");
        }
        
        verification.setStatus(KYCVerification.VerificationStatus.REJECTED);
        verification.setReviewedAt(LocalDateTime.now());
        verification.setReviewedBy(reviewerId);
        verification.setRejectionReason(reason);
        
        return kycVerificationRepository.save(verification);
    }

    @Transactional
    public void deleteExpiredDocuments() {
        List<KYCVerification> expiredDocs = kycVerificationRepository
                .findByStatusAndExpiryDateBefore(KYCVerification.VerificationStatus.APPROVED, LocalDateTime.now());
        
        for (KYCVerification doc : expiredDocs) {
            doc.setStatus(KYCVerification.VerificationStatus.EXPIRED);
        }
        
        kycVerificationRepository.saveAll(expiredDocs);
    }

    @Transactional
    public void deleteVerification(UUID id) {
        if (!kycVerificationRepository.existsById(id)) {
            throw new RuntimeException("Verification not found with id: " + id);
        }
        kycVerificationRepository.deleteById(id);
    }
}
