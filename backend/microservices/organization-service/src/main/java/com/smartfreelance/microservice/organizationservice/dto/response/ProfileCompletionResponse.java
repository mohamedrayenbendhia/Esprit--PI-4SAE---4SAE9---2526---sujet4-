package com.smartfreelance.microservice.organizationservice.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Score de complétude du profil d'une organisation.
 *
 * Critères et points associés :
 *  description        15 pts
 *  logoUrl            10 pts
 *  website            10 pts
 *  specialties ≥ 3   15 pts
 *  siret              10 pts
 *  location           10 pts
 *  portfolio ≥ 1 item 15 pts
 *  membre actif ≠ owner 15 pts
 *  ─────────────────────────
 *  Total              100 pts
 *
 * En dessous de 40 pts → badge INCOMPLETE, exclu de la recherche publique.
 */
@Data
@Builder
public class ProfileCompletionResponse {

    private String organizationId;
    private String organizationName;

    /** Score global 0–100 */
    private int score;

    /** true si le profil est jugé suffisant pour la recherche publique (score >= 40) */
    private boolean visibleInSearch;

    private Breakdown breakdown;

    @Data
    @Builder
    public static class Breakdown {
        private boolean hasDescription;   // 15 pts
        private boolean hasLogo;          // 10 pts
        private boolean hasWebsite;       // 10 pts
        private boolean hasSpecialties;   // 15 pts  (≥ 3)
        private boolean hasSiret;         // 10 pts
        private boolean hasLocation;      // 10 pts
        private boolean hasPortfolio;     // 15 pts  (≥ 1 item)
        private boolean hasTeamMember;    // 15 pts  (au moins 1 membre actif ≠ owner)

        /** Points manquants et conseils */
        private java.util.List<String> missingItems;
    }
}
