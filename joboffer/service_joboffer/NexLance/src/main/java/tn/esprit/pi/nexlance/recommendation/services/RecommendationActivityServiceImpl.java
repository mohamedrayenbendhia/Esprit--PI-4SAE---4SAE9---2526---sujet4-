package tn.esprit.pi.nexlance.recommendation.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.nexlance.recommendation.entities.Recommendation;
import tn.esprit.pi.nexlance.recommendation.entities.RecommendationActivity;
import tn.esprit.pi.nexlance.recommendation.enums.ActivityType;
import tn.esprit.pi.nexlance.recommendation.enums.UserType;
import tn.esprit.pi.nexlance.recommendation.repositories.RecommendationActivityRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class RecommendationActivityServiceImpl implements RecommendationActivityService {

    private final RecommendationActivityRepository activityRepository;

    @Override
    public RecommendationActivity createActivity(RecommendationActivity activity) {
        return activityRepository.save(activity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationActivity> getActivitiesByRecommendationId(Long recommendationId) {
        return activityRepository.findByRecommendationIdOrderByCreatedAtDesc(recommendationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationActivity> getActivitiesByType(ActivityType activityType) {
        return activityRepository.findByActivityType(activityType);
    }

    @Override
    public void logRecommendationCreated(Recommendation recommendation) {
        Map<String, Object> data = new HashMap<>();
        data.put("clientId", recommendation.getClientId());
        data.put("freelanceId", recommendation.getFreelanceId());
        data.put("jobOfferId", recommendation.getJobOfferId());
        data.put("proposedBudget", recommendation.getProposedBudget());
        
        logActivity(
            recommendation.getId(),
            ActivityType.CREATED,
            recommendation.getClientId(),
            UserType.CLIENT,
            data,
            null,
            null
        );
    }

    @Override
    public void logRecommendationSent(Recommendation recommendation) {
        Map<String, Object> data = new HashMap<>();
        data.put("freelanceId", recommendation.getFreelanceId());
        data.put("expiresAt", recommendation.getExpirationDate());
        
        logActivity(
            recommendation.getId(),
            ActivityType.SENT,
            null,
            UserType.SYSTEM,
            data,
            null,
            null
        );
    }

    @Override
    public void logRecommendationViewed(Recommendation recommendation) {
        Map<String, Object> data = new HashMap<>();
        data.put("viewsCount", recommendation.getViewCount());
        
        logActivity(
            recommendation.getId(),
            ActivityType.VIEWED,
            recommendation.getFreelanceId(),
            UserType.FREELANCE,
            data,
            null,
            null
        );
    }

    @Override
    public void logRecommendationAccepted(Recommendation recommendation, String response) {
        Map<String, Object> data = new HashMap<>();
        data.put("response", response);
        data.put("respondedAt", recommendation.getResponseDate());
        
        logActivity(
            recommendation.getId(),
            ActivityType.ACCEPTED,
            recommendation.getFreelanceId(),
            UserType.FREELANCE,
            data,
            null,
            null
        );
    }

    @Override
    public void logRecommendationRejected(Recommendation recommendation, String response) {
        Map<String, Object> data = new HashMap<>();
        data.put("response", response);
        data.put("respondedAt", recommendation.getResponseDate());
        
        logActivity(
            recommendation.getId(),
            ActivityType.REJECTED,
            recommendation.getFreelanceId(),
            UserType.FREELANCE,
            data,
            null,
            null
        );
    }

    @Override
    public void logRecommendationCancelled(Recommendation recommendation, String reason) {
        Map<String, Object> data = new HashMap<>();
        data.put("reason", reason);
        data.put("cancelledAt", LocalDateTime.now());
        
        logActivity(
            recommendation.getId(),
            ActivityType.CANCELLED,
            recommendation.getClientId(),
            UserType.CLIENT,
            data,
            null,
            null
        );
    }

    @Override
    public void logRecommendationExpired(Recommendation recommendation) {
        Map<String, Object> data = new HashMap<>();
        data.put("expiresAt", recommendation.getExpirationDate());
        
        logActivity(
            recommendation.getId(),
            ActivityType.EXPIRED,
            null,
            UserType.SYSTEM,
            data,
            null,
            null
        );
    }

    @Override
    public void logRecommendationUpdated(Recommendation recommendation) {
        Map<String, Object> data = new HashMap<>();
        data.put("updatedAt", recommendation.getUpdatedAt());
        
        logActivity(
            recommendation.getId(),
            ActivityType.UPDATED,
            recommendation.getClientId(),
            UserType.CLIENT,
            data,
            null,
            null
        );
    }

    @Override
    public void logReminderSent(Recommendation recommendation) {
        Map<String, Object> data = new HashMap<>();
        data.put("isReminderSent", recommendation.getIsReminderSent());
        data.put("reminderSentDate", recommendation.getReminderSentDate());
        
        logActivity(
            recommendation.getId(),
            ActivityType.REMINDED,
            null,
            UserType.SYSTEM,
            data,
            null,
            null
        );
    }

    @Override
    public RecommendationActivity logActivity(
            Long recommendationId,
            ActivityType activityType,
            String userId,
            UserType userType,
            Map<String, Object> data,
            String ipAddress,
            String userAgent) {
        RecommendationActivity activity = RecommendationActivity.builder()
                .recommendationId(recommendationId)
                .activityType(activityType)
                .userId(userId)
                .userType(userType)
                .activityData(RecommendationActivity.mapToJson(data))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        
        return activityRepository.save(activity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationActivity> getRecentActivities(LocalDateTime since) {
        return activityRepository.findRecentActivities(since);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationActivity> getActivitiesByUser(String userId, UserType userType) {
        return activityRepository.findByUserIdAndUserType(userId, userType);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActivitiesByType(ActivityType activityType) {
        return activityRepository.countByActivityType(activityType);
    }

    @Override
    public void deleteOldActivities(LocalDateTime before) {
        activityRepository.deleteActivitiesOlderThan(before);
    }
}
