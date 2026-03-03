package com.esprit.microservice.evaluation_pi.controller.client;

import com.esprit.microservice.evaluation_pi.entities.Evaluation;
import com.esprit.microservice.evaluation_pi.entities.ReportReason;
import com.esprit.microservice.evaluation_pi.services.EvaluationService;
import com.esprit.microservice.evaluation_pi.services.VoteService;
import com.esprit.microservice.evaluation_pi.services.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/client/evaluations")
@RequiredArgsConstructor
public class ClientEvaluationController {

    private final EvaluationService evaluationService;
    private final VoteService voteService;
    private final BadgeService badgeService;

    // ==================== CRUD ÉVALUATIONS ====================

    @PostMapping("/freelancer/{freelancerEmail}")
    public ResponseEntity<?> evaluateFreelancer(
            @PathVariable String freelancerEmail,
            @RequestBody Evaluation evaluation,
            @RequestParam String clientId,
            @RequestParam(required = false) String projectId) {

        if (freelancerEmail == null || clientId == null) {
            return ResponseEntity.badRequest().body("Email du freelancer et clientId requis");
        }
        if (!freelancerEmail.contains("@")) {
            return ResponseEntity.badRequest().body("Email invalide");
        }

        // ✅ VÉRIFICATION SUPPRIMÉE - On ne vérifie plus le rôle

        if (projectId != null && !projectId.isEmpty() &&
                evaluationService.hasClientEvaluatedProject(projectId, clientId)) {
            return ResponseEntity.badRequest().body("Vous avez déjà évalué ce projet");
        }

        evaluation.setEvaluatedId(freelancerEmail);
        evaluation.setEvaluatorId(clientId);
        evaluation.setProjectId(projectId);

        return ResponseEntity.ok(evaluationService.createEvaluation(evaluation));
    }

    @GetMapping("/client/{clientId}/given")
    public ResponseEntity<List<Evaluation>> getMyGivenEvaluations(@PathVariable String clientId) {
        return ResponseEntity.ok(evaluationService.getEvaluationsByEvaluatorId(clientId));
    }

    @GetMapping("/{evaluationId}")
    public ResponseEntity<Evaluation> getEvaluationById(@PathVariable Long evaluationId) {
        return ResponseEntity.ok(evaluationService.getEvaluationById(evaluationId));
    }

    @DeleteMapping("/{evaluationId}")
    public ResponseEntity<?> deleteMyEvaluation(
            @PathVariable Long evaluationId,
            @RequestParam String clientId) {

        Evaluation evaluation = evaluationService.getEvaluationById(evaluationId);
        if (!evaluation.getEvaluatorId().equals(clientId)) {
            return ResponseEntity.badRequest().body("Vous ne pouvez supprimer que vos propres évaluations");
        }
        evaluationService.deleteEvaluation(evaluationId);
        return ResponseEntity.noContent().build();
    }

    // ==================== CONSULTATION FREELANCERS ====================

    @GetMapping("/freelancer/{freelancerEmail}")
    public ResponseEntity<List<Evaluation>> getFreelancerEvaluations(@PathVariable String freelancerEmail) {
        // ✅ VÉRIFICATION SUPPRIMÉE
        return ResponseEntity.ok(evaluationService.getEvaluationsByEvaluatedId(freelancerEmail));
    }

    @GetMapping("/freelancers/all/overview")
    public ResponseEntity<Map<String, Object>> getAllFreelancersOverview() {
        List<Evaluation> allEvaluations = evaluationService.getAllEvaluations();

        Map<String, List<Evaluation>> byFreelancer = allEvaluations.stream()
                .collect(Collectors.groupingBy(Evaluation::getEvaluatedId));

        Map<String, Object> result = new HashMap<>();

        byFreelancer.forEach((email, evals) -> {
            Map<String, Object> data = new HashMap<>();
            data.put("email", email);
            data.put("totalEvaluations", evals.size());

            double avg = evals.stream()
                    .mapToDouble(Evaluation::getRatingGlobal)
                    .average().orElse(0.0);
            data.put("averageRating", Math.round(avg * 10.0) / 10.0);

            data.put("badges", badgeService.getUserBadges(email));

            data.put("recentEvaluations", evals.stream()
                    .sorted((e1, e2) -> e2.getCreatedAt().compareTo(e1.getCreatedAt()))
                    .limit(3)
                    .collect(Collectors.toList()));

            result.put(email, data);
        });

        return ResponseEntity.ok(result);
    }

    @GetMapping("/freelancer/{freelancerEmail}/details")
    public ResponseEntity<Map<String, Object>> getFreelancerDetails(@PathVariable String freelancerEmail) {
        // ✅ VÉRIFICATION SUPPRIMÉE

        List<Evaluation> evaluations = evaluationService.getEvaluationsByEvaluatedId(freelancerEmail);

        Map<String, Object> result = new HashMap<>();
        result.put("freelancerEmail", freelancerEmail);
        result.put("totalEvaluations", evaluations.size());

        double avgRating = evaluations.stream()
                .mapToDouble(Evaluation::getRatingGlobal)
                .average().orElse(0.0);
        result.put("averageRating", Math.round(avgRating * 10.0) / 10.0);

        Map<String, Long> distribution = new HashMap<>();
        distribution.put("5stars", evaluations.stream().filter(e -> e.getRatingGlobal() >= 4.5).count());
        distribution.put("4stars", evaluations.stream().filter(e -> e.getRatingGlobal() >= 3.5 && e.getRatingGlobal() < 4.5).count());
        distribution.put("3stars", evaluations.stream().filter(e -> e.getRatingGlobal() >= 2.5 && e.getRatingGlobal() < 3.5).count());
        distribution.put("2stars", evaluations.stream().filter(e -> e.getRatingGlobal() >= 1.5 && e.getRatingGlobal() < 2.5).count());
        distribution.put("1star", evaluations.stream().filter(e -> e.getRatingGlobal() < 1.5).count());
        result.put("distribution", distribution);

        result.put("badges", badgeService.getUserBadges(freelancerEmail));
        result.put("evaluations", evaluations);

        return ResponseEntity.ok(result);
    }

    // ==================== INTERACTIONS ====================

    @PostMapping("/evaluation/{evaluationId}/vote")
    public ResponseEntity<?> voteEvaluation(
            @PathVariable Long evaluationId,
            @RequestParam String clientId,
            @RequestParam boolean helpful) {

        if (voteService.hasUserVoted(evaluationId, clientId)) {
            return ResponseEntity.badRequest().body("Vous avez déjà voté");
        }
        return ResponseEntity.ok(voteService.addVote(evaluationId, clientId, helpful));
    }

    @PostMapping("/evaluation/{evaluationId}/report")
    public ResponseEntity<Evaluation> reportEvaluation(
            @PathVariable Long evaluationId,
            @RequestParam ReportReason reason) {
        return ResponseEntity.ok(evaluationService.reportEvaluation(evaluationId, reason));
    }
}