package com.smartfreelance.microservice.organizationservice.scheduler;

import com.smartfreelance.microservice.organizationservice.entity.AuditLog;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationStatus;
import com.smartfreelance.microservice.organizationservice.notification.OrgNotificationService;
import com.smartfreelance.microservice.organizationservice.repository.AuditLogRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Job nocturne : détection et traitement des organisations dormantes.
 *
 * Règles d'inactivité (basées sur le dernier audit log) :
 *  ≥ 60 jours → avertissement in-app + note admin
 *  ≥ 90 jours → passage en AWAITING_INFO + audit
 * ≥ 180 jours → suspension automatique + audit + notification
 *
 * Exécuté chaque nuit à 02 h 00.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrgDormancyScheduler {

    private static final long WARN_DAYS     = 60L;
    private static final long AWAITING_DAYS = 90L;
    private static final long SUSPEND_DAYS  = 180L;

    private static final String SYSTEM_USER = "SYSTEM";

    private final OrganizationRepository  orgRepo;
    private final AuditLogRepository      auditRepo;
    private final OrgNotificationService  notif;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void detectDormantOrgs() {
        LocalDateTime now = LocalDateTime.now();
        List<Organization> orgs = orgRepo.findAllNonDissolved();

        int warned = 0, awaiting = 0, suspended = 0;

        for (Organization org : orgs) {
            // Date de référence : dernier audit OU date de création
            LocalDateTime lastActivity = auditRepo.findLastActivityAt(org.getId())
                    .orElse(org.getCreatedAt());

            long inactiveDays = ChronoUnit.DAYS.between(lastActivity, now);

            if (inactiveDays >= SUSPEND_DAYS) {
                if (org.getStatus() != OrganizationStatus.SUSPENDED) {
                    suspend(org, inactiveDays, now);
                    suspended++;
                }
            } else if (inactiveDays >= AWAITING_DAYS) {
                if (org.getStatus() == OrganizationStatus.ACTIVE) {
                    markAwaitingInfo(org, inactiveDays, now);
                    awaiting++;
                }
            } else if (inactiveDays >= WARN_DAYS) {
                if (org.getStatus() == OrganizationStatus.ACTIVE) {
                    warnDormancy(org, inactiveDays);
                    warned++;
                }
            }
        }

        log.info("[Dormancy] Job terminé — {} avertis, {} en attente d'info, {} suspendus",
                warned, awaiting, suspended);
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private void warnDormancy(Organization org, long days) {
        // Note admin (non bloquant) + notification asynchrone
        String note = "Inactif depuis " + days + " jours (avertissement automatique).";
        appendAdminNote(org, note);
        orgRepo.save(org);

        notif.notifyDormancyWarning(org.getOwnerId(), org.getName(), days);
        log.debug("[Dormancy] Avertissement envoyé à {} ({} jours)", org.getId(), days);
    }

    private void markAwaitingInfo(Organization org, long days, LocalDateTime now) {
        org.setStatus(OrganizationStatus.AWAITING_INFO);
        String note = "Passage en AWAITING_INFO après " + days + " jours d'inactivité (" + now.toLocalDate() + ").";
        appendAdminNote(org, note);
        orgRepo.save(org);

        audit(org.getId(), "DORMANCY_AWAITING_INFO",
                "Organisation inactive depuis " + days + " jours → AWAITING_INFO.");

        notif.notifyDormancyWarning(org.getOwnerId(), org.getName(), days);
        log.info("[Dormancy] {} → AWAITING_INFO ({} jours)", org.getId(), days);
    }

    private void suspend(Organization org, long days, LocalDateTime now) {
        org.setStatus(OrganizationStatus.SUSPENDED);
        String note = "Suspendue automatiquement après " + days + " jours d'inactivité (" + now.toLocalDate() + ").";
        appendAdminNote(org, note);
        orgRepo.save(org);

        audit(org.getId(), "DORMANCY_AUTO_SUSPEND",
                "Organisation inactive depuis " + days + " jours → SUSPENDED automatiquement.");

        notif.notifyOrganizationSuspended(org.getOwnerId(), org.getName(),
                "Inactivité prolongée (" + days + " jours)");
        log.warn("[Dormancy] {} suspendue automatiquement ({} jours)", org.getId(), days);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void appendAdminNote(Organization org, String note) {
        String existing = org.getAdminNote();
        org.setAdminNote(existing == null ? note : existing + "\n" + note);
    }

    private void audit(String orgId, String action, String details) {
        auditRepo.save(AuditLog.builder()
                .organizationId(orgId)
                .performedByUserId(SYSTEM_USER)
                .action(action)
                .details(details)
                .build());
    }
}
