package com.esprit.microservice.evaluation_pi.services;

import com.esprit.microservice.evaluation_pi.entities.Evaluation;
import com.esprit.microservice.evaluation_pi.entities.EvaluationStatus;
import com.esprit.microservice.evaluation_pi.repositories.EvaluationRepository;
import com.esprit.microservice.evaluation_pi.repositories.EvaluationVoteRepository;
import com.esprit.microservice.evaluation_pi.controller.freelancer.FreelancerEvaluationController;

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
class EvaluationServiceTest {

    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private EvaluationVoteRepository voteRepository;
    @Mock
    private BadgeService badgeService;
    @Mock
    private FreelancerEvaluationController freelancerController;

    @InjectMocks
    private EvaluationService evaluationService;

    @Test
    void testGetEvaluationById_NotFound() {
        // On simule que la base de données ne trouve rien
        when(evaluationRepository.findById(999L)).thenReturn(Optional.empty());

        // On vérifie que l'exception est bien lancée
        assertThrows(RuntimeException.class, () -> {
            evaluationService.getEvaluationById(999L);
        });
    }

    @Test
    void testCalculateWeightedScore() {
        // Given
        Evaluation eval = new Evaluation();
        eval.setQualityScore(5.0);        // 5.0 * 0.40 = 2.0
        eval.setDeadlineScore(4.0);       // 4.0 * 0.25 = 1.0
        eval.setCommunicationScore(3.0);  // 3.0 * 0.20 = 0.6
        eval.setProfessionalismScore(2.0);// 2.0 * 0.15 = 0.3
        // Total attendu = 3.9

        // When
        Double result = evaluationService.calculateWeightedScore(eval);

        // Then
        assertEquals(3.9, result, 0.001); // 0.001 est la marge d'erreur autorisée pour les Double
    }

    @Test
    void testCreateEvaluation_ShouldSetDefaultValues() {
        // Given
        Evaluation eval = new Evaluation();
        eval.setEvaluatedId("freelancer@test.com");
        eval.setQualityScore(4.0);
        eval.setDeadlineScore(4.0);
        eval.setCommunicationScore(4.0);
        eval.setProfessionalismScore(4.0);

        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Evaluation result = evaluationService.createEvaluation(eval);

        // Then
        assertNotNull(result);
        assertEquals(EvaluationStatus.PUBLISHED, result.getStatus());
        assertNotNull(result.getCreatedAt());
        assertTrue(result.getProjectId().startsWith("TEMP_") || result.getProjectId() != null);
        verify(evaluationRepository).save(any(Evaluation.class));
    }

    @Test
    void testVoteEvaluation_AlreadyVoted_ShouldThrowException() {
        // Given
        Long evalId = 1L;
        String userId = "user1";
        when(voteRepository.existsByEvaluationIdAndUserId(evalId, userId)).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            evaluationService.voteEvaluation(evalId, userId, true);
        });

        assertEquals("User already voted for this evaluation", exception.getMessage());
        // On vérifie que save n'a jamais été appelé car l'erreur a stoppé l'exécution
        verify(evaluationRepository, never()).save(any());
    }



}