package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.ApplyCollabOfferRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.CreateCollabOfferRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.RespondCollabApplicationRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.CollabApplicationResponse;
import com.smartfreelance.microservice.organizationservice.dto.response.CollabOfferResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CollabOfferService {

    // ── Offres ────────────────────────────────────────────────────────────────

    /** Création d'une offre (OWNER ou MANAGER uniquement) */
    CollabOfferResponse createOffer(String orgId, CreateCollabOfferRequest request, String creatorId);

    /** Détail d'une offre */
    CollabOfferResponse getOffer(String offerId);

    /** Toutes les offres d'une organisation (managers : toutes statuts) */
    Page<CollabOfferResponse> getOrgOffers(String orgId, Pageable pageable, String requesterId);

    /** Offres publiques ouvertes d'une organisation (vue profil public) */
    Page<CollabOfferResponse> getPublicOffers(String orgId, Pageable pageable);

    /** Clôture ou annulation d'une offre (OWNER ou MANAGER) */
    CollabOfferResponse closeOffer(String offerId, String actorId);
    CollabOfferResponse cancelOffer(String offerId, String actorId);

    // ── Candidatures ──────────────────────────────────────────────────────────

    /** Un freelance postule à une offre */
    CollabApplicationResponse apply(String offerId, ApplyCollabOfferRequest request, String applicantId);

    /** L'organisation répond à une candidature (OWNER ou MANAGER) */
    CollabApplicationResponse respond(String applicationId, RespondCollabApplicationRequest request, String responderId);

    /** Retrait de candidature par le candidat */
    void withdraw(String applicationId, String applicantId);

    /** Candidatures pour une offre (vue manager) */
    Page<CollabApplicationResponse> getApplicationsForOffer(String offerId, Pageable pageable, String requesterId);

    /** Toutes mes candidatures (vue freelance) */
    List<CollabApplicationResponse> getMyApplications(String applicantId);
}
