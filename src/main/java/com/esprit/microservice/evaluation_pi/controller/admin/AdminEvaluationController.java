package com.esprit.microservice.evaluation_pi.controller.admin;

import com.esprit.microservice.evaluation_pi.entities.Badge;
import com.esprit.microservice.evaluation_pi.entities.Evaluation;
import com.esprit.microservice.evaluation_pi.entities.ReportStatus;
import com.esprit.microservice.evaluation_pi.services.BadgeService;
import com.esprit.microservice.evaluation_pi.services.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/evaluations")
@RequiredArgsConstructor
public class AdminEvaluationController {

    private final EvaluationService evaluationService;
    private final BadgeService badgeService;

    //  GESTION DES BADGES

    //  Liste tous les badges
    @GetMapping("/badges")
    public ResponseEntity<List<Badge>> getAllBadges() {
        List<Badge> badges = badgeService.getAllBadges();
        return ResponseEntity.ok(badges);
    }

    // Détail d'un badge
    @GetMapping("/badges/{badgeId}")
    public ResponseEntity<Badge> getBadgeById(@PathVariable Long badgeId) {
        Badge badge = badgeService.getBadgeById(badgeId);
        return ResponseEntity.ok(badge);
    }

    @PostMapping("/badges")
    public ResponseEntity<Badge> createBadge(@RequestBody Badge badge) {
        return ResponseEntity.ok(badgeService.createBadge(badge));
    }

    @PutMapping("/badges/{badgeId}")
    public ResponseEntity<Badge> updateBadge(
            @PathVariable Long badgeId,
            @RequestBody Badge badge) {
        return ResponseEntity.ok(badgeService.updateBadge(badgeId, badge));
    }

    @DeleteMapping("/badges/{badgeId}")
    public ResponseEntity<Void> deleteBadge(
            @PathVariable Long badgeId) {
        badgeService.deleteBadge(badgeId);
        return ResponseEntity.noContent().build();
    }

    //  MODÉRATION DES ÉVALUATIONS
    @GetMapping("/reported")
    public ResponseEntity<List<Evaluation>> getReportedEvaluations() {
        return ResponseEntity.ok(evaluationService.getReportedEvaluations());
    }

    @PutMapping("/evaluation/{evaluationId}/moderate")
    public ResponseEntity<Evaluation> moderateEvaluation(
            @PathVariable Long evaluationId,
            @RequestParam ReportStatus decision) {
        return ResponseEntity.ok(evaluationService.moderateReport(evaluationId, decision));
    }

    //  GESTION DES ÉVALUATIONS
    @GetMapping("/all")
    public ResponseEntity<List<Evaluation>> getAllEvaluations() {
        return ResponseEntity.ok(evaluationService.getAllEvaluations());
    }

    @DeleteMapping("/evaluation/{evaluationId}")
    public ResponseEntity<Void> deleteAnyEvaluation(
            @PathVariable Long evaluationId) {
        evaluationService.deleteEvaluation(evaluationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}/received")
    public ResponseEntity<List<Evaluation>> getUserReceivedEvaluations(
            @PathVariable String userId) {
        return ResponseEntity.ok(evaluationService.getEvaluationsByEvaluatedId(userId));
    }

    @GetMapping("/user/{userId}/given")
    public ResponseEntity<List<Evaluation>> getUserGivenEvaluations(
            @PathVariable String userId) {
        return ResponseEntity.ok(evaluationService.getEvaluationsByEvaluatorId(userId));
    }

    @GetMapping("/with-responses")
    public ResponseEntity<List<Evaluation>> getEvaluationsWithResponses() {
        List<Evaluation> allEvaluations = evaluationService.getAllEvaluations();
        List<Evaluation> withResponses = allEvaluations.stream()
                .filter(e -> e.getResponseText() != null && !e.getResponseText().isEmpty())
                .collect(Collectors.toList());
        return ResponseEntity.ok(withResponses);
    }
}