package tn.esprit.pi.nexlance.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.pi.nexlance.entities.KYCVerification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface KYCVerificationRepository extends JpaRepository<KYCVerification, UUID> {
    
    List<KYCVerification> findByUserId(UUID userId);
    
    Page<KYCVerification> findByStatus(KYCVerification.VerificationStatus status, Pageable pageable);
    
    List<KYCVerification> findByStatusAndExpiryDateBefore(KYCVerification.VerificationStatus status, LocalDateTime date);
    
    List<KYCVerification> findByUserIdAndStatus(UUID userId, KYCVerification.VerificationStatus status);
}
