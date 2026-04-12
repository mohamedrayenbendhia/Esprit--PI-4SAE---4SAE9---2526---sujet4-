package tn.esprit.pi.nexlance.recommendation.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.nexlance.module_job_offers.entities.JobOffer;
import tn.esprit.pi.nexlance.module_job_offers.enums.JobOfferStatus;
import tn.esprit.pi.nexlance.module_job_offers.repositories.JobOfferRepository;
import tn.esprit.pi.nexlance.notification.NotificationService;
import tn.esprit.pi.nexlance.recommendation.entities.Recommendation;
import tn.esprit.pi.nexlance.recommendation.enums.RecommendationStatus;
import tn.esprit.pi.nexlance.recommendation.repositories.RecommendationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final RecommendationActivityService activityService;
    private final JobOfferRepository jobOfferRepository;
    private final NotificationService notificationService;

    @Override
    public Recommendation createRecommendation(Recommendation recommendation) {
        
        // Set default values
        if (recommendation.getStatus() == null) {
            recommendation.setStatus(RecommendationStatus.PENDING);
        }
        if (recommendation.getViewCount() == null) {
            recommendation.setViewCount(0);
        }
        if (recommendation.getIsReminderSent() == null) {
            recommendation.setIsReminderSent(false);
        }
        
        // Capture transient clientName before save (it gets lost after persistence)
        String clientDisplayName = recommendation.getClientName();
        
        Recommendation saved = recommendationRepository.save(recommendation);
        
        // Log activity
        activityService.logRecommendationCreated(saved);
        
        // Send WebSocket notification to freelancer
        try {
            String jobTitle = "a job offer";
            if (saved.getJobOfferId() != null) {
                jobOfferRepository.findById(UUID.fromString(saved.getJobOfferId()))
                    .ifPresent(job -> log.info("Recommendation created for job: {}", job.getTitle()));
                jobTitle = jobOfferRepository.findById(UUID.fromString(saved.getJobOfferId()))
                    .map(JobOffer::getTitle).orElse("a job offer");
            }
            String displayName = (clientDisplayName != null && !clientDisplayName.isBlank()) ? clientDisplayName : saved.getClientId();
            notificationService.notifyNewRecommendation(
                saved.getFreelanceId(), displayName, jobTitle, saved.getId().toString());
        } catch (Exception e) {
            log.warn("Failed to send recommendation notification: {}", e.getMessage());
        }
        
        return saved;
    }

    @Override
    public Recommendation updateRecommendation(Long id, Recommendation recommendation) {
        Recommendation existing = recommendationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recommendation not found with id: " + id));
        
        // Update fields
        if (recommendation.getMessage() != null) {
            existing.setMessage(recommendation.getMessage());
        }
        if (recommendation.getProposedBudget() != null) {
            existing.setProposedBudget(recommendation.getProposedBudget());
        }
        if (recommendation.getExpirationDate() != null) {
            existing.setExpirationDate(recommendation.getExpirationDate());
        }
        
        Recommendation updated = recommendationRepository.save(existing);
        
        // Log activity
        activityService.logRecommendationUpdated(updated);
        
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Recommendation> getRecommendationById(Long id) {
        return recommendationRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Recommendation> getAllRecommendations() {
        return recommendationRepository.findAll();
    }

    @Override
    public void deleteRecommendation(Long id) {
        recommendationRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Recommendation> getRecommendationsByClientId(String clientId) {
        return recommendationRepository.findByClientId(clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Recommendation> getRecommendationsByFreelanceId(String freelanceId) {
        return recommendationRepository.findByFreelanceId(freelanceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Recommendation> getRecommendationsByJobOfferId(String jobOfferId) {
        return recommendationRepository.findByJobOfferId(jobOfferId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Recommendation> getRecommendationsByStatus(RecommendationStatus status) {
        return recommendationRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Recommendation> getPendingRecommendationsForFreelance(String freelanceId) {
        return recommendationRepository.findPendingRecommendationsForFreelance(freelanceId);
    }

    @Override
    public Recommendation acceptRecommendation(Long id, String response) {
        Recommendation recommendation = recommendationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recommendation not found with id: " + id));
        
        if (!recommendation.canBeAccepted()) {
            throw new RuntimeException("Recommendation cannot be accepted in current state");
        }
        
        recommendation.setStatus(RecommendationStatus.ACCEPTED);
        recommendation.setResponseDate(LocalDateTime.now());
        
        Recommendation updated = recommendationRepository.save(recommendation);
        
        // Update JobOffer status to IN_PROGRESS so it's hidden from browse-jobs
        if (updated.getJobOfferId() != null) {
            try {
                UUID jobOfferId = UUID.fromString(updated.getJobOfferId());
                jobOfferRepository.findById(jobOfferId).ifPresent(jobOffer -> {
                    jobOffer.setStatus(JobOfferStatus.IN_PROGRESS);
                    jobOfferRepository.save(jobOffer);
                    log.info("JobOffer {} status updated to IN_PROGRESS after recommendation accepted", jobOfferId);
                });
            } catch (Exception e) {
                log.warn("Failed to update JobOffer status: {}", e.getMessage());
            }
        }
        
        // Log activity
        activityService.logRecommendationAccepted(updated, response);
        
        // Send WebSocket notification to client
        try {
            notificationService.notifyRecommendationResponse(
                updated.getClientId(), updated.getFreelanceId(), "ACCEPTED", updated.getId().toString());
        } catch (Exception e) {
            log.warn("Failed to send recommendation accept notification: {}", e.getMessage());
        }
        
        return updated;
    }

    @Override
    public Recommendation rejectRecommendation(Long id, String response) {
        Recommendation recommendation = recommendationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recommendation not found with id: " + id));
        
        if (recommendation.getStatus() != RecommendationStatus.PENDING) {
            throw new RuntimeException("Recommendation cannot be rejected in current state");
        }
        
        recommendation.setStatus(RecommendationStatus.REJECTED);
        recommendation.setResponseDate(LocalDateTime.now());
        recommendation.setCancellationReason(response);
        
        Recommendation updated = recommendationRepository.save(recommendation);
        
        // Log activity
        activityService.logRecommendationRejected(updated, response);
        
        // Send WebSocket notification to client
        try {
            notificationService.notifyRecommendationResponse(
                updated.getClientId(), updated.getFreelanceId(), "REJECTED", updated.getId().toString());
        } catch (Exception e) {
            log.warn("Failed to send recommendation reject notification: {}", e.getMessage());
        }
        
        return updated;
    }

    @Override
    public Recommendation cancelRecommendation(Long id, String reason) {
        Recommendation recommendation = recommendationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recommendation not found with id: " + id));
        
        if (recommendation.getStatus() != RecommendationStatus.PENDING) {
            throw new RuntimeException("Recommendation cannot be cancelled in current state");
        }
        
        recommendation.setStatus(RecommendationStatus.CANCELLED);
        recommendation.setCancellationReason(reason);
        
        Recommendation cancelledReco = recommendationRepository.save(recommendation);
        
        // Log activity
        activityService.logRecommendationCancelled(cancelledReco, reason);
        
        return cancelledReco;
    }

    @Override
    public Recommendation incrementViews(Long id) {
        Recommendation recommendation = recommendationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recommendation not found with id: " + id));
        
        recommendation.incrementViews();
        Recommendation updated = recommendationRepository.save(recommendation);
        
        // Log activity
        activityService.logRecommendationViewed(updated);
        
        return updated;
    }

    @Override
    public Recommendation sendReminder(Long id) {
        Recommendation recommendation = recommendationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recommendation not found with id: " + id));
        
        recommendation.setIsReminderSent(true);
        recommendation.setReminderSentDate(LocalDateTime.now());
        Recommendation updated = recommendationRepository.save(recommendation);
        
        // Log activity
        activityService.logReminderSent(updated);
        
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Recommendation> findRecommendationsNeedingReminder() {
        // Find recommendations that haven't been reminded in the last 24 hours
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        return recommendationRepository.findRecommendationsNeedingReminder(threshold);
    }

    @Override
    public List<Recommendation> findAndExpireOldRecommendations() {
        List<Recommendation> expiredRecommendations = recommendationRepository.findExpiredRecommendations();
        
        for (Recommendation recommendation : expiredRecommendations) {
            recommendation.setStatus(RecommendationStatus.EXPIRED);
            recommendationRepository.save(recommendation);
            
            // Log activity
            activityService.logRecommendationExpired(recommendation);
        }
        
        return expiredRecommendations;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean recommendationExists(String clientId, String freelanceId, String jobOfferId) {
        return recommendationRepository.existsByClientIdAndFreelanceIdAndJobOfferId(
                clientId, freelanceId, jobOfferId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(RecommendationStatus status) {
        return recommendationRepository.countByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByFreelanceIdAndStatus(String freelanceId, RecommendationStatus status) {
        return recommendationRepository.countByFreelanceIdAndStatus(freelanceId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByClientIdAndStatus(String clientId, RecommendationStatus status) {
        return recommendationRepository.countByClientIdAndStatus(clientId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Recommendation> getRecentRecommendations(LocalDateTime since) {
        return recommendationRepository.findRecentRecommendations(since);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Recommendation> getTopViewedRecommendations() {
        return recommendationRepository.findTopViewedRecommendations();
    }
}
