package com.smartfreelance.microservice.organizationservice.controller;

import com.smartfreelance.microservice.organizationservice.dto.response.TrustScoreResponse;
import com.smartfreelance.microservice.organizationservice.service.TrustScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations/{orgId}/trust-score")
@RequiredArgsConstructor
public class TrustScoreController {

    private final TrustScoreService trustScoreService;

    /**
     * GET /api/organizations/{orgId}/trust-score
     * Retourne le détail du TrustScore courant (lecture seule).
     * Accessible à tous (public — utile pour afficher le score sur le profil).
     */
    @GetMapping
    public ResponseEntity<TrustScoreResponse> getBreakdown(@PathVariable String orgId) {
        return ResponseEntity.ok(trustScoreService.getBreakdown(orgId));
    }

    /**
     * POST /api/organizations/{orgId}/trust-score/recompute
     * Force un recalcul manuel (admin uniquement).
     * Utile après une correction de données ou une migration.
     */
    @PostMapping("/recompute")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TrustScoreResponse> forceRecompute(@PathVariable String orgId,
                                                              Authentication auth) {
        trustScoreService.recompute(orgId);
        return ResponseEntity.ok(trustScoreService.getBreakdown(orgId));
    }
}
