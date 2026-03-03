package com.esprit.microservice.evaluation_pi.services;

import com.esprit.microservice.evaluation_pi.controller.freelancer.FreelancerEvaluationController;
import com.esprit.microservice.evaluation_pi.entities.*;
import com.esprit.microservice.evaluation_pi.repositories.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EvaluationService implements IEvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final EvaluationVoteRepository voteRepository;
    private final BadgeService badgeService;
    // @Lazy pour éviter la dépendance circulaire Spring
    private final FreelancerEvaluationController freelancerController;

    public EvaluationService(
            EvaluationRepository evaluationRepository,
            EvaluationVoteRepository voteRepository,
            BadgeService badgeService,
            @Lazy FreelancerEvaluationController freelancerController) {
        this.evaluationRepository = evaluationRepository;
        this.voteRepository       = voteRepository;
        this.badgeService         = badgeService;
        this.freelancerController = freelancerController;
    }

    // =========================================================
    // CRUD
    // =========================================================

    @Override
    public Evaluation createEvaluation(Evaluation evaluation) {
        evaluation.setCreatedAt(LocalDateTime.now());
        evaluation.setUpdatedAt(LocalDateTime.now());
        evaluation.setStatus(EvaluationStatus.PUBLISHED);
        evaluation.setHelpfulCount(0);
        evaluation.setNotHelpfulCount(0);

        if (evaluation.getProjectId() == null || evaluation.getProjectId().isEmpty()) {
            evaluation.setProjectId("TEMP_" + UUID.randomUUID());
        }

        evaluation.setRatingGlobal(calculateWeightedScore(evaluation));

        Evaluation saved = evaluationRepository.save(evaluation);
        checkAndAssignBadgesForUser(saved.getEvaluatedId());

        // ✅ Push SSE asynchrone — évite le conflit d'écriture concurrent sur le thread HTTP
        pushStatsAsync(saved.getEvaluatedId());

        return saved;
    }

    @Override
    public Evaluation updateEvaluation(Long id, Evaluation evaluation) {
        Evaluation existing = getEvaluationById(id);
        evaluation.setId(id);
        evaluation.setCreatedAt(existing.getCreatedAt());
        evaluation.setUpdatedAt(LocalDateTime.now());

        if (evaluation.getProjectId() == null || evaluation.getProjectId().isEmpty()) {
            evaluation.setProjectId(existing.getProjectId());
        }

        evaluation.setRatingGlobal(calculateWeightedScore(evaluation));

        Evaluation updated = evaluationRepository.save(evaluation);
        checkAndAssignBadgesForUser(updated.getEvaluatedId());

        // ✅ Push SSE asynchrone
        pushStatsAsync(updated.getEvaluatedId());

        return updated;
    }

    @Override
    public void deleteEvaluation(Long id) {
        Evaluation evaluation = getEvaluationById(id);
        String evaluatedId = evaluation.getEvaluatedId();
        evaluationRepository.deleteById(id);
        checkAndAssignBadgesForUser(evaluatedId);

        // ✅ Push SSE asynchrone
        pushStatsAsync(evaluatedId);
    }

    // =========================================================
    // READ
    // =========================================================

    @Override
    public List<Evaluation> getAllEvaluations() {
        return evaluationRepository.findAll();
    }

    @Override
    public Evaluation getEvaluationById(Long id) {
        return evaluationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evaluation not found with id: " + id));
    }

    @Override
    public List<Evaluation> getEvaluationsByEvaluatedId(String evaluatedId) {
        return evaluationRepository.findByEvaluatedId(evaluatedId);
    }

    public List<Evaluation> getEvaluationsByEvaluatorId(String evaluatorId) {
        return evaluationRepository.findByEvaluatorId(evaluatorId);
    }

    public List<Evaluation> getReportedEvaluations() {
        return evaluationRepository.findByReportStatus(ReportStatus.PENDING);
    }

    public boolean hasClientEvaluatedProject(String projectId, String clientId) {
        if (projectId != null && projectId.startsWith("TEMP_")) return false;
        return evaluationRepository.existsByProjectIdAndEvaluatorId(projectId, clientId);
    }

    // =========================================================
    // ACTIONS
    // =========================================================

    @Override
    @Transactional
    public Evaluation respondToEvaluation(Long evaluationId, String response) {
        Evaluation evaluation = getEvaluationById(evaluationId);
        evaluation.setResponseText(response);
        evaluation.setResponseDate(LocalDateTime.now());
        return evaluationRepository.save(evaluation);
    }

    @Override
    @Transactional
    public Evaluation reportEvaluation(Long evaluationId, ReportReason reason) {
        Evaluation evaluation = getEvaluationById(evaluationId);
        evaluation.setStatus(EvaluationStatus.REPORTED);
        evaluation.setReportReason(reason);
        evaluation.setReportStatus(ReportStatus.PENDING);
        return evaluationRepository.save(evaluation);
    }

    @Override
    @Transactional
    public Evaluation moderateReport(Long evaluationId, ReportStatus decision) {
        Evaluation evaluation = getEvaluationById(evaluationId);
        evaluation.setReportStatus(decision);

        if (decision == ReportStatus.APPROVED) {
            evaluation.setStatus(EvaluationStatus.HIDDEN);
        } else if (decision == ReportStatus.REJECTED) {
            evaluation.setStatus(EvaluationStatus.PUBLISHED);
        }

        return evaluationRepository.save(evaluation);
    }

    @Override
    @Transactional
    public Evaluation voteEvaluation(Long evaluationId, String userId, boolean isHelpful) {
        if (voteRepository.existsByEvaluationIdAndUserId(evaluationId, userId)) {
            throw new RuntimeException("User already voted for this evaluation");
        }

        Evaluation evaluation = getEvaluationById(evaluationId);

        EvaluationVote vote = new EvaluationVote();
        vote.setEvaluation(evaluation);
        vote.setUserId(userId);
        vote.setIsHelpful(isHelpful);
        voteRepository.save(vote);

        if (isHelpful) {
            evaluation.setHelpfulCount(evaluation.getHelpfulCount() + 1);
        } else {
            evaluation.setNotHelpfulCount(evaluation.getNotHelpfulCount() + 1);
        }

        Evaluation saved = evaluationRepository.save(evaluation);

        // ✅ Push SSE asynchrone
        pushStatsAsync(saved.getEvaluatedId());

        return saved;
    }

    // =========================================================
    // PUSH SSE ASYNCHRONE
    // =========================================================

    /**
     * Exécuté sur le thread pool "ssePushExecutor" (AsyncConfig).
     *
     * Le délai 300ms garantit que la réponse HTTP du POST est
     * complètement envoyée avant d'écrire dans le flux SSE,
     * ce qui évite le "concurrent write" → 500 Internal Server Error.
     */
    @Async("ssePushExecutor")
    public void pushStatsAsync(String freelancerEmail) {
        try {
            Thread.sleep(300);
            freelancerController.pushStatsUpdate(freelancerEmail);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Ne pas faire échouer l'opération métier si le push SSE plante
            System.err.println("[SSE] Erreur push stats pour " + freelancerEmail + " : " + e.getMessage());
        }
    }

    // =========================================================
    // CALCULS
    // =========================================================

    @Override
    public Double calculateWeightedScore(Evaluation evaluation) {
        // Protection nulls — tous les sous-scores doivent être présents
        if (evaluation.getQualityScore() == null
                || evaluation.getDeadlineScore() == null
                || evaluation.getCommunicationScore() == null
                || evaluation.getProfessionalismScore() == null) {
            return 0.0;
        }
        return (evaluation.getQualityScore()        * 0.40)
                + (evaluation.getDeadlineScore()        * 0.25)
                + (evaluation.getCommunicationScore()   * 0.20)
                + (evaluation.getProfessionalismScore() * 0.15);
    }

    public Double calculateUserAverageRating(String evaluatedId) {
        if (evaluatedId == null) return 0.0;
        return evaluationRepository.findAverageRatingByEvaluatedId(evaluatedId);
    }

    public Long countUserEvaluations(String evaluatedId) {
        if (evaluatedId == null) return 0L;
        return evaluationRepository.countByEvaluatedId(evaluatedId);
    }

    private void checkAndAssignBadgesForUser(String evaluatedId) {
        System.out.println("========== DÉBUT VÉRIFICATION BADGES ==========");
        System.out.println(" Utilisateur: " + evaluatedId);

        Double avgScore = evaluationRepository.findAverageRatingByEvaluatedId(evaluatedId);
        Long count = evaluationRepository.countByEvaluatedId(evaluatedId);

        System.out.println(" Score moyen trouvé: " + avgScore);
        System.out.println(" Nombre d'évaluations: " + count);

        if (avgScore != null) {
            System.out.println(" Appel de badgeService.checkAndAssignBadges()");
            badgeService.checkAndAssignBadges(evaluatedId, avgScore, count);
        } else {
            System.out.println(" avgScore est NULL - aucune vérification effectuée");
        }

        System.out.println("========== FIN VÉRIFICATION BADGES ==========");
    }
}