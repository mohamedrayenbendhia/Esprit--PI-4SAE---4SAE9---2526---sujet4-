package tn.esprit.pi.nexlance.module_job_offers.services;

import tn.esprit.pi.nexlance.module_job_offers.entities.Application;
import tn.esprit.pi.nexlance.module_job_offers.enums.ApplicationStatus;

import java.util.List;
import java.util.UUID;

public interface ApplicationService {
    
    Application createApplication(Application application);
    
    Application getApplicationById(UUID id);
    
    List<Application> getAllApplications();
    
    List<Application> getApplicationsByJobOfferId(UUID jobOfferId);
    
    List<Application> getApplicationsByFreelanceId(UUID freelanceId);
    
    List<Application> getApplicationsByStatus(ApplicationStatus status);
    
    List<Application> getUnreadApplicationsByJobOfferId(UUID jobOfferId);
    
    Long countApplicationsByJobOfferId(UUID jobOfferId);
    
    Long countApplicationsByJobOfferIdAndStatus(UUID jobOfferId, ApplicationStatus status);
    
    java.util.Map<String, Long> getApplicationCountsByStatus(UUID jobOfferId);
    
    Application updateApplication(UUID id, Application application);
    
    Application updateApplicationStatus(UUID id, ApplicationStatus status);
    
    Application markAsRead(UUID id);
    
    void deleteApplication(UUID id);
    
    Application withdrawApplication(UUID id);
}
