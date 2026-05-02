package com.esprit.microservice.evaluation_pi.services;

import com.esprit.microservice.evaluation_pi.entities.EvaluationVote;

public interface IVoteService {
    EvaluationVote addVote(Long evaluationId, String userId, boolean isHelpful);
    void removeVote(Long evaluationId, String userId);
    boolean hasUserVoted(Long evaluationId, String userId);
    long countHelpfulVotes(Long evaluationId);
    long countNotHelpfulVotes(Long evaluationId);
}