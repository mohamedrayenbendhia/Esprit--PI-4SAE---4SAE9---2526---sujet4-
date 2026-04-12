package tn.esprit.pi.nexlance.recommendation.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.pi.nexlance.recommendation.entities.Recommendation;
import tn.esprit.pi.nexlance.recommendation.enums.RecommendationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    // Find by client
    List<Recommendation> findByClientId(String clientId);

    // Find by freelance
    List<Recommendation> findByFreelanceId(String freelanceId);

    // Find by job offer
    List<Recommendation> findByJobOfferId(String jobOfferId);

    // Find by status
    List<Recommendation> findByStatus(RecommendationStatus status);

    // Find by freelance and status
    List<Recommendation> findByFreelanceIdAndStatus(String freelanceId, RecommendationStatus status);

    // Find by client and status
    List<Recommendation> findByClientIdAndStatus(String clientId, RecommendationStatus status);

    // Find by job offer and status
    List<Recommendation> findByJobOfferIdAndStatus(String jobOfferId, RecommendationStatus status);

    // Check if recommendation exists
    boolean existsByClientIdAndFreelanceIdAndJobOfferId(String clientId, String freelanceId, String jobOfferId);

    // Find pending recommendations for freelance
    @Query("SELECT r FROM Recommendation r WHERE r.freelanceId = :freelanceId " +
           "AND r.status = 'PENDING' AND (r.expirationDate IS NULL OR r.expirationDate > CURRENT_TIMESTAMP)")
    List<Recommendation> findPendingRecommendationsForFreelance(@Param("freelanceId") String freelanceId);

    // Find expired recommendations
    @Query("SELECT r FROM Recommendation r WHERE r.status = 'PENDING' " +
           "AND r.expirationDate IS NOT NULL AND r.expirationDate < CURRENT_TIMESTAMP")
    List<Recommendation> findExpiredRecommendations();

    // Find recommendations needing reminder
    @Query("SELECT r FROM Recommendation r WHERE r.status = 'PENDING' " +
           "AND r.isReminderSent = false " +
           "AND (r.expirationDate IS NULL OR r.expirationDate > CURRENT_TIMESTAMP)")
    List<Recommendation> findRecommendationsNeedingReminder(@Param("reminderThreshold") LocalDateTime reminderThreshold);

    // Count by status
    long countByStatus(RecommendationStatus status);

    // Count by freelance and status
    long countByFreelanceIdAndStatus(String freelanceId, RecommendationStatus status);

    // Count by client and status
    long countByClientIdAndStatus(String clientId, RecommendationStatus status);

    // Find recent recommendations
    @Query("SELECT r FROM Recommendation r WHERE r.createdAt >= :since ORDER BY r.createdAt DESC")
    List<Recommendation> findRecentRecommendations(@Param("since") LocalDateTime since);

    // Find top viewed recommendations
    @Query("SELECT r FROM Recommendation r WHERE r.viewCount > 0 ORDER BY r.viewCount DESC")
    List<Recommendation> findTopViewedRecommendations();
}
