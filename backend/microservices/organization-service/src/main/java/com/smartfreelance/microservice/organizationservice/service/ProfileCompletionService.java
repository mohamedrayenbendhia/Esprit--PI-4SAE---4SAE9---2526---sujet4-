package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.response.ProfileCompletionResponse;
import com.smartfreelance.microservice.organizationservice.entity.Organization;

public interface ProfileCompletionService {

    /**
     * Calcule le score de complétude (0–100) de l'organisation et retourne le détail.
     * Ne persiste rien — lecture seule.
     */
    ProfileCompletionResponse compute(String orgId);

    /**
     * Calcule le score à partir d'une entité déjà chargée (pour les appels internes).
     * Évite un aller-retour en base quand l'org est déjà disponible.
     */
    int computeScore(Organization org);
}
