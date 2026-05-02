package tn.esprit.pi.nexlance.module_job_offers.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.nexlance.audit.AuditLogService;
import tn.esprit.pi.nexlance.module_job_offers.entities.Application;
import tn.esprit.pi.nexlance.module_job_offers.entities.JobOffer;
import tn.esprit.pi.nexlance.module_job_offers.enums.ApplicationStatus;
import tn.esprit.pi.nexlance.module_job_offers.repositories.ApplicationRepository;
import tn.esprit.pi.nexlance.notification.NotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobOfferService jobOfferService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Autowired
    public ApplicationServiceImpl(ApplicationRepository applicationRepository,
                                   JobOfferService jobOfferService,
                                   AuditLogService auditLogService,
                                   NotificationService notificationService) {
        this.applicationRepository = applicationRepository;
        this.jobOfferService = jobOfferService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Override
    public Application createApplication(Application application) {
        if (application.getJobOfferId() == null) {
            throw new IllegalArgumentException("JobOfferId is required");
        }
        if (application.getFreelanceId() == null) {
            throw new IllegalArgumentException("FreelanceId is required");
        }
        
        // Verify job offer exists
        JobOffer jobOffer = jobOfferService.getJobOfferById(application.getJobOfferId());
        
        if (application.getStatus() == null) {
            application.setStatus(ApplicationStatus.PENDING);
        }
        if (application.getIsRead() == null) {
            application.setIsRead(false);
        }
        
        Application savedApplication = applicationRepository.save(application);
        
        // Increment applicant count in job offer
        jobOfferService.incrementApplicantCount(application.getJobOfferId());

        auditLogService.logAction("CREATE", "APPLICATION", savedApplication.getId().toString(),
                savedApplication.getFreelanceId() != null ? savedApplication.getFreelanceId().toString() : null,
                "FREELANCE",
                "Applied for job offer: " + application.getJobOfferId(),
                null, null);

        if (jobOffer.getClientId() != null) {
            notificationService.notifyApplicationCreated(
                    jobOffer.getClientId().toString(),
                    jobOffer.getId().toString(),
                    jobOffer.getTitle(),
                    savedApplication.getFreelanceId().toString()
            );
        }
        
        return savedApplication;
    }

    @Override
    @Transactional(readOnly = true)
    public Application getApplicationById(UUID id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Application> getAllApplications() {
        return applicationRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Application> getApplicationsByJobOfferId(UUID jobOfferId) {
        return applicationRepository.findByJobOfferId(jobOfferId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Application> getApplicationsByFreelanceId(UUID freelanceId) {
        return applicationRepository.findByFreelanceId(freelanceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Application> getApplicationsByStatus(ApplicationStatus status) {
        return applicationRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Application> getUnreadApplicationsByJobOfferId(UUID jobOfferId) {
        return applicationRepository.findByJobOfferIdAndIsRead(jobOfferId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countApplicationsByJobOfferId(UUID jobOfferId) {
        return applicationRepository.countByJobOfferId(jobOfferId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countApplicationsByJobOfferIdAndStatus(UUID jobOfferId, ApplicationStatus status) {
        return applicationRepository.countByJobOfferIdAndStatus(jobOfferId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<String, Long> getApplicationCountsByStatus(UUID jobOfferId) {
        java.util.Map<String, Long> counts = new java.util.HashMap<>();
        counts.put("total", applicationRepository.countByJobOfferId(jobOfferId));
        counts.put("pending", applicationRepository.countByJobOfferIdAndStatus(jobOfferId, ApplicationStatus.PENDING));
        counts.put("shortlisted", applicationRepository.countByJobOfferIdAndStatus(jobOfferId, ApplicationStatus.SHORTLISTED));
        counts.put("accepted", applicationRepository.countByJobOfferIdAndStatus(jobOfferId, ApplicationStatus.ACCEPTED));
        counts.put("rejected", applicationRepository.countByJobOfferIdAndStatus(jobOfferId, ApplicationStatus.REJECTED));
        counts.put("withdrawn", applicationRepository.countByJobOfferIdAndStatus(jobOfferId, ApplicationStatus.WITHDRAWN));
        return counts;
    }

    @Override
    public Application updateApplication(UUID id, Application application) {
        Application existingApplication = getApplicationById(id);
        
        if (application.getCoverLetter() != null) {
            existingApplication.setCoverLetter(application.getCoverLetter());
        }
        if (application.getProposedRate() != null) {
            existingApplication.setProposedRate(application.getProposedRate());
        }
        if (application.getEstimatedDelivery() != null) {
            existingApplication.setEstimatedDelivery(application.getEstimatedDelivery());
        }
        if (application.getPortfolioItems() != null) {
            existingApplication.setPortfolioItems(application.getPortfolioItems());
        }
        if (application.getAvailableFrom() != null) {
            existingApplication.setAvailableFrom(application.getAvailableFrom());
        }
        
        Application updated = applicationRepository.save(existingApplication);
        auditLogService.logAction("UPDATE", "APPLICATION", updated.getId().toString(),
                updated.getFreelanceId() != null ? updated.getFreelanceId().toString() : null,
                "FREELANCE", "Updated application for job: " + updated.getJobOfferId(),
                null, null);
        return updated;
    }

    @Override
    public Application updateApplicationStatus(UUID id, ApplicationStatus status) {
        Application application = getApplicationById(id);
        String oldStatus = application.getStatus() != null ? application.getStatus().name() : "null";
        application.setStatus(status);
        
        if (status != ApplicationStatus.PENDING && application.getRespondedAt() == null) {
            application.setRespondedAt(LocalDateTime.now());
        }
        
        Application updated = applicationRepository.save(application);
        auditLogService.logAction("STATUS_CHANGE", "APPLICATION", updated.getId().toString(),
                updated.getFreelanceId() != null ? updated.getFreelanceId().toString() : null,
                "CLIENT",
                "Application status changed: " + oldStatus + " \u2192 " + status.name(),
                oldStatus, status.name());
        if (updated.getFreelanceId() != null) {
            String jobTitle = jobOfferService.getJobOfferById(updated.getJobOfferId()).getTitle();
            notificationService.notifyApplicationStatusChanged(
                    updated.getFreelanceId().toString(),
                    updated.getId().toString(),
                    jobTitle,
                    status.name()
            );
        }
        return updated;
    }

    @Override
    public Application markAsRead(UUID id) {
        Application application = getApplicationById(id);
        application.setIsRead(true);
        return applicationRepository.save(application);
    }

    @Override
    public void deleteApplication(UUID id) {
        Application application = getApplicationById(id);
        
        if (application.getStatus() == ApplicationStatus.ACCEPTED) {
            throw new IllegalStateException("Cannot delete accepted application");
        }
        
        auditLogService.logAction("DELETE", "APPLICATION", id.toString(),
                application.getFreelanceId() != null ? application.getFreelanceId().toString() : null,
                "FREELANCE", "Deleted application for job: " + application.getJobOfferId(),
                null, null);
        applicationRepository.delete(application);
    }

    @Override
    public Application withdrawApplication(UUID id) {
        Application application = getApplicationById(id);
        
        if (application.getStatus() == ApplicationStatus.ACCEPTED) {
            throw new IllegalStateException("Cannot withdraw accepted application");
        }
        
        return updateApplicationStatus(id, ApplicationStatus.WITHDRAWN);
    }
}
