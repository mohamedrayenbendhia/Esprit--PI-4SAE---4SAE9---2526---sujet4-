package tn.esprit.pi.nexlance.recommendation.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi.nexlance.recommendation.entities.Recommendation;
import tn.esprit.pi.nexlance.recommendation.entities.RecommendationActivity;
import tn.esprit.pi.nexlance.recommendation.enums.ActivityType;
import tn.esprit.pi.nexlance.recommendation.enums.RecommendationStatus;
import tn.esprit.pi.nexlance.recommendation.enums.UserType;
import tn.esprit.pi.nexlance.recommendation.services.RecommendationActivityService;
import tn.esprit.pi.nexlance.recommendation.services.RecommendationService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final RecommendationActivityService activityService;

    // ==================== CRUD Operations ====================

    @PostMapping
    public ResponseEntity<Recommendation> createRecommendation(@RequestBody Recommendation recommendation) {
        // Check if recommendation already exists
        if (recommendationService.recommendationExists(
                recommendation.getClientId(),
                recommendation.getFreelanceId(),
                recommendation.getJobOfferId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(null);
        }
        
        Recommendation created = recommendationService.createRecommendation(recommendation);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Recommendation> getRecommendationById(@PathVariable Long id) {
        return recommendationService.getRecommendationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Recommendation>> getAllRecommendations() {
        List<Recommendation> recommendations = recommendationService.getAllRecommendations();
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Recommendation>> getAllRecommendationsAlias() {
        List<Recommendation> recommendations = recommendationService.getAllRecommendations();
        return ResponseEntity.ok(recommendations);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Recommendation> updateRecommendation(
            @PathVariable Long id,
            @RequestBody Recommendation recommendation) {
        try {
            Recommendation updated = recommendationService.updateRecommendation(id, recommendation);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecommendation(@PathVariable Long id) {
        recommendationService.deleteRecommendation(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Query Operations ====================

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<Recommendation>> getRecommendationsByClient(@PathVariable String clientId) {
        List<Recommendation> recommendations = recommendationService.getRecommendationsByClientId(clientId);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/freelance/{freelanceId}")
    public ResponseEntity<List<Recommendation>> getRecommendationsByFreelance(@PathVariable String freelanceId) {
        List<Recommendation> recommendations = recommendationService.getRecommendationsByFreelanceId(freelanceId);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/job-offer/{jobOfferId}")
    public ResponseEntity<List<Recommendation>> getRecommendationsByJobOffer(@PathVariable String jobOfferId) {
        List<Recommendation> recommendations = recommendationService.getRecommendationsByJobOfferId(jobOfferId);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Recommendation>> getRecommendationsByStatus(@PathVariable RecommendationStatus status) {
        List<Recommendation> recommendations = recommendationService.getRecommendationsByStatus(status);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/freelance/{freelanceId}/pending")
    public ResponseEntity<List<Recommendation>> getPendingRecommendationsForFreelance(@PathVariable String freelanceId) {
        List<Recommendation> recommendations = recommendationService.getPendingRecommendationsForFreelance(freelanceId);
        return ResponseEntity.ok(recommendations);
    }

    // ==================== Status Operations ====================

    @PostMapping("/{id}/accept")
    public ResponseEntity<Recommendation> acceptRecommendation(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        try {
            String response = payload.getOrDefault("response", "");
            Recommendation updated = recommendationService.acceptRecommendation(id, response);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Recommendation> rejectRecommendation(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        try {
            String response = payload.getOrDefault("response", "");
            Recommendation updated = recommendationService.rejectRecommendation(id, response);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Recommendation> cancelRecommendation(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        try {
            String reason = payload.getOrDefault("reason", "");
            Recommendation updated = recommendationService.cancelRecommendation(id, reason);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== View Tracking ====================

    @PostMapping("/{id}/view")
    public ResponseEntity<Recommendation> incrementViews(@PathVariable Long id) {
        try {
            Recommendation updated = recommendationService.incrementViews(id);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== Reminder Operations ====================

    @PostMapping("/{id}/reminder")
    public ResponseEntity<Recommendation> sendReminder(@PathVariable Long id) {
        try {
            Recommendation updated = recommendationService.sendReminder(id);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/reminders/pending")
    public ResponseEntity<List<Recommendation>> getRecommendationsNeedingReminder() {
        List<Recommendation> recommendations = recommendationService.findRecommendationsNeedingReminder();
        return ResponseEntity.ok(recommendations);
    }

    @PostMapping("/reminders/send-batch")
    public ResponseEntity<Map<String, Object>> sendBatchReminders() {
        List<Recommendation> recommendations = recommendationService.findRecommendationsNeedingReminder();
        
        int remindersSent = 0;
        for (Recommendation recommendation : recommendations) {
            try {
                recommendationService.sendReminder(recommendation.getId());
                remindersSent++;
            } catch (Exception e) {
                // Failed to send reminder
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalFound", recommendations.size());
        result.put("remindersSent", remindersSent);
        
        return ResponseEntity.ok(result);
    }

    // ==================== Expiration Handling ====================

    @PostMapping("/expire-old")
    public ResponseEntity<Map<String, Object>> expireOldRecommendations() {
        List<Recommendation> expired = recommendationService.findAndExpireOldRecommendations();
        
        Map<String, Object> result = new HashMap<>();
        result.put("expiredCount", expired.size());
        result.put("recommendations", expired);
        
        return ResponseEntity.ok(result);
    }

    // ==================== Statistics ====================

    @GetMapping("/stats/count/{status}")
    public ResponseEntity<Map<String, Object>> countByStatus(@PathVariable RecommendationStatus status) {
        long count = recommendationService.countByStatus(status);
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", status);
        result.put("count", count);
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats/freelance/{freelanceId}/{status}")
    public ResponseEntity<Map<String, Object>> countByFreelanceAndStatus(
            @PathVariable String freelanceId,
            @PathVariable RecommendationStatus status) {
        long count = recommendationService.countByFreelanceIdAndStatus(freelanceId, status);
        
        Map<String, Object> result = new HashMap<>();
        result.put("freelanceId", freelanceId);
        result.put("status", status);
        result.put("count", count);
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats/client/{clientId}/{status}")
    public ResponseEntity<Map<String, Object>> countByClientAndStatus(
            @PathVariable String clientId,
            @PathVariable RecommendationStatus status) {
        long count = recommendationService.countByClientIdAndStatus(clientId, status);
        
        Map<String, Object> result = new HashMap<>();
        result.put("clientId", clientId);
        result.put("status", status);
        result.put("count", count);
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Recommendation>> getRecentRecommendations(
            @RequestParam(defaultValue = "7") int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Recommendation> recommendations = recommendationService.getRecentRecommendations(since);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/top-viewed")
    public ResponseEntity<List<Recommendation>> getTopViewedRecommendations() {
        List<Recommendation> recommendations = recommendationService.getTopViewedRecommendations();
        return ResponseEntity.ok(recommendations);
    }

    // ==================== Dashboard Stats ====================

    @GetMapping("/stats/dashboard/{userId}")
    public ResponseEntity<Map<String, Object>> getDashboardStats(@PathVariable String userId) {
        Map<String, Object> stats = new HashMap<>();

        // As client
        long sentTotal = recommendationService.getRecommendationsByClientId(userId).size();
        long sentPending = recommendationService.countByClientIdAndStatus(userId, RecommendationStatus.PENDING);
        long sentAccepted = recommendationService.countByClientIdAndStatus(userId, RecommendationStatus.ACCEPTED);
        long sentRejected = recommendationService.countByClientIdAndStatus(userId, RecommendationStatus.REJECTED);

        Map<String, Object> clientStats = new HashMap<>();
        clientStats.put("total", sentTotal);
        clientStats.put("pending", sentPending);
        clientStats.put("accepted", sentAccepted);
        clientStats.put("rejected", sentRejected);
        clientStats.put("acceptanceRate", sentTotal > 0 ? (double) sentAccepted / sentTotal * 100 : 0);
        stats.put("asClient", clientStats);

        // As freelancer
        long receivedTotal = recommendationService.getRecommendationsByFreelanceId(userId).size();
        long receivedPending = recommendationService.countByFreelanceIdAndStatus(userId, RecommendationStatus.PENDING);
        long receivedAccepted = recommendationService.countByFreelanceIdAndStatus(userId, RecommendationStatus.ACCEPTED);
        long receivedRejected = recommendationService.countByFreelanceIdAndStatus(userId, RecommendationStatus.REJECTED);

        Map<String, Object> freelancerStats = new HashMap<>();
        freelancerStats.put("total", receivedTotal);
        freelancerStats.put("pending", receivedPending);
        freelancerStats.put("accepted", receivedAccepted);
        freelancerStats.put("rejected", receivedRejected);
        freelancerStats.put("acceptanceRate", receivedTotal > 0 ? (double) receivedAccepted / receivedTotal * 100 : 0);
        stats.put("asFreelancer", freelancerStats);

        return ResponseEntity.ok(stats);
    }

    // ==================== Activity Tracking ====================

    @GetMapping("/{id}/activities")
    public ResponseEntity<List<RecommendationActivity>> getActivities(@PathVariable Long id) {
        List<RecommendationActivity> activities = activityService.getActivitiesByRecommendationId(id);
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/activities/type/{activityType}")
    public ResponseEntity<List<RecommendationActivity>> getActivitiesByType(@PathVariable ActivityType activityType) {
        List<RecommendationActivity> activities = activityService.getActivitiesByType(activityType);
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/activities/user/{userId}/{userType}")
    public ResponseEntity<List<RecommendationActivity>> getActivitiesByUser(
            @PathVariable String userId,
            @PathVariable UserType userType) {
        List<RecommendationActivity> activities = activityService.getActivitiesByUser(userId, userType);
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/activities/recent")
    public ResponseEntity<List<RecommendationActivity>> getRecentActivities(
            @RequestParam(defaultValue = "7") int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<RecommendationActivity> activities = activityService.getRecentActivities(since);
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/activities/count/{activityType}")
    public ResponseEntity<Map<String, Object>> countActivitiesByType(@PathVariable ActivityType activityType) {
        long count = activityService.countActivitiesByType(activityType);
        
        Map<String, Object> result = new HashMap<>();
        result.put("activityType", activityType);
        result.put("count", count);
        
        return ResponseEntity.ok(result);
    }

    // ==================== Cleanup ====================

    @DeleteMapping("/activities/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupOldActivities(
            @RequestParam(defaultValue = "90") int days) {
        LocalDateTime before = LocalDateTime.now().minusDays(days);
        activityService.deleteOldActivities(before);
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Old activities deleted");
        result.put("before", before);
        
        return ResponseEntity.ok(result);
    }
}
