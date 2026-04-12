package tn.esprit.pi.nexlance.module_job_offers.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.pi.nexlance.module_job_offers.entities.JobOffer;
import tn.esprit.pi.nexlance.module_job_offers.enums.JobOfferStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobOfferRepository extends JpaRepository<JobOffer, UUID> {
    
    List<JobOffer> findByClientId(UUID clientId);
    
    List<JobOffer> findByStatus(JobOfferStatus status);
    
    List<JobOffer> findByStatusNot(JobOfferStatus status);
    
    List<JobOffer> findByStatusAndClientId(JobOfferStatus status, UUID clientId);
    
    List<JobOffer> findByCategory(tn.esprit.pi.nexlance.module_job_offers.enums.JobCategory category);
    
    List<JobOffer> findByIsRemote(Boolean isRemote);
    
    List<JobOffer> findByExperienceLevel(tn.esprit.pi.nexlance.module_job_offers.enums.ExperienceLevel experienceLevel);
}
