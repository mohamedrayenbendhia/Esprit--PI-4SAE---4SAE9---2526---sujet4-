package com.smartfreelance.microservice.organizationservice.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Résultat de matching offre de collaboration × freelance.
 */
@Data
@Builder
public class CollabOfferMatchResult {

    private CollabOfferResponse offer;

    /** Nom de l'organisation pour l'affichage */
    private String organizationName;

    /** TrustLevel de l'organisation (1–5) */
    private int organizationTrustLevel;

    /** Score global 0–100 */
    private double compatibilityScore;

    private ScoreBreakdown breakdown;

    @Data
    @Builder
    public static class ScoreBreakdown {

        /** Correspondance compétences freelance × compétences requises → max 50 pts */
        private double skillScore;
        private int    matchedSkillCount;
        private int    offerSkillCount;

        /** Fraîcheur de l'offre (pénalise les offres > 90 jours) → max 20 pts */
        private double freshnessScore;
        private long   offerAgeDays;

        /** TrustLevel de l'organisation → max 15 pts */
        private double trustScore;

        /** Budget de l'offre ≥ budget minimum du freelance → max 10 pts */
        private double budgetScore;
        private String budgetMatch; // "MATCH", "NO_MATCH", "NOT_SPECIFIED"

        /** Correspondance géographique → max 5 pts */
        private double locationScore;
        private String locationMatch; // "EXACT", "REGION", "NONE", "NOT_SPECIFIED"
    }
}
