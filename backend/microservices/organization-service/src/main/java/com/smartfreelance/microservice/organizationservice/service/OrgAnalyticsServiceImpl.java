package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.response.OrgAnalyticsResponse;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.enums.*;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrgAnalyticsServiceImpl implements OrgAnalyticsService {

    private final OrganizationRepository        orgRepo;
    private final OrganizationMemberRepository  memberRepo;
    private final OrgApplicationRepository      appRepo;
    private final OrganizationReviewRepository  reviewRepo;
    private final CollabOfferRepository         collabOfferRepo;
    private final CollabApplicationRepository   collabAppRepo;
    private final OrgRfqRepository              rfqRepo;
    private final InvitationRepository          invitationRepo;

    @Override
    public OrgAnalyticsResponse getAnalytics(String orgId, String requesterId) {

        Organization org = orgRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation introuvable : " + orgId));

        // Vérification accès : OWNER ou MANAGER uniquement
        boolean isOwner   = org.getOwnerId().equals(requesterId);
        boolean isManager = memberRepo.findByOrganizationIdAndUserId(orgId, requesterId)
                .map(m -> m.getRole() == MemberRole.MANAGER || m.getRole() == MemberRole.OWNER)
                .orElse(false);
        if (!isOwner && !isManager) {
            throw new BusinessRuleException("Accès refusé : seuls les propriétaires et gestionnaires peuvent consulter le dashboard.");
        }

        return OrgAnalyticsResponse.builder()
                .organizationId(orgId)
                .organizationName(org.getName())
                .averageRating(org.getAverageRating() != null ? org.getAverageRating() : 0.0)
                .trustLevel(org.getTrustLevel() != null ? org.getTrustLevel() : 1)
                .createdAt(org.getCreatedAt())
                .daysActive(computeDaysActive(org.getCreatedAt()))
                .members(computeMemberStats(orgId))
                .applications(computeApplicationStats(orgId))
                .reviews(computeReviewStats(orgId))
                .collab(computeCollabStats(orgId))
                .rfq(computeRfqStats(orgId))
                .invitations(computeInvitationStats(orgId))
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private long computeDaysActive(LocalDateTime createdAt) {
        if (createdAt == null) return 0;
        return ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());
    }

    private OrgAnalyticsResponse.MemberStats computeMemberStats(String orgId) {
        long active   = memberRepo.countByOrganizationIdAndStatus(orgId, MemberStatus.ACTIVE);
        long inactive = memberRepo.countByOrganizationIdAndStatus(orgId, MemberStatus.INACTIVE);
        long owners   = memberRepo.findByOrganizationIdAndRole(orgId, MemberRole.OWNER).size();
        long managers = memberRepo.findByOrganizationIdAndRole(orgId, MemberRole.MANAGER).size();
        long members  = memberRepo.findByOrganizationIdAndRole(orgId, MemberRole.MEMBER).size();

        return OrgAnalyticsResponse.MemberStats.builder()
                .total(active + inactive)
                .active(active)
                .inactive(inactive)
                .owners(owners)
                .managers(managers)
                .members(members)
                .build();
    }

    private OrgAnalyticsResponse.ApplicationStats computeApplicationStats(String orgId) {
        long total     = appRepo.countByOrganizationId(orgId);
        long pending   = appRepo.countByOrganizationIdAndStatus(orgId, ApplicationStatus.PENDING);
        long accepted  = appRepo.countByOrganizationIdAndStatus(orgId, ApplicationStatus.ACCEPTED);
        long rejected  = appRepo.countByOrganizationIdAndStatus(orgId, ApplicationStatus.REJECTED);
        long withdrawn = appRepo.countByOrganizationIdAndStatus(orgId, ApplicationStatus.WITHDRAWN);

        long decided = accepted + rejected;
        Double rate = decided > 0 ? Math.round((accepted * 100.0 / decided) * 10) / 10.0 : null;

        return OrgAnalyticsResponse.ApplicationStats.builder()
                .total(total)
                .pending(pending)
                .accepted(accepted)
                .rejected(rejected)
                .withdrawn(withdrawn)
                .acceptanceRate(rate)
                .build();
    }

    private OrgAnalyticsResponse.ReviewStats computeReviewStats(String orgId) {
        long   total     = reviewRepo.countByOrganizationId(orgId);
        double average   = reviewRepo.findAverageRatingByOrganizationId(orgId).orElse(0.0);
        long   withReply = reviewRepo.countByOrganizationIdAndReplyIsNotNull(orgId);
        long   reported  = reviewRepo.countByOrganizationIdAndReported(orgId, true);

        // Une seule requête GROUP BY au lieu de 5 requêtes séparées
        Map<Integer, Long> distribution = new java.util.HashMap<>(Map.of(1L, 0L, 2L, 0L, 3L, 0L, 4L, 0L, 5L, 0L))
                .entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(e -> e.getKey().intValue(), java.util.Map.Entry::getValue));
        reviewRepo.findRatingDistribution(orgId)
                .forEach(row -> distribution.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue()));

        return OrgAnalyticsResponse.ReviewStats.builder()
                .total(total)
                .average(Math.round(average * 10) / 10.0)
                .withReply(withReply)
                .reported(reported)
                .distribution(distribution)
                .build();
    }

    private OrgAnalyticsResponse.CollabStats computeCollabStats(String orgId) {
        long totalOffers       = collabOfferRepo.countByOrganizationId(orgId);
        long openOffers        = collabOfferRepo.countByOrganizationIdAndStatus(orgId, CollabOfferStatus.OPEN);
        long closedOffers      = collabOfferRepo.countByOrganizationIdAndStatus(orgId, CollabOfferStatus.CLOSED);
        long cancelledOffers   = collabOfferRepo.countByOrganizationIdAndStatus(orgId, CollabOfferStatus.CANCELLED);
        long totalApplications = collabAppRepo.countByOrganizationId(orgId);
        long accepted          = collabAppRepo.countByOrganizationIdAndStatus(orgId, CollabApplicationStatus.ACCEPTED);
        long pendingApps       = collabAppRepo.countByOrganizationIdAndStatus(orgId, CollabApplicationStatus.PENDING);

        long decided = accepted + collabAppRepo.countByOrganizationIdAndStatus(orgId, CollabApplicationStatus.REJECTED);
        Double rate  = decided > 0 ? Math.round((accepted * 100.0 / decided) * 10) / 10.0 : null;

        return OrgAnalyticsResponse.CollabStats.builder()
                .totalOffers(totalOffers)
                .openOffers(openOffers)
                .closedOffers(closedOffers)
                .cancelledOffers(cancelledOffers)
                .totalApplications(totalApplications)
                .acceptedApplications(accepted)
                .pendingApplications(pendingApps)
                .applicationAcceptanceRate(rate)
                .build();
    }

    private OrgAnalyticsResponse.RfqStats computeRfqStats(String orgId) {
        long total     = rfqRepo.countByOrganizationId(orgId);
        long pending   = rfqRepo.countByOrganizationIdAndStatus(orgId, RfqStatus.PENDING);
        long responded = rfqRepo.countByOrganizationIdAndStatus(orgId, RfqStatus.RESPONDED);
        long closed    = rfqRepo.countByOrganizationIdAndStatus(orgId, RfqStatus.CLOSED);

        Double rate = total > 0 ? Math.round(((responded + closed) * 100.0 / total) * 10) / 10.0 : null;

        return OrgAnalyticsResponse.RfqStats.builder()
                .total(total)
                .pending(pending)
                .responded(responded)
                .closed(closed)
                .responseRate(rate)
                .build();
    }

    private OrgAnalyticsResponse.InvitationStats computeInvitationStats(String orgId) {
        long total     = invitationRepo.countByOrganizationId(orgId);
        long pending   = invitationRepo.countByOrganizationIdAndStatus(orgId, InvitationStatus.PENDING);
        long accepted  = invitationRepo.countByOrganizationIdAndStatus(orgId, InvitationStatus.ACCEPTED);
        long declined  = invitationRepo.countByOrganizationIdAndStatus(orgId, InvitationStatus.DECLINED);
        long expired   = invitationRepo.countByOrganizationIdAndStatus(orgId, InvitationStatus.EXPIRED);
        long cancelled = invitationRepo.countByOrganizationIdAndStatus(orgId, InvitationStatus.CANCELLED);

        long decided = accepted + declined;
        Double rate  = decided > 0 ? Math.round((accepted * 100.0 / decided) * 10) / 10.0 : null;

        return OrgAnalyticsResponse.InvitationStats.builder()
                .total(total)
                .pending(pending)
                .accepted(accepted)
                .declined(declined)
                .expired(expired)
                .cancelled(cancelled)
                .acceptanceRate(rate)
                .build();
    }
}
