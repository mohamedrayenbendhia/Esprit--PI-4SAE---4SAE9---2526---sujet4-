package tn.esprit.pi.nexlance.recommendation.services;

import tn.esprit.pi.nexlance.recommendation.entities.Recommendation;
import tn.esprit.pi.nexlance.recommendation.enums.RecommendationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RecommendationService {

    // CRUD operations
    Recommendation createRecommendation(Recommendation recommendation);
    Recommendation updateRecommendation(Long id, Recommendation recommendation);
    Optional<Recommendation> getRecommendationById(Long id);
    List<Recommendation> getAllRecommendations();
    void deleteRecommendation(Long id);

    // Query operations
    List<Recommendation> getRecommendationsByClientId(String clientId);
    List<Recommendation> getRecommendationsByFreelanceId(String freelanceId);
    List<Recommendation> getRecommendationsByJobOfferId(String jobOfferId);
    List<Recommendation> getRecommendationsByStatus(RecommendationStatus status);
    List<Recommendation> getPendingRecommendationsForFreelance(String freelanceId);
    
    // Status operations
    Recommendation acceptRecommendation(Long id, String response);
    Recommendation rejectRecommendation(Long id, String response);
    Recommendation cancelRecommendation(Long id, String reason);
    
    // View tracking
    Recommendation incrementViews(Long id);
    
    // Reminder operations
    Recommendation sendReminder(Long id);
    List<Recommendation> findRecommendationsNeedingReminder();
    
    // Expiration handling
    List<Recommendation> findAndExpireOldRecommendations();
    
    // Existence check
    boolean recommendationExists(String clientId, String freelanceId, String jobOfferId);
    
    // Statistics
    long countByStatus(RecommendationStatus status);
    long countByFreelanceIdAndStatus(String freelanceId, RecommendationStatus status);
    long countByClientIdAndStatus(String clientId, RecommendationStatus status);
    
    // Recent and popular
    List<Recommendation> getRecentRecommendations(LocalDateTime since);
    List<Recommendation> getTopViewedRecommendations();
}
