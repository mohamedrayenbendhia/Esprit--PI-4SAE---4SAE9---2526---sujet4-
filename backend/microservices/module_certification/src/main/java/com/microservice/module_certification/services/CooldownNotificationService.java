package com.microservice.module_certification.services;

import com.microservice.module_certification.entities.UserTestResult;
import com.microservice.module_certification.repositories.UserTestResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CooldownNotificationService {

    private final UserTestResultRepository userTestResultRepository;
    private final JavaMailSender mailSender;
    private final UserClientService userClientService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void checkExpiredCooldowns() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cooldownExpiredAfter = now.minusMinutes(2);
        LocalDateTime windowStart = cooldownExpiredAfter.minusSeconds(35);

        log.info("[COOLDOWN] Checking at {} | window: {} → {}",
            now, windowStart, cooldownExpiredAfter);

        List<UserTestResult> expiredCooldowns = userTestResultRepository
                .findExpiredCooldowns(cooldownExpiredAfter, windowStart);

        log.info("[COOLDOWN] Found {} expired cooldown(s) to notify", expiredCooldowns.size());

        for (UserTestResult result : expiredCooldowns) {
            log.info("[COOLDOWN] Processing userId={}, testId={}, lastAttemptAt={}",
                result.getUserId(), result.getTest().getId(), result.getLastAttemptAt());
            try {
                String email = userClientService.getUserEmail(result.getUserId());
                String firstName = userClientService.getUserFirstName(result.getUserId());

                log.info("[COOLDOWN] User lookup → email={}, firstName={}", email, firstName);

                if (email != null && !email.isBlank()) {
                    sendCooldownExpiredEmail(email, firstName, result.getTest().getTitle());
                    result.setNotificationSent(true);
                    userTestResultRepository.save(result);
                    log.info("[COOLDOWN] ✅ Email sent and notificationSent=true saved for userId={}", result.getUserId());
                } else {
                    log.warn("[COOLDOWN] ⚠️ Could not fetch email for userId={}, skipping", result.getUserId());
                }
            } catch (Exception e) {
                log.error("[COOLDOWN] ❌ Error for userId={}: {}", result.getUserId(), e.getMessage(), e);
            }
        }
    }

    private void sendCooldownExpiredEmail(String email, String firstName, String testTitle) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("NexLance — You can retake your test!");
        message.setText(
            "Hello " + firstName + ",\n\n" +
            "Your cooldown period has expired.\n" +
            "You can now retake the certification test for: " + testTitle + "\n\n" +
            "Visit: http://localhost:4200/frontoffice/freelancer/certifications\n\n" +
            "Good luck!\n" +
            "NexLance Team"
        );
        mailSender.send(message);
        log.info("[COOLDOWN] Mail sent to {}", email);
    }
}
