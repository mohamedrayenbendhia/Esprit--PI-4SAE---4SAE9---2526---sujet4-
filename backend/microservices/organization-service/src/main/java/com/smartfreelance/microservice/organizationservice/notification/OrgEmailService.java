package com.smartfreelance.microservice.organizationservice.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrgEmailService {

    @Async
    public void sendInvitationEmail(String toEmail, String orgName, String invitationToken) {
        log.info("[EMAIL] Invitation to {} for org {} (token: {})", toEmail, orgName, invitationToken);
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String orgName) {
        log.info("[EMAIL] Welcome email to {} for org {}", toEmail, orgName);
    }

    @Async
    public void sendApplicationStatusEmail(String toEmail, String orgName, boolean accepted) {
        log.info("[EMAIL] Application {} email to {} for org {}",
                accepted ? "accepted" : "rejected", toEmail, orgName);
    }
}
