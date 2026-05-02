package com.esprit.microservice.evaluation_pi.services;

import com.esprit.microservice.evaluation_pi.entities.Evaluation;
import com.esprit.microservice.evaluation_pi.entities.EvaluationVote;
import com.esprit.microservice.evaluation_pi.repositories.EvaluationRepository;
import com.esprit.microservice.evaluation_pi.repositories.EvaluationVoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    @Mock
    private EvaluationVoteRepository voteRepository;

    @Mock
    private EvaluationRepository evaluationRepository;

    @InjectMocks
    private VoteService voteService;

    @Test
    void testAddVote_Helpful_ShouldIncrementCount() {
        // Given
        Long evalId = 1L;
        String userId = "user123";

        Evaluation evaluation = new Evaluation();
        evaluation.setId(evalId);
        evaluation.setHelpfulCount(10);
        evaluation.setNotHelpfulCount(5);

        when(voteRepository.existsByEvaluationIdAndUserId(evalId, userId)).thenReturn(false);
        when(evaluationRepository.findById(evalId)).thenReturn(Optional.of(evaluation));
        when(voteRepository.save(any(EvaluationVote.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        EvaluationVote result = voteService.addVote(evalId, userId, true);

        // Then
        assertNotNull(result);
        assertEquals(11, evaluation.getHelpfulCount()); // Vérifie l'incrémentation
        assertEquals(5, evaluation.getNotHelpfulCount()); // Reste inchangé
        verify(evaluationRepository, times(1)).save(evaluation);
        verify(voteRepository, times(1)).save(any(EvaluationVote.class));
    }



    @Test
    void testRemoveVote_NotHelpful_ShouldDecrementCount() {
        // Given
        Long evalId = 1L;
        String userId = "user123";

        Evaluation evaluation = new Evaluation();
        evaluation.setId(evalId);
        evaluation.setNotHelpfulCount(5);

        EvaluationVote vote = new EvaluationVote();
        vote.setIsHelpful(false);

        when(evaluationRepository.findById(evalId)).thenReturn(Optional.of(evaluation));
        // Ici, ton service cherche le vote par evaluationId dans findById (attention à la logique du code source)
        when(voteRepository.findById(evalId)).thenReturn(Optional.of(vote));

        // When
        voteService.removeVote(evalId, userId);

        // Then
        assertEquals(4, evaluation.getNotHelpfulCount()); // 5 - 1 = 4
        verify(voteRepository, times(1)).delete(vote);
    }

    @Test
    void testAddVote_AlreadyVoted_ShouldThrowException() {
        // Given
        when(voteRepository.existsByEvaluationIdAndUserId(1L, "user1")).thenReturn(true);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            voteService.addVote(1L, "user1", true);
        });
    }
}