package tn.esprit.pi.nexlance.recommendation.services;

import tn.esprit.pi.nexlance.recommendation.entities.Recommendation;
import tn.esprit.pi.nexlance.recommendation.entities.RecommendationActivity;
import tn.esprit.pi.nexlance.recommendation.enums.ActivityType;
import tn.esprit.pi.nexlance.recommendation.enums.UserType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface RecommendationActivityService {

    // CRUD operations
    RecommendationActivity createActivity(RecommendationActivity activity);
    List<RecommendationActivity> getActivitiesByRecommendationId(Long recommendationId);
    List<RecommendationActivity> getActivitiesByType(ActivityType activityType);
    
    // Logging methods
    void logRecommendationCreated(Recommendation recommendation);
    void logRecommendationSent(Recommendation recommendation);
    void logRecommendationViewed(Recommendation recommendation);
    void logRecommendationAccepted(Recommendation recommendation, String response);
    void logRecommendationRejected(Recommendation recommendation, String response);
    void logRecommendationCancelled(Recommendation recommendation, String reason);
    void logRecommendationExpired(Recommendation recommendation);
    void logRecommendationUpdated(Recommendation recommendation);
    void logReminderSent(Recommendation recommendation);
    
    // Custom activity logging
    RecommendationActivity logActivity(
            Long recommendationId,
            ActivityType activityType,
            String userId,
            UserType userType,
            Map<String, Object> data,
            String ipAddress,
            String userAgent
    );
    
    // Query operations
    List<RecommendationActivity> getRecentActivities(LocalDateTime since);
    List<RecommendationActivity> getActivitiesByUser(String userId, UserType userType);
    long countActivitiesByType(ActivityType activityType);
    
    // Cleanup
    void deleteOldActivities(LocalDateTime before);
}
