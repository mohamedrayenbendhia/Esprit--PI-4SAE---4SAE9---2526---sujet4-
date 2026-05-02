package com.esprit.microservice.evaluation_pi.repositories;

import com.esprit.microservice.evaluation_pi.entities.EvaluationVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaluationVoteRepository extends JpaRepository<EvaluationVote, Long> {
    boolean existsByEvaluationIdAndUserId(Long evaluationId, String userId); // ✅ String
}