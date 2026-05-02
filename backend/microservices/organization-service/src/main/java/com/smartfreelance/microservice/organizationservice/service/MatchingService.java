package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.CollabOfferMatchingRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.MatchingRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.ScoredMatchingRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.CollabOfferMatchResult;
import com.smartfreelance.microservice.organizationservice.dto.response.CompatibilityResult;
import com.smartfreelance.microservice.organizationservice.dto.response.OrganizationSummaryResponse;

import java.util.List;

public interface MatchingService {

    /** Matching simple par filtres (rétrocompatibilité) */
    List<OrganizationSummaryResponse> match(MatchingRequest request);

    /** Matching scoré : retourne les organisations triées par compatibilité */
    List<CompatibilityResult> matchWithScore(ScoredMatchingRequest request);

    /** Matching offres de collaboration : retourne les offres OPEN triées par compatibilité */
    List<CollabOfferMatchResult> matchCollabOffers(CollabOfferMatchingRequest request);
}
