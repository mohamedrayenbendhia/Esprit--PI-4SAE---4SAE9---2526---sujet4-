package com.smartfreelance.microservice.organizationservice.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Résultat de matching org × freelance.
 * Encapsule le résumé de l'organisation + le score de compatibilité détaillé.
 */
@Data
@Builder
public class CompatibilityResult {

    private OrganizationSummaryResponse organization;

    /** Score global 0–100 (trié décroissant dans la liste retournée) */
    private double compatibilityScore;

    private ScoreBreakdown breakdown;

    @Data
    @Builder
    public static class ScoreBreakdown {

        /** Compétences communes / compétences demandées × 40 → max 40 pts */
        private double skillScore;
        private int    matchedSkillCount;
        private int    requiredSkillCount;

        /** trustLevel / 5 × 25 → max 25 pts */
        private double trustScore;
        private int    trustLevel;

        /** Correspondance géographique → max 15 pts */
        private double locationScore;
        private String locationMatch; // "EXACT", "REGION", "NONE", "NOT_SPECIFIED"

        /** Correspondance de type (AGENCY, STARTUP…) → max 10 pts */
        private double typeScore;
        private String typeMatch;     // "EXACT", "NONE", "NOT_SPECIFIED"

        /** Note moyenne / 5 × 10 → max 10 pts */
        private double ratingScore;
        private double averageRating;
    }
}
