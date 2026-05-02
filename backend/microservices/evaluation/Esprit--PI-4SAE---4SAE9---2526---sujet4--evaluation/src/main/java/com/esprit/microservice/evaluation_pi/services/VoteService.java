package com.esprit.microservice.evaluation_pi.services;

import com.esprit.microservice.evaluation_pi.entities.Evaluation;
import com.esprit.microservice.evaluation_pi.entities.EvaluationVote;
import com.esprit.microservice.evaluation_pi.repositories.EvaluationRepository;
import com.esprit.microservice.evaluation_pi.repositories.EvaluationVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VoteService implements IVoteService {

    private final EvaluationVoteRepository voteRepository;
    private final EvaluationRepository evaluationRepository;

    @Override
    @Transactional
    public EvaluationVote addVote(Long evaluationId, String userId, boolean isHelpful) {
        if (voteRepository.existsByEvaluationIdAndUserId(evaluationId, userId)) {
            throw new RuntimeException("User already voted for this evaluation");
        }

        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new RuntimeException("Evaluation not found"));

        EvaluationVote vote = new EvaluationVote();
        vote.setEvaluation(evaluation);
        vote.setUserId(userId);
        vote.setIsHelpful(isHelpful);

        if (isHelpful) {
            evaluation.setHelpfulCount(evaluation.getHelpfulCount() + 1);
        } else {
            evaluation.setNotHelpfulCount(evaluation.getNotHelpfulCount() + 1);
        }

        evaluationRepository.save(evaluation);
        return voteRepository.save(vote);
    }

    @Override
    @Transactional
    public void removeVote(Long evaluationId, String userId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new RuntimeException("Evaluation not found"));

        EvaluationVote vote = voteRepository.findById(evaluationId)
                .orElseThrow(() -> new RuntimeException("Vote not found"));

        if (vote.getIsHelpful()) {
            evaluation.setHelpfulCount(evaluation.getHelpfulCount() - 1);
        } else {
            evaluation.setNotHelpfulCount(evaluation.getNotHelpfulCount() - 1);
        }

        evaluationRepository.save(evaluation);
        voteRepository.delete(vote);
    }

    @Override
    public boolean hasUserVoted(Long evaluationId, String userId) {
        return voteRepository.existsByEvaluationIdAndUserId(evaluationId, userId);
    }

    @Override
    public long countHelpfulVotes(Long evaluationId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new RuntimeException("Evaluation not found"));
        return evaluation.getHelpfulCount();
    }

    @Override
    public long countNotHelpfulVotes(Long evaluationId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new RuntimeException("Evaluation not found"));
        return evaluation.getNotHelpfulCount();
    }
}