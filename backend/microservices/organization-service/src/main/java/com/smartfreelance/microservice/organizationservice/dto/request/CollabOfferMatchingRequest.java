package com.smartfreelance.microservice.organizationservice.dto.request;

import lombok.Data;

import java.util.List;

/**
 * Requête de matching offres de collaboration × freelance.
 * Le freelance indique ses compétences et contraintes ;
 * l'algo retourne les offres OPEN triées par compatibilité.
 */
@Data
public class CollabOfferMatchingRequest {

    /** Compétences du freelance */
    private List<String> freelancerSkills;

    /** Localisation du freelance */
    private String freelancerLocation;

    /** Budget minimum attendu (facultatif — filtre souple) */
    private Double minBudget;

    /** Score minimum retenu (0–100, défaut 0). */
    private double minScore = 0;

    /** Nombre maximum de résultats (défaut 20, max 50). */
    private int limit = 20;
}
