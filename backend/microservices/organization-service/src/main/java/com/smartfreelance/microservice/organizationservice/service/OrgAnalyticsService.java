package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.response.OrgAnalyticsResponse;

public interface OrgAnalyticsService {

    /**
     * Calcule les métriques complètes d'une organisation.
     * Accessible uniquement aux OWNER et MANAGER.
     *
     * @param orgId     identifiant de l'organisation
     * @param requesterId identifiant de l'utilisateur qui demande le dashboard
     */
    OrgAnalyticsResponse getAnalytics(String orgId, String requesterId);
}
