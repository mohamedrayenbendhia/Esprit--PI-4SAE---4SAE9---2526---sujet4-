package com.smartfreelance.microservice.complaintservice.scheduler;

import com.smartfreelance.microservice.complaintservice.email.ComplaintEmailService;
import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.repository.ComplaintRepository;
import com.smartfreelance.microservice.complaintservice.service.SanctionService;
import com.smartfreelance.microservice.complaintservice.service.SlaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlaScheduler {

    private final SlaService          slaService;
    private final SanctionService     sanctionService;
    private final ComplaintRepository complaintRepository;
    private final ComplaintEmailService emailService;

    /** Vérifie les breaches SLA toutes les 30 minutes. */
    @Scheduled(fixedDelay = 1_800_000)
    public void checkBreaches() {
        log.debug("SLA breach check running...");
        slaService.processBreaches();
    }

    /** Expire les sanctions temporaires toutes les heures. */
    @Scheduled(fixedDelay = 3_600_000)
    public void expireSanctions() {
        int expired = sanctionService.expireOldSanctions();
        if (expired > 0) log.info("Expired {} sanctions.", expired);
    }

    /**
     * Digest quotidien pour les agents — chaque lundi–vendredi à 8h00.
     *
     * Pour chaque agent ayant des réclamations actives non résolues depuis > 24h,
     * envoie un email récapitulatif groupé par priorité.
     */
    @Scheduled(cron = "0 0 8 * * MON-FRI")
    public void sendAgentDailyDigest() {
        log.info("[Digest] Envoi des récapitulatifs quotidiens aux agents...");

        LocalDateTime threshold = LocalDateTime.now().minusHours(24);

        // Réclamations IN_PROGRESS ou PENDING_USER assignées depuis > 24h
        List<Complaint> stale = complaintRepository.findAll().stream()
                .filter(c -> c.getAssignedToId() != null
                          && c.getStatus() != Complaint.Status.RESOLVED
                          && c.getStatus() != Complaint.Status.CLOSED
                          && c.getUpdatedAt() != null
                          && c.getUpdatedAt().isBefore(threshold))
                .toList();

        if (stale.isEmpty()) {
            log.debug("[Digest] Aucune réclamation en retard — pas de digest envoyé.");
            return;
        }

        // Grouper par agent
        Map<String, List<Complaint>> byAgent = stale.stream()
                .collect(Collectors.groupingBy(Complaint::getAssignedToId));

        byAgent.forEach((agentId, complaints) -> {
            try {
                emailService.sendAgentDailyDigest(agentId, complaints);
                log.debug("[Digest] Email envoyé à l'agent {} ({} réclamations)", agentId, complaints.size());
            } catch (Exception e) {
                log.warn("[Digest] Erreur envoi à l'agent {} : {}", agentId, e.getMessage());
            }
        });

        log.info("[Digest] Digest envoyé à {} agent(s) pour {} réclamation(s) en retard.",
                byAgent.size(), stale.size());
    }
}
