package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.response.TrustScoreResponse;

public interface TrustScoreService {

    /**
     * Recalcule le TrustScore de l'organisation, met à jour trustLevel,
     * recalcule tous les badges automatiques, et persiste le tout.
     * À appeler après chaque événement significatif (avis, RFQ, invitation…).
     */
    void recompute(String orgId);

    /**
     * Retourne le détail du score actuel sans modification (lecture seule).
     */
    TrustScoreResponse getBreakdown(String orgId);
}
