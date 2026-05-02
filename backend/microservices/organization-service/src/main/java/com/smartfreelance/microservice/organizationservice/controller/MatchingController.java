package com.smartfreelance.microservice.organizationservice.controller;

import com.smartfreelance.microservice.organizationservice.dto.request.CollabOfferMatchingRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.MatchingRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.ScoredMatchingRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.CollabOfferMatchResult;
import com.smartfreelance.microservice.organizationservice.dto.response.CompatibilityResult;
import com.smartfreelance.microservice.organizationservice.dto.response.OrganizationSummaryResponse;
import com.smartfreelance.microservice.organizationservice.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations/matching")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;

    /**
     * POST /api/organizations/matching
     * Matching simple par filtres — rétrocompatibilité.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrganizationSummaryResponse>> match(
            @RequestBody MatchingRequest request) {
        return ResponseEntity.ok(matchingService.match(request));
    }

    /**
     * POST /api/organizations/matching/scored
     * Matching scoré : le freelance fournit son profil, reçoit les orgs
     * triées par score de compatibilité (0–100) avec détail de chaque signal.
     */
    @PostMapping("/scored")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CompatibilityResult>> matchWithScore(
            @RequestBody ScoredMatchingRequest request) {
        return ResponseEntity.ok(matchingService.matchWithScore(request));
    }

    /**
     * POST /api/organizations/matching/collab-offers
     * Matching offres de collaboration : retourne les offres OPEN
     * triées par compatibilité avec le profil du freelance.
     */
    @PostMapping("/collab-offers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CollabOfferMatchResult>> matchCollabOffers(
            @RequestBody CollabOfferMatchingRequest request) {
        return ResponseEntity.ok(matchingService.matchCollabOffers(request));
    }
}
