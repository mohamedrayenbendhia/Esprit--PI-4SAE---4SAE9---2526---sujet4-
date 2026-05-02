package tn.esprit.pi.nexlance.module_job_offers.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.pi.nexlance.module_job_offers.entities.Application;
import tn.esprit.pi.nexlance.module_job_offers.enums.ApplicationStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    
    List<Application> findByJobOfferId(UUID jobOfferId);
    
    List<Application> findByFreelanceId(UUID freelanceId);
    
    List<Application> findByStatus(ApplicationStatus status);
    
    List<Application> findByJobOfferIdAndStatus(UUID jobOfferId, ApplicationStatus status);
    
    List<Application> findByFreelanceIdAndStatus(UUID freelanceId, ApplicationStatus status);
    
    List<Application> findByJobOfferIdAndIsRead(UUID jobOfferId, Boolean isRead);
    
    Long countByJobOfferId(UUID jobOfferId);
    
    Long countByJobOfferIdAndStatus(UUID jobOfferId, ApplicationStatus status);
}
