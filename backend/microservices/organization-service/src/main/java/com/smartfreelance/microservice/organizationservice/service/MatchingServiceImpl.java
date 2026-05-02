package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.CollabOfferMatchingRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.MatchingRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.ScoredMatchingRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.*;
import com.smartfreelance.microservice.organizationservice.entity.CollabOffer;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.enums.CollabOfferStatus;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationStatus;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationVisibility;
import com.smartfreelance.microservice.organizationservice.repository.CollabApplicationRepository;
import com.smartfreelance.microservice.organizationservice.repository.CollabOfferRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Moteur de compatibilité Freelance ↔ Organisation / Offre de collaboration.
 *
 * ── Org-level (ScoredMatchingRequest) ────────────────────────────────────────
 *  Signal               Poids    Logique
 *  Skill overlap         40 pts  matchCount / requiredCount × 40
 *  Trust level           25 pts  trustLevel / 5 × 25
 *  Location match        15 pts  exact=15 | région=8 | non précisé=7 | aucun=0
 *  Type preference       10 pts  exact=10 | non précisé=5 | mismatch=0
 *  Avg rating            10 pts  avgRating / 5 × 10
 *
 * ── Offer-level (CollabOfferMatchingRequest) ──────────────────────────────────
 *  Signal               Poids    Logique
 *  Skill overlap         50 pts  matchCount / offerSkillCount × 50
 *  Freshness             20 pts  max(0, 1 − ageDays/90) × 20
 *  Org trust level       15 pts  trustLevel / 5 × 15
 *  Budget match          10 pts  offer.budget >= minBudget → 10 | non précisé → 5 | aucun → 0
 *  Location match         5 pts  exact=5 | région=3 | non précisé=2 | aucun=0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchingServiceImpl implements MatchingService {

    private static final double FRESHNESS_CAP_DAYS = 90.0;

    private final OrganizationRepository     orgRepo;
    private final CollabOfferRepository      collabOfferRepo;
    private final CollabApplicationRepository collabAppRepo;

    // ── Matching simple (rétrocompatibilité) ─────────────────────────────────

    @Override
    public List<OrganizationSummaryResponse> match(MatchingRequest request) {
        return orgRepo.findByStatusAndVisibility(OrganizationStatus.ACTIVE, OrganizationVisibility.PUBLIC).stream()
                .filter(o -> request.getPreferredType() == null || o.getType() == request.getPreferredType())
                .filter(o -> request.getPreferredSize() == null || o.getSize() == request.getPreferredSize())
                .filter(o -> request.getLocation() == null ||
                        (o.getLocation() != null && o.getLocation().equalsIgnoreCase(request.getLocation())))
                .filter(o -> request.getRequiredSkills() == null || request.getRequiredSkills().isEmpty() ||
                        o.getSpecialties().stream().anyMatch(s -> request.getRequiredSkills().contains(s)))
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    // ── Matching scoré — niveau organisation ─────────────────────────────────

    @Override
    public List<CompatibilityResult> matchWithScore(ScoredMatchingRequest req) {
        int limit = Math.min(req.getLimit() > 0 ? req.getLimit() : 20, 50);

        List<Organization> candidates = orgRepo
                .findByStatusAndVisibility(OrganizationStatus.ACTIVE, OrganizationVisibility.PUBLIC);

        // Filtre optionnel sur la taille (critère dur, pas scoré)
        if (req.getPreferredSize() != null) {
            candidates = candidates.stream()
                    .filter(o -> o.getSize() == req.getPreferredSize())
                    .collect(Collectors.toList());
        }

        return candidates.stream()
                .map(org -> scoreOrg(org, req))
                .filter(r -> r.getCompatibilityScore() >= req.getMinScore())
                .sorted(Comparator.comparingDouble(CompatibilityResult::getCompatibilityScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private CompatibilityResult scoreOrg(Organization org, ScoredMatchingRequest req) {

        List<String> freelancerSkills = normalize(req.getFreelancerSkills());
        List<String> orgSkills        = normalize(org.getSpecialties());

        // ── Signal 1 : Skill overlap (40 pts) ────────────────────────────────
        double skillScore;
        int matchedCount = 0;
        int requiredCount = freelancerSkills.size();

        if (freelancerSkills.isEmpty()) {
            skillScore = 20.0; // neutre
        } else {
            matchedCount = (int) freelancerSkills.stream()
                    .filter(orgSkills::contains)
                    .count();
            skillScore = ((double) matchedCount / requiredCount) * 40.0;
        }

        // ── Signal 2 : Trust level (25 pts) ──────────────────────────────────
        int trustLevel  = org.getTrustLevel() != null ? org.getTrustLevel() : 1;
        double trustScore = (trustLevel / 5.0) * 25.0;

        // ── Signal 3 : Location match (15 pts) ───────────────────────────────
        String locMatch;
        double locationScore;
        String freelancerLoc = req.getFreelancerLocation();

        if (freelancerLoc == null || freelancerLoc.isBlank()) {
            locMatch = "NOT_SPECIFIED"; locationScore = 7.0;
        } else if (org.getLocation() == null) {
            locMatch = "NONE"; locationScore = 0.0;
        } else if (org.getLocation().equalsIgnoreCase(freelancerLoc.trim())) {
            locMatch = "EXACT"; locationScore = 15.0;
        } else if (sameRegion(org.getLocation(), freelancerLoc)) {
            locMatch = "REGION"; locationScore = 8.0;
        } else {
            locMatch = "NONE"; locationScore = 0.0;
        }

        // ── Signal 4 : Type preference (10 pts) ──────────────────────────────
        String typeMatch;
        double typeScore;

        if (req.getPreferredType() == null) {
            typeMatch = "NOT_SPECIFIED"; typeScore = 5.0;
        } else if (org.getType() == req.getPreferredType()) {
            typeMatch = "EXACT"; typeScore = 10.0;
        } else {
            typeMatch = "NONE"; typeScore = 0.0;
        }

        // ── Signal 5 : Average rating (10 pts) ───────────────────────────────
        double avgRating  = org.getAverageRating() != null ? org.getAverageRating() : 0.0;
        double ratingScore = (avgRating / 5.0) * 10.0;

        double total = round(skillScore + trustScore + locationScore + typeScore + ratingScore);

        return CompatibilityResult.builder()
                .organization(toSummary(org))
                .compatibilityScore(total)
                .breakdown(CompatibilityResult.ScoreBreakdown.builder()
                        .skillScore(round(skillScore))
                        .matchedSkillCount(matchedCount)
                        .requiredSkillCount(requiredCount)
                        .trustScore(round(trustScore))
                        .trustLevel(trustLevel)
                        .locationScore(round(locationScore))
                        .locationMatch(locMatch)
                        .typeScore(round(typeScore))
                        .typeMatch(typeMatch)
                        .ratingScore(round(ratingScore))
                        .averageRating(round(avgRating))
                        .build())
                .build();
    }

    // ── Matching scoré — niveau offre de collaboration ────────────────────────

    @Override
    public List<CollabOfferMatchResult> matchCollabOffers(CollabOfferMatchingRequest req) {
        int limit = Math.min(req.getLimit() > 0 ? req.getLimit() : 20, 50);

        // Charge toutes les offres OPEN (on ne pagine pas, on trie par score)
        List<CollabOffer> openOffers = collabOfferRepo
                .findByStatus(CollabOfferStatus.OPEN, PageRequest.of(0, 500))
                .getContent();

        // Index des organisations pour éviter les requêtes N+1
        Set<String> orgIds = openOffers.stream()
                .map(CollabOffer::getOrganizationId)
                .collect(Collectors.toSet());
        Map<String, Organization> orgMap = orgRepo.findAllById(orgIds).stream()
                .collect(Collectors.toMap(Organization::getId, o -> o));

        return openOffers.stream()
                .map(offer -> scoreOffer(offer, orgMap.get(offer.getOrganizationId()), req))
                .filter(Objects::nonNull)
                .filter(r -> r.getCompatibilityScore() >= req.getMinScore())
                .sorted(Comparator.comparingDouble(CollabOfferMatchResult::getCompatibilityScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private CollabOfferMatchResult scoreOffer(CollabOffer offer, Organization org,
                                              CollabOfferMatchingRequest req) {
        if (org == null) return null;

        List<String> freelancerSkills = normalize(req.getFreelancerSkills());
        List<String> offerSkills      = normalize(offer.getRequiredSkills());

        // ── Signal 1 : Skill overlap (50 pts) ────────────────────────────────
        double skillScore;
        int matchedCount = 0;
        int offerSkillCount = offerSkills.size();

        if (freelancerSkills.isEmpty() || offerSkills.isEmpty()) {
            skillScore = 25.0; // neutre — on ne pénalise pas l'absence de données
        } else {
            matchedCount = (int) freelancerSkills.stream()
                    .filter(offerSkills::contains)
                    .count();
            skillScore = ((double) matchedCount / offerSkillCount) * 50.0;
        }

        // ── Signal 2 : Fraîcheur de l'offre (20 pts) ─────────────────────────
        long ageDays = offer.getCreatedAt() != null
                ? ChronoUnit.DAYS.between(offer.getCreatedAt(), LocalDateTime.now())
                : 0;
        double freshnessScore = Math.max(0.0, (1.0 - ageDays / FRESHNESS_CAP_DAYS)) * 20.0;

        // ── Signal 3 : TrustLevel de l'org (15 pts) ──────────────────────────
        int trustLevel  = org.getTrustLevel() != null ? org.getTrustLevel() : 1;
        double trustScore = (trustLevel / 5.0) * 15.0;

        // ── Signal 4 : Budget match (10 pts) ─────────────────────────────────
        String budgetMatch;
        double budgetScore;

        if (offer.getBudgetEstimate() == null || req.getMinBudget() == null) {
            budgetMatch = "NOT_SPECIFIED"; budgetScore = 5.0;
        } else if (offer.getBudgetEstimate() >= req.getMinBudget()) {
            budgetMatch = "MATCH"; budgetScore = 10.0;
        } else {
            budgetMatch = "NO_MATCH"; budgetScore = 0.0;
        }

        // ── Signal 5 : Location match (5 pts) ────────────────────────────────
        String locMatch;
        double locationScore;
        String freelancerLoc = req.getFreelancerLocation();
        String orgLoc        = org.getLocation();

        if (freelancerLoc == null || freelancerLoc.isBlank()) {
            locMatch = "NOT_SPECIFIED"; locationScore = 2.0;
        } else if (orgLoc == null) {
            locMatch = "NONE"; locationScore = 0.0;
        } else if (orgLoc.equalsIgnoreCase(freelancerLoc.trim())) {
            locMatch = "EXACT"; locationScore = 5.0;
        } else if (sameRegion(orgLoc, freelancerLoc)) {
            locMatch = "REGION"; locationScore = 3.0;
        } else {
            locMatch = "NONE"; locationScore = 0.0;
        }

        double total = round(skillScore + freshnessScore + trustScore + budgetScore + locationScore);

        // Reconstruit le CollabOfferResponse minimal pour l'affichage
        long accepted = collabAppRepo.countByOfferIdAndStatus(offer.getId(),
                com.smartfreelance.microservice.organizationservice.enums.CollabApplicationStatus.ACCEPTED);

        CollabOfferResponse offerResp = CollabOfferResponse.builder()
                .id(offer.getId())
                .organizationId(offer.getOrganizationId())
                .createdBy(offer.getCreatedBy())
                .title(offer.getTitle())
                .description(offer.getDescription())
                .requiredSkills(offer.getRequiredSkills())
                .durationLabel(offer.getDurationLabel())
                .budgetEstimate(offer.getBudgetEstimate())
                .maxApplicants(offer.getMaxApplicants())
                .deadlineDate(offer.getDeadlineDate())
                .status(offer.getStatus())
                .acceptedCount(accepted)
                .createdAt(offer.getCreatedAt())
                .updatedAt(offer.getUpdatedAt())
                .build();

        return CollabOfferMatchResult.builder()
                .offer(offerResp)
                .organizationName(org.getName())
                .organizationTrustLevel(trustLevel)
                .compatibilityScore(total)
                .breakdown(CollabOfferMatchResult.ScoreBreakdown.builder()
                        .skillScore(round(skillScore))
                        .matchedSkillCount(matchedCount)
                        .offerSkillCount(offerSkillCount)
                        .freshnessScore(round(freshnessScore))
                        .offerAgeDays(ageDays)
                        .trustScore(round(trustScore))
                        .budgetScore(round(budgetScore))
                        .budgetMatch(budgetMatch)
                        .locationScore(round(locationScore))
                        .locationMatch(locMatch)
                        .build())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Compare la "région" : premier mot de chaque localisation, insensible à la casse.
     * Ex: "Paris 15" et "Paris 10" → même région "Paris".
     */
    private boolean sameRegion(String loc1, String loc2) {
        String r1 = loc1.trim().split("\\s+")[0].toLowerCase();
        String r2 = loc2.trim().split("\\s+")[0].toLowerCase();
        return r1.equals(r2);
    }

    /** Normalise une liste de compétences : minuscules, trim, sans null. */
    private List<String> normalize(List<String> skills) {
        if (skills == null) return Collections.emptyList();
        return skills.stream()
                .filter(Objects::nonNull)
                .map(s -> s.trim().toLowerCase())
                .collect(Collectors.toList());
    }

    private double round(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private OrganizationSummaryResponse toSummary(Organization org) {
        return OrganizationSummaryResponse.builder()
                .id(org.getId()).name(org.getName()).logoUrl(org.getLogoUrl())
                .type(org.getType()).status(org.getStatus()).location(org.getLocation())
                .averageRating(org.getAverageRating()).size(org.getSize())
                .reviewCount(org.getReviewCount())
                .build();
    }
}
