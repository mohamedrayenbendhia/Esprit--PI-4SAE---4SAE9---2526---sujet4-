package com.microservice.module_certification.services;

import com.microservice.module_certification.dto.*;
import com.microservice.module_certification.entities.*;
import com.microservice.module_certification.exceptions.*;
import com.microservice.module_certification.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserTestResultService {

    private final UserTestResultRepository userTestResultRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final QuestionRepository questionRepository;
    private final CertificationRepository certificationRepository;
    private final TestRepository testRepository;

    // ── Passer un test
    @Transactional
    public UserTestResultResponse submitTest(SubmitTestRequest request) {

        // 1. Trouver le test via userSkillId
        Test test = testRepository.findBySkillId(request.getUserSkillId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No test found for this skill"));

        // 2. Vérifier si déjà passé
        boolean alreadyPassed = userTestResultRepository
                .existsByUserIdAndTestIdAndIsPassed(request.getUserId(), test.getId(), true);
        if (alreadyPassed) {
            throw new DuplicateResourceException("You already passed this test");
        }

// 3. Vérifier cooldown 24h SEULEMENT si pas déjà passé
        Optional<UserTestResult> lastAttempt = userTestResultRepository
                .findTopByUserIdAndTestIdOrderByLastAttemptAtDesc(request.getUserId(), test.getId());

        if (lastAttempt.isPresent() && !lastAttempt.get().isPassed()) {
            LocalDateTime lastAttemptTime = lastAttempt.get().getLastAttemptAt();
            if (lastAttemptTime != null && lastAttemptTime.isAfter(LocalDateTime.now().minusMinutes(2))) {
                long hoursLeft = 0;
                long minutesLeft = 2 - java.time.Duration.between(lastAttemptTime, LocalDateTime.now()).toMinutes();
                throw new RuntimeException("COOLDOWN:" + hoursLeft + "h " + minutesLeft + "m");
            }
        }
        // 3. Récupérer les questions
        List<Question> questions = questionRepository.findByTestId(test.getId());

        // 4. Vérifier nombre de réponses
        if (request.getAnswers().size() != questions.size()) {
            throw new ResourceNotFoundException(
                    "Number of answers must be: " + questions.size());
        }

        // 5. Créer le résultat
        UserTestResult result = UserTestResult.builder()
                .userId(request.getUserId())
                .test(test)
                .userSkillId(request.getUserSkillId())
                .passedAt(LocalDateTime.now())
                .lastAttemptAt(LocalDateTime.now())
                .build();
        UserTestResult saved = userTestResultRepository.save(result);

        // 6. Traiter les réponses
        List<UserAnswer> answers = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            String answer = request.getAnswers().get(i);
            boolean correct = question.getCorrectAnswer()
                    .equalsIgnoreCase(answer.trim());
            answers.add(UserAnswer.builder()
                    .userId(request.getUserId())
                    .question(question)
                    .userTestResult(saved)
                    .answer(answer)
                    .isCorrect(correct)
                    .build());
        }
        userAnswerRepository.saveAll(answers);

        // 7. Calculer score
        long correctCount = answers.stream()
                .filter(UserAnswer::isCorrect).count();
        int score = (int) ((correctCount * 100) / answers.size());
        boolean isPassed = score >= test.getPassingScore();

        // 8. Mettre à jour résultat
        saved.setScore(score);
        saved.setPassed(isPassed);
        saved.setAnswers(answers);
        userTestResultRepository.save(saved);

        // 9. Générer certification si isPassed
        if (isPassed) {
            Certification certification = Certification.builder()
                    .userId(request.getUserId())
                    .userSkillId(request.getUserSkillId())
                    .test(test)
                    .score(score)
                    .date(LocalDate.now())
                    .certificateUrl("https://platform.com/certificates/"
                            + request.getUserId() + "/" + test.getId())
                    .build();
            certificationRepository.save(certification);
        }

        return toResponse(saved, answers, test.getPassingScore());
    }

    // ── Voir résultats par userId
    public List<UserTestResultResponse> getByUserId(String userId) { // ✅
        return userTestResultRepository.findByUserId(userId)
                .stream().map(r -> toResponse(r,
                        userAnswerRepository.findByUserTestResultId(r.getId()),
                        r.getTest().getPassingScore()))
                .toList();
    }

    // ── Voir certifications par userId
    public List<CertificationResponse> getCertificationsByUserId(String userId) { // ✅
        return certificationRepository.findByUserId(userId)
                .stream().map(c -> CertificationResponse.builder()
                        .id(c.getId())
                        .userId(c.getUserId())
                        .userSkillId(c.getUserSkillId())
                        .testTitle(c.getTest().getTitle())
                        .score(c.getScore())
                        .date(c.getDate())
                        .certificateUrl(c.getCertificateUrl())
                        .build())
                .toList();
    }

    // ── Mapper
    private UserTestResultResponse toResponse(UserTestResult r,
                                              List<UserAnswer> answers, int passingScore) {
        return UserTestResultResponse.builder()
                .id(r.getId())
                .userId(r.getUserId())
                .testId(r.getTest().getId())
                .testTitle(r.getTest().getTitle())
                .userSkillId(r.getUserSkillId())
                .score(r.getScore())
                .passingScore(passingScore)
                .isPassed(r.isPassed())
                .passedAt(r.getPassedAt())
                .answers(answers.stream().map(a -> UserAnswerResponse.builder()
                        .id(a.getId())
                        .questionText(a.getQuestion().getQuestionText())
                        .yourAnswer(a.getAnswer())
                        .isCorrect(a.isCorrect())
                        .build()).toList())
                .build();
    }
    public Map<String, Object> checkCooldown(String userId, Long skillId) {
        Test test = testRepository.findBySkillId(skillId).orElse(null);
        if (test == null) return Map.of("onCooldown", false);

        // Vérifier si déjà certifié
        boolean alreadyCertified = certificationRepository
                .existsByUserIdAndUserSkillId(userId, skillId);
        if (alreadyCertified) {
            return Map.of("onCooldown", true, "certified", true);
        }

        Optional<UserTestResult> last = userTestResultRepository
                .findTopByUserIdAndTestIdOrderByLastAttemptAtDesc(userId, test.getId());

        if (last.isPresent() && !last.get().isPassed()) {
            LocalDateTime lastAttemptTime = last.get().getLastAttemptAt();
            if (lastAttemptTime != null && lastAttemptTime.isAfter(LocalDateTime.now().minusMinutes(2))) {
                return Map.of("onCooldown", true, "lastAttemptAt", lastAttemptTime.toString());
            }
        }
        return Map.of("onCooldown", false);
    }
}