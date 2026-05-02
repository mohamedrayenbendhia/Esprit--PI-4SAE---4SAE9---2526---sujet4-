package com.smartfreelance.microservice.organizationservice.controller;

import com.smartfreelance.microservice.organizationservice.dto.response.ProfileCompletionResponse;
import com.smartfreelance.microservice.organizationservice.service.ProfileCompletionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations/{orgId}/completion-score")
@RequiredArgsConstructor
public class ProfileCompletionController {

    private final ProfileCompletionService completionService;

    /**
     * GET /api/organizations/{orgId}/completion-score
     *
     * Retourne le score de complétude (0–100) avec la liste des critères manquants.
     * Accessible aux utilisateurs authentifiés.
     * Lecture seule — aucune persistance.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileCompletionResponse> getScore(@PathVariable String orgId) {
        return ResponseEntity.ok(completionService.compute(orgId));
    }
}
