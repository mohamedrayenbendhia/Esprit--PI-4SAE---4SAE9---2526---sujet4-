package com.microservice.module_certification.services;

import com.microservice.module_certification.dto.SubmitTestRequest;
import com.microservice.module_certification.dto.UserTestResultResponse;
import com.microservice.module_certification.entities.*;
import com.microservice.module_certification.exceptions.DuplicateResourceException;
import com.microservice.module_certification.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserTestResultServiceTest {

    @Mock private UserTestResultRepository userTestResultRepository;
    @Mock private UserAnswerRepository userAnswerRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private CertificationRepository certificationRepository;
    @Mock private TestRepository testRepository;

    @InjectMocks
    private UserTestResultService userTestResultService;

    private com.microservice.module_certification.entities.Test mockTest;
    private SubmitTestRequest request;

    @BeforeEach
    void setUp() {
        // 1. Initialisation du Test Entity
        mockTest = com.microservice.module_certification.entities.Test.builder()
                .id(1L)
                .title("Java Basics")
                .passingScore(50)
                .build();

        // 2. Initialisation de la Request
        request = new SubmitTestRequest();
        request.setUserId("user-123");
        request.setUserSkillId(10L);
        request.setAnswers(List.of("A", "B")); // 2 réponses
    }

    @Test
    @DisplayName("Succès : Devrait valider le test et créer une certification quand le score est suffisant")
    void submitTest_Success_ShouldGenerateCertification() {
        // Simulation questions (2 questions)
        Question q1 = Question.builder().id(1L).correctAnswer("A").questionText("Q1").build();
        Question q2 = Question.builder().id(2L).correctAnswer("B").questionText("Q2").build();

        when(testRepository.findBySkillId(10L)).thenReturn(Optional.of(mockTest));
        when(userTestResultRepository.existsByUserIdAndTestIdAndIsPassed(anyString(), anyLong(), anyBoolean())).thenReturn(false);
        when(userTestResultRepository.findTopByUserIdAndTestIdOrderByLastAttemptAtDesc(anyString(), anyLong())).thenReturn(Optional.empty());
        when(questionRepository.findByTestId(1L)).thenReturn(List.of(q1, q2));
        when(userTestResultRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Action
        UserTestResultResponse response = userTestResultService.submitTest(request);

        // Vérifications
        assertNotNull(response);
        assertEquals(100, response.getScore());
        assertTrue(response.isPassed());
        verify(certificationRepository, times(1)).save(any(Certification.class));
        verify(userAnswerRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Échec : Devrait lancer une exception si déjà certifié")
    void submitTest_Fail_AlreadyPassed() {
        when(testRepository.findBySkillId(10L)).thenReturn(Optional.of(mockTest));
        when(userTestResultRepository.existsByUserIdAndTestIdAndIsPassed("user-123", 1L, true)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userTestResultService.submitTest(request));
    }

    @Test
    @DisplayName("Échec : Devrait lancer une exception si le cooldown de 2 minutes n'est pas respecté")
    void submitTest_Fail_CooldownActive() {
        when(testRepository.findBySkillId(10L)).thenReturn(Optional.of(mockTest));

        // Simuler un échec il y a 30 secondes
        UserTestResult lastAttempt = new UserTestResult();
        lastAttempt.setPassed(false);
        lastAttempt.setLastAttemptAt(LocalDateTime.now().minusSeconds(30));

        when(userTestResultRepository.findTopByUserIdAndTestIdOrderByLastAttemptAtDesc(anyString(), anyLong()))
                .thenReturn(Optional.of(lastAttempt));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userTestResultService.submitTest(request));
        assertTrue(exception.getMessage().contains("COOLDOWN"));
    }

    @Test
    @DisplayName("Score : Devrait échouer le test si le score est insuffisant")
    void submitTest_ShouldNotPass_WhenScoreLow() {
        Question q1 = Question.builder().id(1L).correctAnswer("A").questionText("Q1").build();
        Question q2 = Question.builder().id(2L).correctAnswer("A").questionText("Q2").build(); // Réponse correcte est A

        request.setAnswers(List.of("A", "B")); // Une seule bonne réponse sur 2 (50%)
        mockTest.setPassingScore(80); // Il faut 80% pour réussir

        when(testRepository.findBySkillId(10L)).thenReturn(Optional.of(mockTest));
        when(questionRepository.findByTestId(1L)).thenReturn(List.of(q1, q2));
        when(userTestResultRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UserTestResultResponse response = userTestResultService.submitTest(request);

        assertFalse(response.isPassed());
        assertEquals(50, response.getScore());
        verify(certificationRepository, never()).save(any()); // Pas de certification générée
    }

    @Test
    @DisplayName("Cooldown Check : Devrait retourner certified=true si l'utilisateur possède déjà la certification")
    void checkCooldown_ShouldReturnCertifiedTrue() {
        when(testRepository.findBySkillId(10L)).thenReturn(Optional.of(mockTest));
        when(certificationRepository.existsByUserIdAndUserSkillId("user-123", 10L)).thenReturn(true);

        var result = userTestResultService.checkCooldown("user-123", 10L);

        assertTrue((Boolean) result.get("onCooldown"));
        assertTrue((Boolean) result.get("certified"));
    }
}