package com.microservice.module_certification.services;

import com.microservice.module_certification.entities.UserTestResult;
import com.microservice.module_certification.repositories.UserTestResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CooldownNotificationService {

    private final UserTestResultRepository userTestResultRepository;
    private final JavaMailSender mailSender;
    private final UserClientService userClientService;

    // Vérifie toutes les minutes
    @Scheduled(fixedRate = 60000)
    public void checkExpiredCooldowns() {
        LocalDateTime cooldownLimit = LocalDateTime.now().minusMinutes(2);

        List<UserTestResult> expiredCooldowns = userTestResultRepository
                .findExpiredCooldowns(cooldownLimit);

        for (UserTestResult result : expiredCooldowns) {
            try {
                String email = userClientService.getUserEmail(result.getUserId());
                String firstName = userClientService.getUserFirstName(result.getUserId());
                if (email != null) {
                    sendCooldownExpiredEmail(email, firstName, result.getTest().getTitle());
                    result.setNotificationSent(true);
                    userTestResultRepository.save(result);
                }
            } catch (Exception e) {
                log.error("Error sending cooldown notification for userId: {}", result.getUserId(), e);
            }
        }
    }

    private void sendCooldownExpiredEmail(String email, String firstName, String testTitle) {
        SimpleMailMessage message = new SimpleMailMessage();
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
        log.info("Cooldown expired email sent to: {}", email);
    }
}