package com.smartfreelance.microservice.organizationservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfreelance.microservice.organizationservice.dto.response.TrustScoreResponse;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.enums.InvitationStatus;
import com.smartfreelance.microservice.organizationservice.enums.RfqStatus;
import com.smartfreelance.microservice.organizationservice.enums.TrustBadge;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Moteur de calcul du TrustScore.
 *
 * ┌─────────────────────────────────────────────────────────┐
 * │  Signal               Poids   Source                    │
 * ├─────────────────────────────────────────────────────────┤
 * │  Note moyenne (avis)   35 %   OrganizationReview        │
 * │  Taux réponse RFQ      20 %   OrgRfq                    │
 * │  Taux réponse avis     15 %   OrganizationReview.reply  │
 * │  Maturité (ancienneté) 10 %   Organization.createdAt    │
 * │  Taux acc. invitations 10 %   Invitation                │
 * │  Projets complétés     10 %   Organization.completedP.  │
 * └─────────────────────────────────────────────────────────┘
 *
 * Score 0–100  →  trustLevel 1–5 :
 *   >= 80 → 5 | >= 60 → 4 | >= 40 → 3 | >= 20 → 2 | else → 1
 *
 * Badges auto-recalculés (VERIFIED reste manuel) :
 *   TOP_RATED      : avgRating >= 4.5 ET reviewCount >= 10
 *   EXPERIENCED    : completedProjectsCount >= 10
 *   FAST_RESPONDER : rfqResponseRate >= 80 % ET rfqTotal >= 3
 *   PREMIUM        : trustLevel == 5
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TrustScoreServiceImpl implements TrustScoreService {

    // ── Pondérations ──────────────────────────────────────────────────────────
    private static final double W_RATING     = 35.0;
    private static final double W_RFQ        = 20.0;
    private static final double W_REPLY      = 15.0;
    private static final double W_MATURITY   = 10.0;
    private static final double W_INVITATION = 10.0;
    private static final double W_PROJECTS   = 10.0;

    /** Plafond : au-delà de 365 jours la maturité est maximale */
    private static final double MATURITY_CAP_DAYS = 365.0;
    /** Plafond : au-delà de 20 projets le signal projets est maximal */
    private static final double PROJECTS_CAP = 20.0;
    /** Minimum de RFQ pour que le signal soit pris en compte */
    private static final long   RFQ_MIN_COUNT = 3;

    private final OrganizationRepository        orgRepo;
    private final OrganizationReviewRepository  reviewRepo;
    private final OrgRfqRepository              rfqRepo;
    private final InvitationRepository          invitationRepo;
    private final ObjectMapper                  objectMapper;

    // ── API publique ──────────────────────────────────────────────────────────

    @Override
    public void recompute(String orgId) {
        orgRepo.findById(orgId).ifPresent(org -> {
            Signals s      = collectSignals(org);
            double  score  = computeScore(s);
            int     level  = scoreToLevel(score);

            org.setAverageRating(s.avgRating);
            org.setReviewCount((int) s.reviewCount);
            org.setTrustLevel(level);
            org.setBadges(recomputeBadges(org, s, level));
            orgRepo.save(org);

            log.debug("[TrustScore] org={} score={} level={}", orgId, score, level);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public TrustScoreResponse getBreakdown(String orgId) {
        Organization org = orgRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation introuvable : " + orgId));

        Signals s     = collectSignals(org);
        double  score = computeScore(s);
        int     level = scoreToLevel(score);

        return TrustScoreResponse.builder()
                .organizationId(orgId)
                .organizationName(org.getName())
                .globalScore(Math.round(score * 10) / 10.0)
                .trustLevel(level)
                .badges(parseBadges(org.getBadges()))
                .breakdown(TrustScoreResponse.SignalBreakdown.builder()
                        // ── Signal 1 : note moyenne ─────────────────────────
                        .ratingScore(Math.round(s.ratingContrib * 10) / 10.0)
                        .averageRating(Math.round(s.avgRating * 10) / 10.0)
                        .reviewCount((int) s.reviewCount)
                        // ── Signal 2 : taux réponse RFQ ─────────────────────
                        .rfqScore(Math.round(s.rfqContrib * 10) / 10.0)
                        .rfqResponseRate(Math.round(s.rfqResponseRate * 1000) / 10.0) // en %
                        .rfqTotal(s.rfqTotal)
                        // ── Signal 3 : taux réponse avis ────────────────────
                        .replyScore(Math.round(s.replyContrib * 10) / 10.0)
                        .reviewReplyRate(Math.round(s.replyRate * 1000) / 10.0)       // en %
                        .reviewsWithReply(s.reviewsWithReply)
                        // ── Signal 4 : maturité ──────────────────────────────
                        .maturityScore(Math.round(s.maturityContrib * 10) / 10.0)
                        .daysActive(s.daysActive)
                        // ── Signal 5 : taux acc. invitations ────────────────
                        .invitationScore(Math.round(s.invitationContrib * 10) / 10.0)
                        .invitationAcceptanceRate(Math.round(s.invitationAccRate * 1000) / 10.0)
                        .invitationDecided(s.invitationDecided)
                        // ── Signal 6 : projets complétés ────────────────────
                        .projectScore(Math.round(s.projectContrib * 10) / 10.0)
                        .completedProjectsCount(s.completedProjects)
                        .build())
                .build();
    }

    // ── Collecte des données brutes ───────────────────────────────────────────

    private Signals collectSignals(Organization org) {
        String orgId = org.getId();
        Signals s    = new Signals();

        // Signal 1 — Note moyenne
        s.avgRating         = reviewRepo.findAverageRatingByOrganizationId(orgId).orElse(0.0);
        s.reviewCount       = reviewRepo.countByOrganizationId(orgId);
        s.reviewsWithReply  = reviewRepo.countByOrganizationIdAndReplyIsNotNull(orgId);

        // Signal 2 — Taux réponse RFQ
        s.rfqTotal          = rfqRepo.countByOrganizationId(orgId);
        long rfqAnswered    = rfqRepo.countByOrganizationIdAndStatus(orgId, RfqStatus.RESPONDED)
                            + rfqRepo.countByOrganizationIdAndStatus(orgId, RfqStatus.CLOSED);
        s.rfqResponseRate   = (s.rfqTotal > 0) ? (double) rfqAnswered / s.rfqTotal : 0.0;

        // Signal 3 — Taux réponse aux avis
        s.replyRate         = (s.reviewCount > 0) ? (double) s.reviewsWithReply / s.reviewCount : 0.0;

        // Signal 4 — Maturité
        LocalDateTime createdAt = org.getCreatedAt();
        s.daysActive        = (createdAt != null) ? ChronoUnit.DAYS.between(createdAt, LocalDateTime.now()) : 0;

        // Signal 5 — Taux acceptation invitations
        long invAccepted    = invitationRepo.countByOrganizationIdAndStatus(orgId, InvitationStatus.ACCEPTED);
        long invDeclined    = invitationRepo.countByOrganizationIdAndStatus(orgId, InvitationStatus.DECLINED);
        s.invitationDecided = invAccepted + invDeclined;
        s.invitationAccRate = (s.invitationDecided > 0) ? (double) invAccepted / s.invitationDecided : 0.0;

        // Signal 6 — Projets complétés
        s.completedProjects = org.getCompletedProjectsCount() != null ? org.getCompletedProjectsCount() : 0;

        return s;
    }

    // ── Calcul du score ───────────────────────────────────────────────────────

    private double computeScore(Signals s) {

        // Signal 1 : note moyenne — seulement si au moins 1 avis
        s.ratingContrib = (s.reviewCount > 0)
                ? (s.avgRating / 5.0) * W_RATING
                : 0.0;

        // Signal 2 : RFQ — seulement si seuil minimum atteint (évite les faux 100 %)
        s.rfqContrib = (s.rfqTotal >= RFQ_MIN_COUNT)
                ? s.rfqResponseRate * W_RFQ
                : (s.rfqTotal > 0 ? s.rfqResponseRate * W_RFQ * 0.5 : 0.0); // demi-poids sous le seuil

        // Signal 3 : réponses aux avis
        s.replyContrib = (s.reviewCount > 0)
                ? s.replyRate * W_REPLY
                : 0.0;

        // Signal 4 : maturité (plafonnée à 365 jours)
        double maturityRatio = Math.min(s.daysActive / MATURITY_CAP_DAYS, 1.0);
        s.maturityContrib    = maturityRatio * W_MATURITY;

        // Signal 5 : invitations
        s.invitationContrib = (s.invitationDecided > 0)
                ? s.invitationAccRate * W_INVITATION
                : 0.0;

        // Signal 6 : projets (plafonnés à 20)
        double projectRatio = Math.min(s.completedProjects / PROJECTS_CAP, 1.0);
        s.projectContrib    = projectRatio * W_PROJECTS;

        return s.ratingContrib + s.rfqContrib + s.replyContrib
             + s.maturityContrib + s.invitationContrib + s.projectContrib;
    }

    // ── Conversion score → niveau ─────────────────────────────────────────────

    private int scoreToLevel(double score) {
        if (score >= 80) return 5;
        if (score >= 60) return 4;
        if (score >= 40) return 3;
        if (score >= 20) return 2;
        return 1;
    }

    // ── Recalcul des badges automatiques ─────────────────────────────────────

    @SneakyThrows
    private String recomputeBadges(Organization org, Signals s, int newLevel) {
        List<TrustBadge> badges = parseBadges(org.getBadges());

        // TOP_RATED — note moyenne >= 4.5 ET au moins 10 avis
        setbadge(badges, TrustBadge.TOP_RATED,
                s.avgRating >= 4.5 && s.reviewCount >= 10);

        // EXPERIENCED — au moins 10 projets complétés
        setbadge(badges, TrustBadge.EXPERIENCED,
                s.completedProjects >= 10);

        // FAST_RESPONDER — taux de réponse aux RFQ >= 80 % ET au moins 3 RFQ reçues
        setbadge(badges, TrustBadge.FAST_RESPONDER,
                s.rfqTotal >= RFQ_MIN_COUNT && s.rfqResponseRate >= 0.80);

        // PREMIUM — niveau de confiance maximal (5)
        setbadge(badges, TrustBadge.PREMIUM,
                newLevel == 5);

        // VERIFIED reste exclusivement manuel (admin) — on ne le touche pas

        return objectMapper.writeValueAsString(badges);
    }

    /** Ajoute ou retire un badge de la liste selon la condition. */
    private void setbadge(List<TrustBadge> badges, TrustBadge badge, boolean eligible) {
        if (eligible && !badges.contains(badge)) {
            badges.add(badge);
        } else if (!eligible) {
            badges.remove(badge);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @SneakyThrows
    private List<TrustBadge> parseBadges(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        return objectMapper.readValue(json, new TypeReference<List<TrustBadge>>() {});
    }

    /** Conteneur interne pour les données brutes collectées + contributions calculées. */
    private static class Signals {
        // Signal 1
        double avgRating; long reviewCount; long reviewsWithReply;
        // Signal 2
        long rfqTotal; double rfqResponseRate;
        // Signal 3
        double replyRate;
        // Signal 4
        long daysActive;
        // Signal 5
        long invitationDecided; double invitationAccRate;
        // Signal 6
        int completedProjects;
        // Contributions calculées (renseignées par computeScore)
        double ratingContrib, rfqContrib, replyContrib,
               maturityContrib, invitationContrib, projectContrib;
    }
}
