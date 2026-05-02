package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.response.OrgAnalyticsResponse;
import com.smartfreelance.microservice.organizationservice.entity.OrganizationMember;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.enums.*;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrgAnalyticsServiceImpl Unit Tests")
class OrgAnalyticsServiceImplTest {

    @Mock private OrganizationRepository        orgRepo;
    @Mock private OrganizationMemberRepository  memberRepo;
    @Mock private OrgApplicationRepository      appRepo;
    @Mock private OrganizationReviewRepository  reviewRepo;
    @Mock private CollabOfferRepository         collabOfferRepo;
    @Mock private CollabApplicationRepository   collabAppRepo;
    @Mock private OrgRfqRepository              rfqRepo;
    @Mock private InvitationRepository          invitationRepo;

    @InjectMocks private OrgAnalyticsServiceImpl analyticsService;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Organization buildOrg(String orgId, String ownerId) {
        return Organization.builder()
                .id(orgId).name("Acme Corp").ownerId(ownerId)
                .status(OrganizationStatus.ACTIVE)
                .averageRating(4.5).trustLevel(3)
                .createdAt(LocalDateTime.now().minusDays(30))
                .build();
    }

    /** Stubs all counting repositories with zeros for a given orgId. */
    private void stubAllCountsToZero(String orgId) {
        when(memberRepo.countByOrganizationIdAndStatus(eq(orgId), any(MemberStatus.class))).thenReturn(0L);
        when(memberRepo.findByOrganizationIdAndRole(eq(orgId), any(MemberRole.class))).thenReturn(List.of());

        when(appRepo.countByOrganizationId(orgId)).thenReturn(0L);
        when(appRepo.countByOrganizationIdAndStatus(eq(orgId), any(ApplicationStatus.class))).thenReturn(0L);

        when(reviewRepo.countByOrganizationId(orgId)).thenReturn(0L);
        when(reviewRepo.findAverageRatingByOrganizationId(orgId)).thenReturn(Optional.of(0.0));
        when(reviewRepo.countByOrganizationIdAndReplyIsNotNull(orgId)).thenReturn(0L);
        when(reviewRepo.countByOrganizationIdAndReported(orgId, true)).thenReturn(0L);
        when(reviewRepo.findRatingDistribution(orgId)).thenReturn(Collections.emptyList());

        when(collabOfferRepo.countByOrganizationId(orgId)).thenReturn(0L);
        when(collabOfferRepo.countByOrganizationIdAndStatus(eq(orgId), any(CollabOfferStatus.class))).thenReturn(0L);
        when(collabAppRepo.countByOrganizationId(orgId)).thenReturn(0L);
        when(collabAppRepo.countByOrganizationIdAndStatus(eq(orgId), any(CollabApplicationStatus.class))).thenReturn(0L);

        when(rfqRepo.countByOrganizationId(orgId)).thenReturn(0L);
        when(rfqRepo.countByOrganizationIdAndStatus(eq(orgId), any(RfqStatus.class))).thenReturn(0L);

        when(invitationRepo.countByOrganizationId(orgId)).thenReturn(0L);
        when(invitationRepo.countByOrganizationIdAndStatus(eq(orgId), any(InvitationStatus.class))).thenReturn(0L);
    }

    // ── getAnalytics() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAnalytics_ownerRequests_returnsFullDashboard")
    void getAnalytics_ownerRequests_returnsFullDashboard() {
        String orgId = "org-1";
        String ownerId = "owner-1";

        Organization org = buildOrg(orgId, ownerId);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(memberRepo.findByOrganizationIdAndUserId(orgId, ownerId)).thenReturn(Optional.empty());
        stubAllCountsToZero(orgId);

        OrgAnalyticsResponse response = analyticsService.getAnalytics(orgId, ownerId);

        assertThat(response).isNotNull();
        assertThat(response.getOrganizationId()).isEqualTo(orgId);
        assertThat(response.getOrganizationName()).isEqualTo("Acme Corp");
        assertThat(response.getAverageRating()).isEqualTo(4.5);
        assertThat(response.getTrustLevel()).isEqualTo(3);
        assertThat(response.getDaysActive()).isGreaterThanOrEqualTo(29);
        assertThat(response.getMembers()).isNotNull();
        assertThat(response.getApplications()).isNotNull();
        assertThat(response.getReviews()).isNotNull();
        assertThat(response.getCollab()).isNotNull();
        assertThat(response.getRfq()).isNotNull();
        assertThat(response.getInvitations()).isNotNull();
    }

    @Test
    @DisplayName("getAnalytics_orgNotFound_throwsResourceNotFoundException")
    void getAnalytics_orgNotFound_throwsResourceNotFoundException() {
        when(orgRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analyticsService.getAnalytics("missing", "owner-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Organisation introuvable");
    }

    @Test
    @DisplayName("getAnalytics_nonMemberNonOwner_throwsBusinessRuleException")
    void getAnalytics_nonMemberNonOwner_throwsBusinessRuleException() {
        String orgId = "org-1";
        Organization org = buildOrg(orgId, "owner-1");
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(memberRepo.findByOrganizationIdAndUserId(orgId, "stranger")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analyticsService.getAnalytics(orgId, "stranger"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Accès refusé");
    }

    @Test
    @DisplayName("getAnalytics_managerRequests_returnsFullDashboard")
    void getAnalytics_managerRequests_returnsFullDashboard() {
        String orgId = "org-1";
        String managerId = "manager-1";

        Organization org = buildOrg(orgId, "owner-1");
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));

        OrganizationMember member = OrganizationMember.builder()
                .organizationId(orgId).userId(managerId).role(MemberRole.MANAGER).build();
        when(memberRepo.findByOrganizationIdAndUserId(orgId, managerId))
                .thenReturn(Optional.of(member));
        stubAllCountsToZero(orgId);

        OrgAnalyticsResponse response = analyticsService.getAnalytics(orgId, managerId);

        assertThat(response).isNotNull();
        assertThat(response.getOrganizationId()).isEqualTo(orgId);
    }

    @Test
    @DisplayName("getAnalytics_withApplicationStats_computesAcceptanceRate")
    void getAnalytics_withApplicationStats_computesAcceptanceRate() {
        String orgId = "org-1";
        String ownerId = "owner-1";

        Organization org = buildOrg(orgId, ownerId);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(memberRepo.findByOrganizationIdAndUserId(orgId, ownerId)).thenReturn(Optional.empty());

        stubAllCountsToZero(orgId);

        // Override application stats
        when(appRepo.countByOrganizationId(orgId)).thenReturn(10L);
        when(appRepo.countByOrganizationIdAndStatus(orgId, ApplicationStatus.ACCEPTED)).thenReturn(7L);
        when(appRepo.countByOrganizationIdAndStatus(orgId, ApplicationStatus.REJECTED)).thenReturn(3L);

        OrgAnalyticsResponse response = analyticsService.getAnalytics(orgId, ownerId);

        assertThat(response.getApplications().getTotal()).isEqualTo(10L);
        assertThat(response.getApplications().getAccepted()).isEqualTo(7L);
        assertThat(response.getApplications().getAcceptanceRate()).isNotNull();
        // 7 accepted out of 10 decided = 70%
        assertThat(response.getApplications().getAcceptanceRate()).isEqualTo(70.0);
    }

    @Test
    @DisplayName("getAnalytics_withRfqStats_computesResponseRate")
    void getAnalytics_withRfqStats_computesResponseRate() {
        String orgId = "org-1";
        String ownerId = "owner-1";

        Organization org = buildOrg(orgId, ownerId);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(memberRepo.findByOrganizationIdAndUserId(orgId, ownerId)).thenReturn(Optional.empty());

        stubAllCountsToZero(orgId);

        // Override RFQ stats: 4 total, 2 responded, 2 closed
        when(rfqRepo.countByOrganizationId(orgId)).thenReturn(4L);
        when(rfqRepo.countByOrganizationIdAndStatus(orgId, RfqStatus.RESPONDED)).thenReturn(2L);
        when(rfqRepo.countByOrganizationIdAndStatus(orgId, RfqStatus.CLOSED)).thenReturn(2L);

        OrgAnalyticsResponse response = analyticsService.getAnalytics(orgId, ownerId);

        assertThat(response.getRfq().getTotal()).isEqualTo(4L);
        assertThat(response.getRfq().getResponseRate()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("getAnalytics_noCreatedAt_returnsDaysActiveZero")
    void getAnalytics_noCreatedAt_returnsDaysActiveZero() {
        String orgId = "org-1";
        String ownerId = "owner-1";

        Organization org = Organization.builder()
                .id(orgId).name("Acme Corp").ownerId(ownerId)
                .status(OrganizationStatus.ACTIVE)
                .averageRating(0.0).trustLevel(1)
                .createdAt(null)  // no createdAt
                .build();
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(memberRepo.findByOrganizationIdAndUserId(orgId, ownerId)).thenReturn(Optional.empty());

        stubAllCountsToZero(orgId);

        OrgAnalyticsResponse response = analyticsService.getAnalytics(orgId, ownerId);

        assertThat(response.getDaysActive()).isEqualTo(0L);
    }
}
