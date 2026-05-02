package com.smartfreelance.microservice.organizationservice.scheduler;

import com.smartfreelance.microservice.organizationservice.entity.AuditLog;
import com.smartfreelance.microservice.organizationservice.entity.CollabOffer;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.enums.CollabOfferStatus;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.notification.OrgNotificationService;
import com.smartfreelance.microservice.organizationservice.repository.AuditLogRepository;
import com.smartfreelance.microservice.organizationservice.repository.CollabOfferRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Job nocturne : expiration intelligente des offres de collaboration.
 *
 * Règles :
 *  1. Deadline dépassée          → CLOSED  (normale, silencieuse)
 *  2. OPEN depuis ≥ 90 j sans candidature → CLOSED + notification propriétaire
 *  3. Organisation SUSPENDED     → toutes ses offres OPEN → CANCELLED + audit
 *
 * Exécuté chaque nuit à 03 h 00 (après le job de dormance à 02 h 00).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CollabOfferExpiryScheduler {

    /** Nombre de jours sans candidature avant fermeture automatique. */
    private static final long STALE_DAYS = 90L;

    private static final String SYSTEM_USER = "SYSTEM";

    private final CollabOfferRepository  offerRepo;
    private final OrganizationRepository orgRepo;
    private final AuditLogRepository     auditRepo;
    private final OrgNotificationService notif;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void processExpiredOffers() {
        LocalDate today = LocalDate.now();
        LocalDateTime staleCutoff = LocalDateTime.now().minusDays(STALE_DAYS);

        int closedByDeadline = closeByDeadline(today);
        int closedByStale    = closeStaleOffers(staleCutoff);
        int cancelled        = cancelOffersOfSuspendedOrgs();

        log.info("[CollabExpiry] Job terminé — {} clôturées (deadline), {} clôturées (inactivité), {} annulées (org suspendue)",
                closedByDeadline, closedByStale, cancelled);
    }

    // ── Rule 1 : deadline dépassée ────────────────────────────────────────────

    private int closeByDeadline(LocalDate today) {
        List<CollabOffer> expired = offerRepo.findExpiredOpenOffers(today);
        for (CollabOffer offer : expired) {
            offer.setStatus(CollabOfferStatus.CLOSED);
            offerRepo.save(offer);
            audit(offer.getOrganizationId(), "OFFER_CLOSED_DEADLINE",
                    "Offre « " + offer.getTitle() + " » clôturée automatiquement (deadline dépassée).");
        }
        if (!expired.isEmpty()) {
            log.debug("[CollabExpiry] {} offres clôturées (deadline)", expired.size());
        }
        return expired.size();
    }

    // ── Rule 2 : aucune candidature depuis ≥ 90 jours ────────────────────────

    private int closeStaleOffers(LocalDateTime staleCutoff) {
        List<CollabOffer> stale = offerRepo.findStaleOffersWithNoApplications(staleCutoff);
        for (CollabOffer offer : stale) {
            offer.setStatus(CollabOfferStatus.CLOSED);
            offerRepo.save(offer);

            audit(offer.getOrganizationId(), "OFFER_CLOSED_NO_APPLICATIONS",
                    "Offre « " + offer.getTitle() + " » clôturée automatiquement (aucune candidature en " + STALE_DAYS + " jours).");

            // Notifier le propriétaire de l'organisation
            orgRepo.findById(offer.getOrganizationId()).ifPresent(org ->
                    notif.notifyCollabOfferAutoExpired(org.getOwnerId(), org.getName(), offer.getTitle())
            );
        }
        if (!stale.isEmpty()) {
            log.debug("[CollabExpiry] {} offres clôturées (inactivité)", stale.size());
        }
        return stale.size();
    }

    // ── Rule 3 : organisation suspendue ───────────────────────────────────────

    private int cancelOffersOfSuspendedOrgs() {
        List<CollabOffer> toCancel = offerRepo.findOpenOffersOfSuspendedOrgs();
        for (CollabOffer offer : toCancel) {
            offer.setStatus(CollabOfferStatus.CANCELLED);
            offerRepo.save(offer);
            audit(offer.getOrganizationId(), "OFFER_CANCELLED_ORG_SUSPENDED",
                    "Offre « " + offer.getTitle() + " » annulée car l'organisation est suspendue.");
        }
        if (!toCancel.isEmpty()) {
            log.warn("[CollabExpiry] {} offres annulées (organisations suspendues)", toCancel.size());
        }
        return toCancel.size();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void audit(String orgId, String action, String details) {
        auditRepo.save(AuditLog.builder()
                .organizationId(orgId)
                .performedByUserId(SYSTEM_USER)
                .action(action)
                .details(details)
                .build());
    }
}
