package com.microservice.module_certification.services;

import com.microservice.module_certification.dto.*;
import com.microservice.module_certification.entities.*;
import com.microservice.module_certification.exceptions.*;
import com.microservice.module_certification.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TestService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final UserAnswerRepository userAnswerRepository;


    // ── Créer test + questions
    @Transactional
    public TestResponse create(TestRequest request) {
        if (testRepository.existsBySkillId(request.getSkillId())) {
            throw new DuplicateResourceException(
                    "Test already exists for skillId: " + request.getSkillId());
        }
        Test test = Test.builder()
                .title(request.getTitle())
                .skillId(request.getSkillId())
                .passingScore(request.getPassingScore())
                .build();
        Test saved = testRepository.save(test);

        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            List<Question> questions = request.getQuestions().stream()
                    .map(q -> Question.builder()
                            .test(saved)
                            .questionText(q.getQuestionText())
                            .correctAnswer(q.getCorrectAnswer())
                            .build())
                    .toList();
            questionRepository.saveAll(questions);
            saved.setQuestions(new ArrayList<>(questions));
        }
        return toResponse(saved);
    }

    // ── Voir tous les tests
    public List<TestResponse> getAll() {
        return testRepository.findAll()
                .stream().map(this::toResponse).toList();
    }

    // ── Voir test par id
    public TestResponse getById(Long id) {
        return toResponse(findById(id));
    }

    // ── Voir test par skillId
    public TestResponse getBySkillId(Long skillId) {
        Test test = testRepository.findBySkillId(skillId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Test not found for skillId: " + skillId));
        return toResponse(test);
    }

    // ── Modifier test
    @Transactional
    public TestResponse update(Long id, TestRequest request) {
        Test test = findById(id);
        test.setTitle(request.getTitle());
        test.setPassingScore(request.getPassingScore());
        return toResponse(testRepository.save(test));
    }

    // ── Supprimer test
    @Transactional
    public void delete(Long id) {
        if (!testRepository.existsById(id)) {
            throw new ResourceNotFoundException("Test not found with id: " + id);
        }
        testRepository.deleteById(id);
    }

    // ── Ajouter question
    @Transactional
    public QuestionResponse addQuestion(Long testId, QuestionRequest request) {
        Test test = findById(testId);
        Question question = Question.builder()
                .test(test)
                .questionText(request.getQuestionText())
                .correctAnswer(request.getCorrectAnswer())
                .build();
        return toQuestionResponse(questionRepository.save(question));
    }

    // ── Modifier question
    @Transactional
    public QuestionResponse updateQuestion(Long questionId, QuestionRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Question not found with id: " + questionId));
        question.setQuestionText(request.getQuestionText());
        question.setCorrectAnswer(request.getCorrectAnswer());
        return toQuestionResponse(questionRepository.save(question));
    }

    // ── Supprimer question
    @Transactional
    public void deleteQuestion(Long questionId) {
        if (!questionRepository.existsById(questionId)) {
            throw new ResourceNotFoundException(
                    "Question not found with id: " + questionId);
        }
        userAnswerRepository.deleteByQuestionId(questionId);
        questionRepository.deleteById(questionId);
    }

    // ── Finder
    public Test findById(Long id) {
        return testRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Test not found with id: " + id));
    }

    // ── Mappers
    public QuestionResponse toQuestionResponse(Question q) {
        return QuestionResponse.builder()
                .id(q.getId())
                .questionText(q.getQuestionText())
                .correctAnswer(q.getCorrectAnswer())
                .build();
    }

    public TestResponse toResponse(Test t) {
        List<Question> questions = t.getQuestions() != null && !t.getQuestions().isEmpty()
                ? t.getQuestions()
                : questionRepository.findByTestId(t.getId());
        return TestResponse.builder()
                .id(t.getId())
                .title(t.getTitle())
                .skillId(t.getSkillId())
                .passingScore(t.getPassingScore())
                .questions(questions.stream()
                        .map(this::toQuestionResponse).toList())
                .build();
    }

    // ── Voir test sans correctAnswer (pour freelancer)
    public TestPublicResponse getBySkillIdPublic(Long skillId) {
        Test test = testRepository.findBySkillId(skillId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Test not found for skillId: " + skillId));
        List<Question> questions = questionRepository.findByTestId(test.getId());
        return TestPublicResponse.builder()
                .id(test.getId())
                .title(test.getTitle())
                .skillId(test.getSkillId())
                .passingScore(test.getPassingScore())
                .questions(questions.stream().map(q -> QuestionPublicResponse.builder()
                        .id(q.getId())
                        .questionText(q.getQuestionText())
                        .build()).toList())
                .build();
    }
}