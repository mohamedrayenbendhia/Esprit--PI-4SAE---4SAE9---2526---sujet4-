package com.esprit.microservice.evaluation_pi.services;

import com.esprit.microservice.evaluation_pi.entities.Evaluation;
import com.esprit.microservice.evaluation_pi.entities.ReportReason;
import com.esprit.microservice.evaluation_pi.entities.ReportStatus;
import java.util.List;

public interface IEvaluationService {
    Evaluation createEvaluation(Evaluation evaluation);
    Evaluation updateEvaluation(Long id, Evaluation evaluation);
    void deleteEvaluation(Long id);
    List<Evaluation> getAllEvaluations();
    Evaluation getEvaluationById(Long id);
    List<Evaluation> getEvaluationsByEvaluatedId(String evaluatedId); // ✅ String

    Evaluation respondToEvaluation(Long evaluationId, String response);
    Evaluation reportEvaluation(Long evaluationId, ReportReason reason);
    Evaluation moderateReport(Long evaluationId, ReportStatus decision);
    Evaluation voteEvaluation(Long evaluationId, String userId, boolean isHelpful); // ✅ String
    Double calculateWeightedScore(Evaluation evaluation);
}