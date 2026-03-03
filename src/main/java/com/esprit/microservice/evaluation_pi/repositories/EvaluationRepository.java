package com.esprit.microservice.evaluation_pi.repositories;

import com.esprit.microservice.evaluation_pi.entities.Evaluation;
import com.esprit.microservice.evaluation_pi.entities.EvaluationStatus;
import com.esprit.microservice.evaluation_pi.entities.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    List<Evaluation> findByEvaluatedId(String evaluatedId);
    List<Evaluation> findByEvaluatorId(String evaluatorId);
    List<Evaluation> findByStatus(EvaluationStatus status);
    List<Evaluation> findByProjectId(String projectId);
    List<Evaluation> findByReportStatus(ReportStatus reportStatus);
    boolean existsByProjectIdAndEvaluatorId(String projectId, String evaluatorId);

    @Query("SELECT AVG(e.ratingGlobal) FROM Evaluation e WHERE e.evaluatedId = :evaluatedId")
    Double findAverageRatingByEvaluatedId(@Param("evaluatedId") String evaluatedId);

    @Query("SELECT COUNT(e) FROM Evaluation e WHERE e.evaluatedId = :evaluatedId")
    Long countByEvaluatedId(@Param("evaluatedId") String evaluatedId);
}