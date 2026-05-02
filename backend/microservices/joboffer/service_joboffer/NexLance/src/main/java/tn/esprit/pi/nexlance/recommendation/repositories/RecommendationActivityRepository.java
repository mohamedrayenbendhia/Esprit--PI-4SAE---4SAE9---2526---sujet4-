package tn.esprit.pi.nexlance.recommendation.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.pi.nexlance.recommendation.entities.RecommendationActivity;
import tn.esprit.pi.nexlance.recommendation.enums.ActivityType;
import tn.esprit.pi.nexlance.recommendation.enums.UserType;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecommendationActivityRepository extends JpaRepository<RecommendationActivity, Long> {

    // Find by recommendation
    List<RecommendationActivity> findByRecommendationId(Long recommendationId);

    // Find by recommendation ordered by date
    List<RecommendationActivity> findByRecommendationIdOrderByCreatedAtDesc(Long recommendationId);

    // Find by activity type
    List<RecommendationActivity> findByActivityType(ActivityType activityType);

    // Find by recommendation and activity type
    List<RecommendationActivity> findByRecommendationIdAndActivityType(Long recommendationId, ActivityType activityType);

    // Find by user
    List<RecommendationActivity> findByUserIdAndUserType(String userId, UserType userType);

    // Find recent activities
    @Query("SELECT a FROM RecommendationActivity a WHERE a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<RecommendationActivity> findRecentActivities(@Param("since") LocalDateTime since);

    // Find activities by recommendation and user
    @Query("SELECT a FROM RecommendationActivity a WHERE a.recommendationId = :recommendationId " +
           "AND a.userId = :userId AND a.userType = :userType ORDER BY a.createdAt DESC")
    List<RecommendationActivity> findByRecommendationAndUser(
            @Param("recommendationId") Long recommendationId,
            @Param("userId") String userId,
            @Param("userType") UserType userType
    );

    // Count activities by type
    long countByActivityType(ActivityType activityType);

    // Count activities for recommendation
    long countByRecommendationId(Long recommendationId);

    // Find latest activity for recommendation
    @Query("SELECT a FROM RecommendationActivity a WHERE a.recommendationId = :recommendationId " +
           "ORDER BY a.createdAt DESC LIMIT 1")
    RecommendationActivity findLatestActivityForRecommendation(@Param("recommendationId") Long recommendationId);

    // Delete old activities
    @Query("DELETE FROM RecommendationActivity a WHERE a.createdAt < :before")
    void deleteActivitiesOlderThan(@Param("before") LocalDateTime before);
}
