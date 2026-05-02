package com.smartfreelance.microservice.organizationservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Réponse complète du dashboard analytique d'une organisation.
 * Accessible uniquement aux OWNER et MANAGER.
 */
@Data
@Builder
public class OrgAnalyticsResponse {

    private String organizationId;
    private String organizationName;

    // ── Snapshot global ────────────────────────────────────────────────────
    private double averageRating;
    private int    trustLevel;
    private LocalDateTime createdAt;
    /** Nombre de jours depuis la création de l'organisation */
    private long daysActive;

    // ── Membres ────────────────────────────────────────────────────────────
    private MemberStats members;

    // ── Candidatures spontanées ────────────────────────────────────────────
    private ApplicationStats applications;

    // ── Avis ───────────────────────────────────────────────────────────────
    private ReviewStats reviews;

    // ── Offres de collaboration ────────────────────────────────────────────
    private CollabStats collab;

    // ── Demandes de devis (RFQ) ────────────────────────────────────────────
    private RfqStats rfq;

    // ── Invitations envoyées ───────────────────────────────────────────────
    private InvitationStats invitations;

    // ──────────────────────────────────────────────────────────────────────
    // Sous-objets
    // ──────────────────────────────────────────────────────────────────────

    @Data @Builder
    public static class MemberStats {
        private long total;
        private long active;
        private long inactive;
        private long owners;
        private long managers;
        private long members;
    }

    @Data @Builder
    public static class ApplicationStats {
        private long total;
        private long pending;
        private long accepted;
        private long rejected;
        private long withdrawn;
        /** Taux d'acceptation en % (0–100), null si aucune candidature */
        private Double acceptanceRate;
    }

    @Data @Builder
    public static class ReviewStats {
        private long   total;
        private double average;
        private long   withReply;    // avis auxquels l'org a répondu
        private long   reported;     // avis signalés
        /** Distribution : clé = note (1-5), valeur = nombre d'avis */
        private Map<Integer, Long> distribution;
    }

    @Data @Builder
    public static class CollabStats {
        private long totalOffers;
        private long openOffers;
        private long closedOffers;
        private long cancelledOffers;
        private long totalApplications;
        private long acceptedApplications;
        private long pendingApplications;
        /** Taux d'acceptation des candidatures collab en % */
        private Double applicationAcceptanceRate;
    }

    @Data @Builder
    public static class RfqStats {
        private long total;
        private long pending;
        private long responded;
        private long closed;
        /** Taux de réponse en % */
        private Double responseRate;
    }

    @Data @Builder
    public static class InvitationStats {
        private long total;
        private long pending;
        private long accepted;
        private long declined;
        private long expired;
        private long cancelled;
        /** Taux d'acceptation des invitations en % */
        private Double acceptanceRate;
    }
}
