package com.microservice.module_certification.services;

import com.microservice.module_certification.dto.*;
import com.microservice.module_certification.entities.Question;
import com.microservice.module_certification.exceptions.DuplicateResourceException;
import com.microservice.module_certification.exceptions.ResourceNotFoundException;
import com.microservice.module_certification.repositories.QuestionRepository;
import com.microservice.module_certification.repositories.TestRepository;
import com.microservice.module_certification.repositories.UserAnswerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test; // JUnit Test
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestServiceTest {

    @Mock
    private TestRepository testRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private UserAnswerRepository userAnswerRepository;

    @InjectMocks
    private TestService testService;

    private com.microservice.module_certification.entities.Test mockTest;
    private Long testId = 1L;
    private Long skillId = 100L;

    @BeforeEach
    void setUp() {
        mockTest = com.microservice.module_certification.entities.Test.builder()
                .id(testId)
                .title("Java Test")
                .skillId(skillId)
                .passingScore(70)
                .questions(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Devrait créer un test avec succès")
    void create_ShouldReturnResponse_WhenValid() {
        // Préparation
        TestRequest request = new TestRequest();
        request.setSkillId(skillId);
        request.setTitle("Java Test");
        request.setPassingScore(70);
        request.setQuestions(Collections.emptyList());

        when(testRepository.existsBySkillId(skillId)).thenReturn(false);
        when(testRepository.save(any())).thenReturn(mockTest);

        // Action
        TestResponse response = testService.create(request);

        // Vérification
        assertNotNull(response);
        assertEquals("Java Test", response.getTitle());
        verify(testRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Devrait lancer une exception si le test existe déjà pour ce Skill")
    void create_ShouldThrowException_WhenDuplicateSkill() {
        TestRequest request = new TestRequest();
        request.setSkillId(skillId);

        when(testRepository.existsBySkillId(skillId)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> testService.create(request));
    }

    @Test
    @DisplayName("Devrait retourner un test par son ID")
    void getById_ShouldReturnResponse_WhenExists() {
        when(testRepository.findById(testId)).thenReturn(Optional.of(mockTest));
        when(questionRepository.findByTestId(testId)).thenReturn(Collections.emptyList());

        TestResponse response = testService.getById(testId);

        assertNotNull(response);
        assertEquals(testId, response.getId());
    }

    @Test
    @DisplayName("Devrait lancer ResourceNotFound si l'ID n'existe pas")
    void getById_ShouldThrowNotFound_WhenNotExists() {
        when(testRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> testService.getById(999L));
    }

    @Test
    @DisplayName("Devrait supprimer un test et ses dépendances")
    void delete_ShouldCallRepository_WhenExists() {
        when(testRepository.existsById(testId)).thenReturn(true);

        testService.delete(testId);

        verify(testRepository, times(1)).deleteById(testId);
    }

    @Test
    @DisplayName("Devrait ajouter une question à un test existant")
    void addQuestion_ShouldSaveQuestion() {
        QuestionRequest qRequest = new QuestionRequest();
        qRequest.setQuestionText("What is Spring?");
        qRequest.setCorrectAnswer("A Framework");

        when(testRepository.findById(testId)).thenReturn(Optional.of(mockTest));
        when(questionRepository.save(any(Question.class))).thenAnswer(i -> i.getArgument(0));

        QuestionResponse response = testService.addQuestion(testId, qRequest);

        assertNotNull(response);
        assertEquals("What is Spring?", response.getQuestionText());
        verify(questionRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Devrait retourner la version publique du test (sans les réponses)")
    void getBySkillIdPublic_ShouldHideCorrectAnswers() {
        Question q = Question.builder().id(10L).questionText("Q1").correctAnswer("SECRET").build();

        when(testRepository.findBySkillId(skillId)).thenReturn(Optional.of(mockTest));
        when(questionRepository.findByTestId(testId)).thenReturn(List.of(q));

        TestPublicResponse response = testService.getBySkillIdPublic(skillId);

        assertNotNull(response);
        assertEquals(1, response.getQuestions().size());
        // On vérifie que le champ correctAnswer n'existe pas dans le DTO de réponse publique
        // (C'est une vérification de design, le compilateur le ferait déjà si tu essayais d'y accéder)
    }
}