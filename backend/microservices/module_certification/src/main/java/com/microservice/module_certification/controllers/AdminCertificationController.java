package com.microservice.module_certification.controllers;

import com.microservice.module_certification.dto.*;
import com.microservice.module_certification.entities.Test;
import com.microservice.module_certification.services.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/tests")
public class AdminCertificationController {

    private final TestService testService;
    private final AiQuestionGeneratorService aiService;

    public AdminCertificationController(TestService testService, AiQuestionGeneratorService aiService) {
        this.testService = testService;
        this.aiService = aiService;
    }

    // POST /api/admin/tests
    @PostMapping
    public ResponseEntity<TestResponse> create(
            @Valid @RequestBody TestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(testService.create(request));
    }

    // GET /api/admin/tests
    @GetMapping
    public ResponseEntity<List<TestResponse>> getAll() {
        return ResponseEntity.ok(testService.getAll());
    }

    // GET /api/admin/tests/{id}
    @GetMapping("/{id}")
    public ResponseEntity<TestResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(testService.getById(id));
    }

    // PUT /api/admin/tests/{id}
    @PutMapping("/{id}")
    public ResponseEntity<TestResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TestRequest request) {
        return ResponseEntity.ok(testService.update(id, request));
    }

    // DELETE /api/admin/tests/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        testService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // POST /api/admin/tests/{testId}/questions
    @PostMapping("/{testId}/questions")
    public ResponseEntity<QuestionResponse> addQuestion(
            @PathVariable Long testId,
            @Valid @RequestBody QuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(testService.addQuestion(testId, request));
    }

    // PUT /api/admin/tests/questions/{id}
    @PutMapping("/questions/{id}")
    public ResponseEntity<QuestionResponse> updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody QuestionRequest request) {
        return ResponseEntity.ok(testService.updateQuestion(id, request));
    }

    // DELETE /api/admin/tests/questions/{id}
    @DeleteMapping("/questions/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        testService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }

    // POST /api/admin/tests/{testId}/ai-generate?count=5&skillName=Angular
    @PostMapping("/{testId}/ai-generate")
    public ResponseEntity<?> generateQuestionsWithAI(
            @PathVariable Long testId,
            @RequestParam(defaultValue = "5") int count,
            @RequestParam(defaultValue = "") String skillName) {
        try {
            Test test = testService.findById(testId);
            List<AiQuestionGeneratorService.GeneratedQuestion> generated =
                aiService.generateQuestions(test.getTitle(), skillName, count);

            List<QuestionResponse> saved = generated.stream().map(g -> {
                QuestionRequest req = new QuestionRequest();
                req.setQuestionText(g.questionText());
                req.setCorrectAnswer(g.correctAnswer());
                return testService.addQuestion(testId, req);
            }).toList();

            return ResponseEntity.ok(saved);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(java.util.Map.of("error", "AI generation failed: " + e.getMessage()));
        }
    }
}