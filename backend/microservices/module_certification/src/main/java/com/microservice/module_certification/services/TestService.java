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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final UserAnswerRepository userAnswerRepository;

    // ── Créer test + questions
    @Transactional
    public TestResponse create(TestRequest request) {
        Test test = new Test();
        test.setTitle(request.getTitle());
        test.setSkillId(request.getSkillId());
        test.setPassingScore(request.getPassingScore());

        Test saved = testRepository.save(test);

        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            List<Question> questions = new ArrayList<>();
            for (QuestionRequest qReq : request.getQuestions()) {
                Question question = new Question();
                question.setTest(saved);
                question.setQuestionText(qReq.getQuestionText());
                question.setCorrectAnswer(qReq.getCorrectAnswer());
                questions.add(question);
            }
            questionRepository.saveAll(questions);
            saved.setQuestions(questions);
        }
        return toResponse(saved);
    }

    // ── Voir tous les tests
    public List<TestResponse> getAll() {
        List<TestResponse> responses = new ArrayList<>();
        for (Test test : testRepository.findAll()) {
            responses.add(toResponse(test));
        }
        return responses;
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
        test.setSkillId(request.getSkillId());
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
        Question question = new Question();
        question.setTest(test);
        question.setQuestionText(request.getQuestionText());
        question.setCorrectAnswer(request.getCorrectAnswer());
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
        QuestionResponse response = new QuestionResponse();
        response.setId(q.getId());
        response.setQuestionText(q.getQuestionText());
        response.setCorrectAnswer(q.getCorrectAnswer());
        return response;
    }

    public TestResponse toResponse(Test t) {
        List<Question> questions = t.getQuestions() != null && !t.getQuestions().isEmpty()
                ? t.getQuestions()
                : questionRepository.findByTestId(t.getId());

        List<QuestionResponse> questionResponses = new ArrayList<>();
        for (Question q : questions) {
            questionResponses.add(toQuestionResponse(q));
        }

        TestResponse response = new TestResponse();
        response.setId(t.getId());
        response.setTitle(t.getTitle());
        response.setSkillId(t.getSkillId());
        response.setPassingScore(t.getPassingScore());
        response.setQuestions(questionResponses);
        return response;
    }

    // ── Voir test sans correctAnswer (pour freelancer)
    public TestPublicResponse getBySkillIdPublic(Long skillId) {
        Test test = testRepository.findBySkillId(skillId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Test not found for skillId: " + skillId));
        List<Question> questions = questionRepository.findByTestId(test.getId());

        List<QuestionPublicResponse> publicQuestions = new ArrayList<>();
        for (Question q : questions) {
            QuestionPublicResponse publicQ = new QuestionPublicResponse();
            publicQ.setId(q.getId());
            publicQ.setQuestionText(q.getQuestionText());
            publicQuestions.add(publicQ);
        }

        TestPublicResponse response = new TestPublicResponse();
        response.setId(test.getId());
        response.setTitle(test.getTitle());
        response.setSkillId(test.getSkillId());
        response.setPassingScore(test.getPassingScore());
        response.setQuestions(publicQuestions);
        return response;
    }
}