package com.smartfreelance.microservice.organizationservice.controller;

import com.smartfreelance.microservice.organizationservice.dto.response.OrgAnalyticsResponse;
import com.smartfreelance.microservice.organizationservice.service.OrgAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations/{orgId}/analytics")
@RequiredArgsConstructor
public class OrgAnalyticsController {

    private final OrgAnalyticsService analyticsService;

    /**
     * GET /api/organizations/{orgId}/analytics
     * Retourne le tableau de bord analytique complet.
     * Accès réservé aux OWNER et MANAGER (vérification dans le service).
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrgAnalyticsResponse> getAnalytics(
            @PathVariable String orgId,
            Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.ok(analyticsService.getAnalytics(orgId, userId));
    }
}
