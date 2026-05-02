package com.esprit.microservice.evaluation_pi.controller.freelancer;

import com.esprit.microservice.evaluation_pi.entities.Badge;
import com.esprit.microservice.evaluation_pi.entities.UserBadge;
import com.esprit.microservice.evaluation_pi.services.BadgeService;
import com.esprit.microservice.evaluation_pi.services.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/freelancer/badges")
@RequiredArgsConstructor
public class FreelancerBadgeController {

    private final BadgeService badgeService;
    private final EvaluationService evaluationService;

    // Tous les badges disponibles
    @GetMapping
    public ResponseEntity<List<Badge>> getAllBadges() {
        return ResponseEntity.ok(badgeService.getAllBadges());
    }

    // Badges d'un freelancer par email
    @GetMapping("/my-badges/{identifier}")
    public ResponseEntity<List<UserBadge>> getMyBadges(@PathVariable String identifier) {
        System.out.println("Recherche badges pour: " + identifier);
        List<UserBadge> myBadges = badgeService.getUserBadges(identifier);
        System.out.println("Badges trouvés: " + myBadges.size());
        return ResponseEntity.ok(myBadges);
    }

    // Critères d'un badge
    @GetMapping("/badge/{badgeId}/criteria")
    public ResponseEntity<Map<String, Object>> getBadgeCriteria(@PathVariable Long badgeId) {
        Badge badge = badgeService.getBadgeById(badgeId);
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("badgeId", badge.getId());
        criteria.put("name", badge.getName());
        criteria.put("description", badge.getDescription());
        criteria.put("minScore", badge.getMinScore());
        criteria.put("minProjects", badge.getMinProjects());
        criteria.put("icon", badge.getIcon());
        criteria.put("createdAt", badge.getCreatedAt());
        return ResponseEntity.ok(criteria);
    }

    // Progression d'un badge pour un freelancer
    @GetMapping("/badge/{badgeId}/progress/{identifier}")
    public ResponseEntity<Map<String, Object>> getBadgeProgress(
            @PathVariable Long badgeId,
            @PathVariable String identifier) {

        Badge badge = badgeService.getBadgeById(badgeId);
        Double avgScore = evaluationService.calculateUserAverageRating(identifier);
        Long totalEvaluations = evaluationService.countUserEvaluations(identifier);

        Map<String, Object> progress = new HashMap<>();
        progress.put("badge", badge);
        progress.put("currentScore", avgScore != null ? Math.round(avgScore * 100.0) / 100.0 : 0.0);
        progress.put("requiredScore", badge.getMinScore());
        progress.put("currentProjects", totalEvaluations != null ? totalEvaluations : 0L);
        progress.put("requiredProjects", badge.getMinProjects());

        double scoreProgress = avgScore != null ?
                Math.min(100, (avgScore / badge.getMinScore()) * 100) : 0;
        double projectsProgress = totalEvaluations != null ?
                Math.min(100, ((double) totalEvaluations / badge.getMinProjects()) * 100) : 0;

        progress.put("scoreProgress", Math.min(100, Math.round(scoreProgress * 100.0) / 100.0));
        progress.put("projectsProgress", Math.min(100, Math.round(projectsProgress * 100.0) / 100.0));

        boolean hasBadge = badgeService.getUserBadges(identifier).stream()
                .anyMatch(ub -> ub.getBadge().getId().equals(badge.getId()));
        progress.put("isObtained", hasBadge);

        double overallProgress = (scoreProgress + projectsProgress) / 2;
        progress.put("overallProgress", Math.min(100, Math.round(overallProgress * 100.0) / 100.0));

        return ResponseEntity.ok(progress);
    }

    // Tous les badges avec progression
    @GetMapping("/all-with-progress/{identifier}")
    public ResponseEntity<List<Map<String, Object>>> getAllBadgesWithProgress(
            @PathVariable String identifier) {

        List<Badge> allBadges = badgeService.getAllBadges();
        Double avgScore = evaluationService.calculateUserAverageRating(identifier);
        Long totalEvaluations = evaluationService.countUserEvaluations(identifier);

        List<Map<String, Object>> badgesWithProgress = allBadges.stream().map(badge -> {
            Map<String, Object> badgeProgress = new HashMap<>();
            badgeProgress.put("badge", badge);

            double scoreProgress = avgScore != null ?
                    Math.min(100, (avgScore / badge.getMinScore()) * 100) : 0;
            double projectsProgress = totalEvaluations != null ?
                    Math.min(100, ((double) totalEvaluations / badge.getMinProjects()) * 100) : 0;

            badgeProgress.put("scoreProgress", Math.min(100, Math.round(scoreProgress * 100.0) / 100.0));
            badgeProgress.put("projectsProgress", Math.min(100, Math.round(projectsProgress * 100.0) / 100.0));

            boolean hasBadge = badgeService.getUserBadges(identifier).stream()
                    .anyMatch(ub -> ub.getBadge().getId().equals(badge.getId()));
            badgeProgress.put("isObtained", hasBadge);

            double overallProgress = (scoreProgress + projectsProgress) / 2;
            badgeProgress.put("overallProgress", Math.min(100, Math.round(overallProgress * 100.0) / 100.0));

            return badgeProgress;
        }).toList();

        return ResponseEntity.ok(badgesWithProgress);
    }

    // ✅ NOUVEAU — Vérifier et attribuer les badges automatiquement
    @PostMapping("/check-and-assign")
    public ResponseEntity<Map<String, Object>> checkAndAssignBadges(
            @RequestBody Map<String, Object> request) {

        String userId = (String) request.get("userId");
        Double averageScore = Double.valueOf(request.get("averageScore").toString());
        Long totalProjects = Long.valueOf(request.get("totalProjects").toString());

        System.out.println("=== Check and assign pour: " + userId);
        System.out.println("Score: " + averageScore + ", Projets: " + totalProjects);

        // Attribution automatique des badges éligibles
        badgeService.checkAndAssignBadges(userId, averageScore, totalProjects);

        // Retourner les badges mis à jour
        List<UserBadge> updatedBadges = badgeService.getUserBadges(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("badges", updatedBadges);
        response.put("totalBadges", updatedBadges.size());

        return ResponseEntity.ok(response);
    }
}