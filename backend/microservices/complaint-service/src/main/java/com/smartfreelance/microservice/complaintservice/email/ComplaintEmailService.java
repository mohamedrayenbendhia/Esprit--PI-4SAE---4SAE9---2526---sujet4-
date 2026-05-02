package com.smartfreelance.microservice.complaintservice.email;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Service d'envoi d'emails pour les réclamations.
 *
 * Tous les envois sont asynchrones (@Async("mailExecutor")).
 * Chaque envoi fait jusqu'à 3 tentatives avec backoff exponentiel.
 *
 * Règle anti-spam :
 *  - sendNewMessageEmailIfInactive() vérifie UserActivityTracker avant d'envoyer.
 *  - Les autres méthodes envoient toujours.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintEmailService {

    private final JavaMailSender mailSender;
    private final UserActivityTracker activityTracker;

    // B-E1 — FROM injecté depuis spring.mail.username (compatible Gmail SMTP)
    @Value("${spring.mail.username}")
    private String mailFrom;

    @Value("${user-service.url:http://localhost:8084}")
    private String userServiceUrl;

    @Value("${frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    private static final String APP_NAME = "NexLance";

    private WebClient userWebClient;

    @PostConstruct
    public void init() {
        this.userWebClient = WebClient.builder()
                .baseUrl(userServiceUrl + "/api/users")
                .build();
    }

    // =========================================================================
    // EMAILS MÉTIER
    // =========================================================================

    /** 1. Accusé de réception — envoi immédiat */
    @Async("mailExecutor")
    public void sendComplaintCreatedEmail(String userId, String ticketNumber,
                                          String subject, String complaintUrl) {
        String email = fetchUserEmail(userId);
        if (email == null) return;
        String html = buildCreatedHtml(ticketNumber, subject, complaintUrl, userId);
        send(email, "[" + APP_NAME + "] Réclamation enregistrée — " + ticketNumber, html);
    }

    /** 2. Changement de statut — envoi immédiat */
    @Async("mailExecutor")
    public void sendStatusChangedEmail(String userId, String ticketNumber,
                                       String subject, String newStatus, String complaintUrl) {
        String email = fetchUserEmail(userId);
        if (email == null) return;
        String html = buildStatusChangedHtml(ticketNumber, subject, newStatus, complaintUrl, userId);
        send(email, "[" + APP_NAME + "] Mise à jour — " + ticketNumber, html);
    }

    /** 3. Résolution — envoi immédiat */
    @Async("mailExecutor")
    public void sendComplaintResolvedEmail(String userId, String ticketNumber,
                                           String subject, String complaintUrl) {
        String email = fetchUserEmail(userId);
        if (email == null) return;
        String html = buildResolvedHtml(ticketNumber, subject, complaintUrl, userId);
        send(email, "[" + APP_NAME + "] Réclamation résolue — " + ticketNumber, html);
    }

    /** 4. Clôture — envoi immédiat */
    @Async("mailExecutor")
    public void sendComplaintClosedEmail(String userId, String ticketNumber,
                                         String subject, String complaintUrl) {
        String email = fetchUserEmail(userId);
        if (email == null) return;
        String html = buildClosedHtml(ticketNumber, subject, complaintUrl, userId);
        send(email, "[" + APP_NAME + "] Réclamation clôturée — " + ticketNumber, html);
    }

    /**
     * 5. Nouveau message — envoi conditionnel (anti-spam).
     * L'email est envoyé UNIQUEMENT si l'utilisateur est inactif (> 8 min sans ping).
     */
    @Async("mailExecutor")
    public void sendNewMessageEmailIfInactive(String userId, String ticketNumber,
                                              String subject, String senderName,
                                              String messageExcerpt, String complaintUrl) {
        if (!activityTracker.isInactive(userId)) {
            log.debug("[Email] Utilisateur {} actif — email nouveau message ignoré", userId);
            return;
        }
        String email = fetchUserEmail(userId);
        if (email == null) return;
        String html = buildNewMessageHtml(ticketNumber, subject, senderName,
                                          messageExcerpt, complaintUrl, userId);
        send(email, "[" + APP_NAME + "] Nouveau message — " + ticketNumber, html);
    }

    /** 6. Implication de la partie mise en cause — envoi immédiat */
    @Async("mailExecutor")
    public void sendInvolvedEmail(String userId, String ticketNumber,
                                  String subject, String invitationMessage, String complaintUrl) {
        String email = fetchUserEmail(userId);
        if (email == null) return;
        String html = buildInvolvedHtml(ticketNumber, subject, invitationMessage, complaintUrl, userId);
        send(email,
             "[" + APP_NAME + "] Vous êtes impliqué dans une réclamation — " + ticketNumber,
             html);
    }

    /** 7. Escalade — email à l'admin assigné */
    @Async("mailExecutor")
    public void sendEscalatedAdminEmail(String adminId, String ticketNumber,
                                        String subject, String complaintUrl) {
        String email = fetchUserEmail(adminId);
        if (email == null) return;
        String html = buildEscalatedAdminHtml(ticketNumber, subject, complaintUrl, adminId);
        send(email,
             "[" + APP_NAME + "] Action requise — réclamation escaladée : " + ticketNumber,
             html);
    }

    /** 8. Escalade — email rassurant au plaignant */
    @Async("mailExecutor")
    public void sendEscalatedUserEmail(String userId, String ticketNumber,
                                       String subject, String complaintUrl) {
        String email = fetchUserEmail(userId);
        if (email == null) return;
        String html = buildEscalatedUserHtml(ticketNumber, subject, complaintUrl, userId);
        send(email,
             "[" + APP_NAME + "] Votre réclamation est prise en charge par un superviseur — "
                 + ticketNumber,
             html);
    }

    // =========================================================================
    // ENVOI SMTP — 3 tentatives avec backoff
    // =========================================================================

    private void send(String to, String subject, String htmlBody) {
        int maxAttempts = 3;
        long[] delaysMs = { 0L, 5_000L, 15_000L };

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                if (attempt > 0) {
                    Thread.sleep(delaysMs[attempt]);
                    log.info("[Email] Tentative {}/{} pour {}", attempt + 1, maxAttempts, to);
                }

                var message = mailSender.createMimeMessage();
                var helper  = new MimeMessageHelper(message, false, "UTF-8");

                try {
                    helper.setFrom(mailFrom, APP_NAME);
                } catch (java.io.UnsupportedEncodingException ex) {
                    log.warn("[Email] Encoding expéditeur échoué, fallback sans nom : {}", ex.getMessage());
                    helper.setFrom(mailFrom);
                }

                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlBody, true);
                mailSender.send(message);
                log.debug("[Email] Envoyé à {} — {}", to, subject);
                return; // succès

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("[Email] Échec tentative {}/{} pour {} : {}",
                        attempt + 1, maxAttempts, to, e.getMessage());
            }
        }
        log.error("[Email] Abandon après {} tentatives pour {}", maxAttempts, to);
    }

    // =========================================================================
    // RÉCUPÉRATION EMAIL UTILISATEUR
    // =========================================================================

    private String fetchUserEmail(String userId) {
        try {
            Map<String, Object> response = userWebClient.get()
                    .uri("/{id}", userId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            if (response != null && response.containsKey("email")) {
                return (String) response.get("email");
            }
        } catch (Exception e) {
            log.warn("[Email] Impossible de récupérer l'email de {} : {}", userId, e.getMessage());
        }
        return null;
    }

    // =========================================================================
    // UTILITAIRE — Échappement HTML (B-E1)
    // =========================================================================

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&",  "&amp;")
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;")
                .replace("'",  "&#x27;")
                .replace("\n", "<br>")
                .replace("\r", "");
    }

    // =========================================================================
    // LIEN DE DÉSINSCRIPTION (B-E3)
    // =========================================================================

    private String buildUnsubscribeUrl(String userId) {
        return frontendBaseUrl + "/settings/notifications?unsubscribe=" + userId;
    }

    // =========================================================================
    // TEMPLATES HTML (B-E2 — escapeHtml + userId pour lien désabo)
    // =========================================================================

    private String buildCreatedHtml(String ticket, String subject, String url, String userId) {
        return wrapInLayout("""
            <p>Bonjour,</p>
            <span class="ticket-badge">%s</span>
            <p>Votre réclamation a bien été enregistrée :</p>
            <p><strong>%s</strong></p>
            <p>Notre équipe support l'examinera dans les plus brefs délais.
               Vous serez notifié à chaque mise à jour.</p>
            <a href="%s" class="btn">Voir ma réclamation</a>
            <p>Merci pour votre confiance.</p>
            """.formatted(escapeHtml(ticket), escapeHtml(subject), url),
            buildUnsubscribeUrl(userId));
    }

    private String buildStatusChangedHtml(String ticket, String subject,
                                           String status, String url, String userId) {
        return wrapInLayout("""
            <p>Bonjour,</p>
            <span class="ticket-badge">%s</span>
            <p>Le statut de votre réclamation a été mis à jour :</p>
            <p><strong>%s</strong></p>
            <p>Nouveau statut : <span class="status-badge">%s</span></p>
            <a href="%s" class="btn">Voir la réclamation</a>
            """.formatted(escapeHtml(ticket), escapeHtml(subject),
                          escapeHtml(formatStatus(status)), url),
            buildUnsubscribeUrl(userId));
    }

    private String buildResolvedHtml(String ticket, String subject, String url, String userId) {
        return wrapInLayout("""
            <p>Bonjour,</p>
            <span class="ticket-badge">%s</span>
            <p>Bonne nouvelle ! Votre réclamation a été <strong>résolue</strong> :</p>
            <p><strong>%s</strong></p>
            <p>Vous pouvez consulter le détail de la résolution et noter votre expérience.</p>
            <a href="%s" class="btn">Voir la résolution</a>
            """.formatted(escapeHtml(ticket), escapeHtml(subject), url),
            buildUnsubscribeUrl(userId));
    }

    private String buildClosedHtml(String ticket, String subject, String url, String userId) {
        return wrapInLayout("""
            <p>Bonjour,</p>
            <span class="ticket-badge">%s</span>
            <p>Votre réclamation a été <strong>clôturée</strong> :</p>
            <p><strong>%s</strong></p>
            <p>Vous pouvez désormais noter votre satisfaction pour nous aider à améliorer notre service.</p>
            <a href="%s" class="btn">Noter mon expérience</a>
            """.formatted(escapeHtml(ticket), escapeHtml(subject), url),
            buildUnsubscribeUrl(userId));
    }

    private String buildNewMessageHtml(String ticket, String subject,
                                        String sender, String excerpt,
                                        String url, String userId) {
        return wrapInLayout("""
            <p>Bonjour,</p>
            <span class="ticket-badge">%s</span>
            <p>Vous avez un nouveau message sur votre réclamation :</p>
            <p><strong>%s</strong></p>
            <p>De : <strong>%s</strong></p>
            <div class="excerpt-box">%s</div>
            <a href="%s" class="btn">Répondre au message</a>
            """.formatted(escapeHtml(ticket), escapeHtml(subject),
                          escapeHtml(sender), escapeHtml(excerpt), url),
            buildUnsubscribeUrl(userId));
    }

    private String buildInvolvedHtml(String ticket, String subject,
                                      String invitationMessage, String url, String userId) {
        return wrapInLayout("""
            <p>Bonjour,</p>
            <p>Vous avez été impliqué dans une réclamation sur NexLance :</p>
            <span class="ticket-badge">%s</span>
            <p><strong>%s</strong></p>
            <div class="excerpt-box">%s</div>
            <p>Notre équipe support souhaite recueillir votre version des faits.
               Merci de vous connecter pour répondre.</p>
            <a href="%s" class="btn">Accéder à la conversation</a>
            """.formatted(escapeHtml(ticket), escapeHtml(subject),
                          escapeHtml(invitationMessage), url),
            buildUnsubscribeUrl(userId));
    }

    private String buildEscalatedAdminHtml(String ticket, String subject,
                                            String url, String userId) {
        return wrapInLayout("""
            <p>Bonjour,</p>
            <span class="ticket-badge">%s</span>
            <p>Une réclamation <strong>CRITIQUE</strong> vous a été escaladée
               et nécessite votre intervention :</p>
            <p><strong>%s</strong></p>
            <p>Un agent a jugé ce dossier nécessitant une décision de niveau supérieur.</p>
            <a href="%s" class="btn">Accéder à la réclamation</a>
            """.formatted(escapeHtml(ticket), escapeHtml(subject), url),
            buildUnsubscribeUrl(userId));
    }

    private String buildEscalatedUserHtml(String ticket, String subject,
                                           String url, String userId) {
        return wrapInLayout("""
            <p>Bonjour,</p>
            <span class="ticket-badge">%s</span>
            <p>Votre réclamation a été transmise à un superviseur :</p>
            <p><strong>%s</strong></p>
            <p>Notre équipe a décidé d'impliquer un responsable pour traiter votre dossier au mieux.
               Vous serez informé dès qu'une décision sera prise.</p>
            <a href="%s" class="btn">Suivre ma réclamation</a>
            """.formatted(escapeHtml(ticket), escapeHtml(subject), url),
            buildUnsubscribeUrl(userId));
    }

    // =========================================================================
    // LAYOUT HTML (B-E3 — surcharge avec lien de désinscription)
    // =========================================================================

    private String wrapInLayout(String content, String unsubscribeUrl) {
        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <style>
                body { font-family: 'Roboto', Arial, sans-serif;
                       background: #f5f7fa; margin: 0; padding: 20px; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff;
                             border-radius: 8px; overflow: hidden;
                             box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                .header { background: linear-gradient(135deg, #1565C0, #0d47a1);
                          padding: 24px 32px; text-align: center; }
                .header h1 { color: #fff; margin: 0; font-size: 22px; font-weight: 700; }
                .header span { color: #90CAF9; font-size: 13px; }
                .body { padding: 28px 32px; color: #333; }
                .body p { line-height: 1.6; margin: 0 0 16px; }
                .ticket-badge { display: inline-block; background: #E3F2FD; color: #1565C0;
                                border-radius: 20px; padding: 4px 14px; font-size: 13px;
                                font-weight: 600; margin-bottom: 16px; }
                .status-badge { display: inline-block; background: #E8F5E9; color: #2E7D32;
                                border-radius: 4px; padding: 4px 12px; font-weight: 600; }
                .excerpt-box { background: #F5F5F5; border-left: 4px solid #1565C0;
                               border-radius: 4px; padding: 12px 16px; margin: 16px 0;
                               font-size: 14px; color: #555; font-style: italic; }
                .btn { display: inline-block; background: #1565C0; color: #fff !important;
                       text-decoration: none; padding: 12px 28px; border-radius: 6px;
                       font-weight: 600; font-size: 14px; margin: 16px 0; }
                .footer { background: #F5F7FA; padding: 16px 32px;
                          text-align: center; font-size: 12px; color: #999; }
                .unsub-link { color: #bbb; font-size: 11px; text-decoration: underline; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>NexLance</h1>
                  <span>Smart Matching · Support</span>
                </div>
                <div class="body">
                  %s
                </div>
                <div class="footer">
                  © 2026 NexLance — Plateforme de Freelance<br>
                  Cet email est automatique, merci de ne pas y répondre directement.<br>
                  <a href="%s" class="unsub-link">Se désabonner des notifications email</a>
                </div>
              </div>
            </body>
            </html>
            """.formatted(content, unsubscribeUrl);
    }

    // =========================================================================
    // DIGEST QUOTIDIEN AGENT
    // =========================================================================

    /**
     * Envoie un récapitulatif quotidien à un agent listant ses réclamations
     * actives non mises à jour depuis > 24h, groupées par priorité.
     */
    @Async("mailExecutor")
    public void sendAgentDailyDigest(String agentId, java.util.List<com.smartfreelance.microservice.complaintservice.entity.Complaint> complaints) {
        String agentEmail = fetchUserEmail(agentId);
        if (agentEmail == null || agentEmail.isBlank()) return;

        StringBuilder rows = new StringBuilder();
        complaints.stream()
                .sorted(java.util.Comparator.comparing(c ->
                    c.getPriority() != null ? c.getPriority().ordinal() : 99))
                .forEach(c -> rows.append(String.format("""
                    <tr>
                      <td style="padding:8px 12px;border-bottom:1px solid #f0f2f5;font-weight:600;">%s</td>
                      <td style="padding:8px 12px;border-bottom:1px solid #f0f2f5;">%s</td>
                      <td style="padding:8px 12px;border-bottom:1px solid #f0f2f5;">%s</td>
                      <td style="padding:8px 12px;border-bottom:1px solid #f0f2f5;color:%s;">%s</td>
                    </tr>
                """,
                    c.getTicketNumber(), c.getSubject(),
                    formatStatus(c.getStatus().name()),
                    priorityColor(c.getPriority() != null ? c.getPriority().name() : ""),
                    c.getPriority() != null ? c.getPriority().name() : "—"
                )));

        String unsubUrl = buildUnsubscribeUrl(agentId);
        String html = wrapInLayout(
            String.format("""
                <h2 style="margin:0 0 8px;font-size:18px;color:#1a1f36;">
                  Bonjour,
                </h2>
                <p style="color:#6b7280;margin:0 0 20px;font-size:14px;">
                  Voici vos <strong>%d réclamations actives</strong> en attente de traitement depuis plus de 24h.
                </p>
                <table style="width:100%%;border-collapse:collapse;font-size:13px;">
                  <thead>
                    <tr style="background:#f8fafc;">
                      <th style="padding:10px 12px;text-align:left;font-weight:700;color:#374151;">Ticket</th>
                      <th style="padding:10px 12px;text-align:left;font-weight:700;color:#374151;">Sujet</th>
                      <th style="padding:10px 12px;text-align:left;font-weight:700;color:#374151;">Statut</th>
                      <th style="padding:10px 12px;text-align:left;font-weight:700;color:#374151;">Priorité</th>
                    </tr>
                  </thead>
                  <tbody>%s</tbody>
                </table>
                <p style="color:#6b7280;font-size:12px;margin-top:20px;">
                  Connectez-vous au backoffice pour traiter ces réclamations.
                </p>
            """, complaints.size(), rows.toString()),
            unsubUrl
        );

        send(agentEmail, "📋 Votre récapitulatif quotidien — " + complaints.size() + " réclamation(s) en attente", html);
    }

    // =========================================================================
    // UTILITAIRE — Formatage statut
    // =========================================================================

    private String formatStatus(String status) {
        if (status == null) return "";
        return switch (status) {
            case "OPEN"         -> "Ouverte";
            case "IN_PROGRESS"  -> "En cours";
            case "PENDING_USER" -> "En attente de votre réponse";
            case "RESOLVED"     -> "Résolue";
            case "CLOSED"       -> "Clôturée";
            case "ESCALATED"    -> "Escaladée";
            default             -> status;
        };
    }

    private String priorityColor(String priority) {
        if (priority == null) return "#9ca3af";
        return switch (priority) {
            case "CRITICAL" -> "#b91c1c";
            case "HIGH"     -> "#d97706";
            case "MEDIUM"   -> "#2563eb";
            case "LOW"      -> "#059669";
            default         -> "#9ca3af";
        };
    }

}
