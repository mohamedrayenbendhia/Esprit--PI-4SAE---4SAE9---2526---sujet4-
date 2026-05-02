package tn.esprit.pi.nexlance.module_job_offers.services;

import tn.esprit.pi.nexlance.module_job_offers.dto.*;
import tn.esprit.pi.nexlance.module_job_offers.entities.JobOffer;
import tn.esprit.pi.nexlance.module_job_offers.enums.JobCategory;
import tn.esprit.pi.nexlance.module_job_offers.enums.JobOfferStatus;
import tn.esprit.pi.nexlance.module_job_offers.enums.ExperienceLevel;

import java.util.List;
import java.util.UUID;

public interface JobOfferService {
    
    JobOffer createJobOffer(JobOffer jobOffer);
    
    JobOffer getJobOfferById(UUID id);
    
    List<JobOffer> getAllJobOffers();
    
    List<JobOffer> getJobOffersByStatus(JobOfferStatus status);
    
    List<JobOffer> getJobOffersByClientId(UUID clientId);
    
    List<JobOffer> getActiveJobOffers();
    
    List<JobOffer> getJobOffersByCategory(JobCategory category);
    
    List<JobOffer> getRemoteJobOffers();
    
    List<JobOffer> getJobOffersByExperienceLevel(ExperienceLevel experienceLevel);
    
    JobOffer updateJobOffer(UUID id, JobOffer jobOffer);
    
    JobOffer updateJobOfferStatus(UUID id, JobOfferStatus status);
    
    void deleteJobOffer(UUID id);
    
    JobOffer archiveJobOffer(UUID id);
    
    JobOffer incrementViewCount(UUID id);
    
    JobOffer incrementApplicantCount(UUID id);
    
    JobOfferStatsDTO getJobOfferStats();
    
    List<CategoryDistributionDTO> getCategoryDistribution();
    
    List<BudgetDistributionDTO> getBudgetDistribution();
    
    List<TopClientDTO> getTopClients(int limit);
    
    List<MonthlyDataDTO> getMonthlyData(int months);
}
