package com.smartfreelance.microservice.organizationservice.dto.response;

import com.smartfreelance.microservice.organizationservice.enums.TrustBadge;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Détail complet du TrustScore d'une organisation.
 * Expose chaque signal et sa contribution au score global.
 */
@Data
@Builder
public class TrustScoreResponse {

    private String  organizationId;
    private String  organizationName;

    /** Score global 0–100 */
    private double  globalScore;

    /** Niveau de confiance 1–5 (dérivé du score global) */
    private int     trustLevel;

    /** Badges actifs après recalcul */
    private List<TrustBadge> badges;

    /** Détail de chaque signal */
    private SignalBreakdown breakdown;

    @Data
    @Builder
    public static class SignalBreakdown {

        /** Note moyenne (0–5) × poids 35 % → contribution max 35 pts */
        private double ratingScore;
        private double averageRating;
        private int    reviewCount;

        /** Taux de réponse aux RFQ × poids 20 % → contribution max 20 pts */
        private double rfqScore;
        private double rfqResponseRate;
        private long   rfqTotal;

        /** Taux de réponse aux avis × poids 15 % → contribution max 15 pts */
        private double replyScore;
        private double reviewReplyRate;
        private long   reviewsWithReply;

        /** Maturité de l'org (ancienneté plafonnée à 1 an) × poids 10 % → max 10 pts */
        private double maturityScore;
        private long   daysActive;

        /** Taux d'acceptation des invitations × poids 10 % → max 10 pts */
        private double invitationScore;
        private double invitationAcceptanceRate;
        private long   invitationDecided;

        /** Projets complétés (plafonnés à 20) × poids 10 % → max 10 pts */
        private double projectScore;
        private int    completedProjectsCount;
    }
}
