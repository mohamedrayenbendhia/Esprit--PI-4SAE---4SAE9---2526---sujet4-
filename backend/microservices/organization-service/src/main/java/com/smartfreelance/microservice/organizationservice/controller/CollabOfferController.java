package com.smartfreelance.microservice.organizationservice.controller;

import com.smartfreelance.microservice.organizationservice.dto.request.ApplyCollabOfferRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.CreateCollabOfferRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.RespondCollabApplicationRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.CollabApplicationResponse;
import com.smartfreelance.microservice.organizationservice.dto.response.CollabOfferResponse;
import com.smartfreelance.microservice.organizationservice.service.CollabOfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints pour les offres de collaboration ponctuelle.
 *
 * Routes :
 *   POST   /api/organizations/{orgId}/collab-offers                   → créer une offre
 *   GET    /api/organizations/{orgId}/collab-offers                   → lister les offres (public = OPEN, managers = tout)
 *   GET    /api/organizations/{orgId}/collab-offers/{offerId}         → détail d'une offre
 *   POST   /api/organizations/{orgId}/collab-offers/{offerId}/close   → clôturer une offre
 *   POST   /api/organizations/{orgId}/collab-offers/{offerId}/cancel  → annuler une offre
 *
 *   POST   /api/collab-offers/{offerId}/apply                         → postuler
 *   GET    /api/collab-offers/{offerId}/applications                  → candidatures de l'offre (manager)
 *   POST   /api/collab-offers/applications/{applicationId}/respond    → répondre à une candidature
 *   DELETE /api/collab-offers/applications/{applicationId}/withdraw   → retirer sa candidature
 *   GET    /api/collab-offers/my-applications                         → mes candidatures
 */
@RestController
@RequiredArgsConstructor
public class CollabOfferController {

    private final CollabOfferService collabOfferService;

    // ── Gestion des offres (par organisation) ────────────────────────────────

    @PostMapping("/api/organizations/{orgId}/collab-offers")
    @PreAuthorize("hasRole('FREELANCE')")
    public ResponseEntity<CollabOfferResponse> createOffer(
            @PathVariable String orgId,
            @Valid @RequestBody CreateCollabOfferRequest request,
            Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(collabOfferService.createOffer(orgId, request, userId));
    }

    @GetMapping("/api/organizations/{orgId}/collab-offers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<CollabOfferResponse>> listOffers(
            @PathVariable String orgId,
            Pageable pageable,
            Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.ok(collabOfferService.getOrgOffers(orgId, pageable, userId));
    }

    @GetMapping("/api/organizations/{orgId}/collab-offers/{offerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CollabOfferResponse> getOffer(
            @PathVariable String orgId,
            @PathVariable String offerId) {
        return ResponseEntity.ok(collabOfferService.getOffer(offerId));
    }

    @PostMapping("/api/organizations/{orgId}/collab-offers/{offerId}/close")
    @PreAuthorize("hasRole('FREELANCE')")
    public ResponseEntity<CollabOfferResponse> closeOffer(
            @PathVariable String orgId,
            @PathVariable String offerId,
            Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.ok(collabOfferService.closeOffer(offerId, userId));
    }

    @PostMapping("/api/organizations/{orgId}/collab-offers/{offerId}/cancel")
    @PreAuthorize("hasRole('FREELANCE')")
    public ResponseEntity<CollabOfferResponse> cancelOffer(
            @PathVariable String orgId,
            @PathVariable String offerId,
            Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.ok(collabOfferService.cancelOffer(offerId, userId));
    }

    // ── Candidatures ──────────────────────────────────────────────────────────

    @PostMapping("/api/collab-offers/{offerId}/apply")
    @PreAuthorize("hasRole('FREELANCE')")
    public ResponseEntity<CollabApplicationResponse> apply(
            @PathVariable String offerId,
            @Valid @RequestBody ApplyCollabOfferRequest request,
            Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(collabOfferService.apply(offerId, request, userId));
    }

    @GetMapping("/api/collab-offers/{offerId}/applications")
    @PreAuthorize("hasRole('FREELANCE')")
    public ResponseEntity<Page<CollabApplicationResponse>> getApplicationsForOffer(
            @PathVariable String offerId,
            Pageable pageable,
            Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.ok(collabOfferService.getApplicationsForOffer(offerId, pageable, userId));
    }

    @PostMapping("/api/collab-offers/applications/{applicationId}/respond")
    @PreAuthorize("hasRole('FREELANCE')")
    public ResponseEntity<CollabApplicationResponse> respond(
            @PathVariable String applicationId,
            @Valid @RequestBody RespondCollabApplicationRequest request,
            Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.ok(collabOfferService.respond(applicationId, request, userId));
    }

    @DeleteMapping("/api/collab-offers/applications/{applicationId}/withdraw")
    @PreAuthorize("hasRole('FREELANCE')")
    public ResponseEntity<Void> withdraw(
            @PathVariable String applicationId,
            Authentication auth) {
        String userId = (String) auth.getDetails();
        collabOfferService.withdraw(applicationId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/collab-offers/my-applications")
    @PreAuthorize("hasRole('FREELANCE')")
    public ResponseEntity<List<CollabApplicationResponse>> myApplications(Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.ok(collabOfferService.getMyApplications(userId));
    }
}
