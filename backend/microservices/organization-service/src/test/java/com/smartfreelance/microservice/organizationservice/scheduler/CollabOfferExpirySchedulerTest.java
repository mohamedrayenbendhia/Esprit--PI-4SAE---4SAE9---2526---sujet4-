package com.smartfreelance.microservice.organizationservice.scheduler;

import com.smartfreelance.microservice.organizationservice.entity.AuditLog;
import com.smartfreelance.microservice.organizationservice.entity.CollabOffer;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.enums.CollabOfferStatus;
import com.smartfreelance.microservice.organizationservice.notification.OrgNotificationService;
import com.smartfreelance.microservice.organizationservice.repository.AuditLogRepository;
import com.smartfreelance.microservice.organizationservice.repository.CollabOfferRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CollabOfferExpiryScheduler Unit Tests")
class CollabOfferExpirySchedulerTest {

    /* ── Mocks ───────────────────────────────────────────────────────────── */

    @Mock private CollabOfferRepository  offerRepo;
    @Mock private OrganizationRepository orgRepo;
    @Mock private AuditLogRepository     auditRepo;
    @Mock private OrgNotificationService notif;

    @InjectMocks
    private CollabOfferExpiryScheduler scheduler;

    /* ── Helpers ─────────────────────────────────────────────────────────── */

    private CollabOffer buildOffer(String id, String orgId, CollabOfferStatus status) {
        return CollabOffer.builder()
                .id(id)
                .organizationId(orgId)
                .createdBy("owner-" + orgId)
                .title("Offer " + id)
                .description("Description")
                .status(status)
                .deadlineDate(LocalDate.now().minusDays(1))
                .build();
    }

    private Organization buildOrg(String orgId) {
        return Organization.builder()
                .id(orgId)
                .name("Org " + orgId)
                .ownerId("owner-" + orgId)
                .build();
    }

    /* ── processExpiredOffers() ──────────────────────────────────────────── */

    @Test
    @DisplayName("processExpiredOffers_noExpiredOffers_doesNothing")
    void processExpiredOffers_noExpiredOffers_doesNothing() {
        when(offerRepo.findExpiredOpenOffers(any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(offerRepo.findStaleOffersWithNoApplications(any(LocalDateTime.class))).thenReturn(Collections.emptyList());
        when(offerRepo.findOpenOffersOfSuspendedOrgs()).thenReturn(Collections.emptyList());

        scheduler.processExpiredOffers();

        verify(offerRepo, never()).save(any());
        verify(auditRepo, never()).save(any());
        verify(notif, never()).notifyCollabOfferAutoExpired(any(), any(), any());
    }

    @Test
    @DisplayName("processExpiredOffers_deadlinePassed_closesOffer")
    void processExpiredOffers_deadlinePassed_closesOffer() {
        CollabOffer expiredOffer = buildOffer("offer-1", "org-1", CollabOfferStatus.OPEN);
        when(offerRepo.findExpiredOpenOffers(any(LocalDate.class))).thenReturn(List.of(expiredOffer));
        when(offerRepo.findStaleOffersWithNoApplications(any(LocalDateTime.class))).thenReturn(Collections.emptyList());
        when(offerRepo.findOpenOffersOfSuspendedOrgs()).thenReturn(Collections.emptyList());
        when(offerRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.processExpiredOffers();

        // Offer status changed to CLOSED
        assertThat(expiredOffer.getStatus()).isEqualTo(CollabOfferStatus.CLOSED);
        verify(offerRepo).save(expiredOffer);

        // Audit log created
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditRepo).save(auditCaptor.capture());
        AuditLog log = auditCaptor.getValue();
        assertThat(log.getAction()).isEqualTo("OFFER_CLOSED_DEADLINE");
        assertThat(log.getOrganizationId()).isEqualTo("org-1");
        assertThat(log.getPerformedByUserId()).isEqualTo("SYSTEM");
        assertThat(log.getDetails()).contains("Offer offer-1");

        // No notification sent for normal deadline closure
        verify(notif, never()).notifyCollabOfferAutoExpired(any(), any(), any());
    }

    @Test
    @DisplayName("processExpiredOffers_staleOffer_closesAndNotifies")
    void processExpiredOffers_staleOffer_closesAndNotifies() {
        CollabOffer staleOffer = buildOffer("offer-stale", "org-2", CollabOfferStatus.OPEN);
        when(offerRepo.findExpiredOpenOffers(any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(offerRepo.findStaleOffersWithNoApplications(any(LocalDateTime.class))).thenReturn(List.of(staleOffer));
        when(offerRepo.findOpenOffersOfSuspendedOrgs()).thenReturn(Collections.emptyList());
        when(offerRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Organization org = buildOrg("org-2");
        when(orgRepo.findById("org-2")).thenReturn(Optional.of(org));

        scheduler.processExpiredOffers();

        // Offer status changed to CLOSED
        assertThat(staleOffer.getStatus()).isEqualTo(CollabOfferStatus.CLOSED);
        verify(offerRepo).save(staleOffer);

        // Audit log created for stale closure
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditRepo).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("OFFER_CLOSED_NO_APPLICATIONS");

        // Owner is notified for stale closure
        verify(notif).notifyCollabOfferAutoExpired("owner-org-2", "Org org-2", "Offer offer-stale");
    }

    @Test
    @DisplayName("processExpiredOffers_staleOffer_orgNotFound_noNotification")
    void processExpiredOffers_staleOffer_orgNotFound_noNotification() {
        CollabOffer staleOffer = buildOffer("offer-stale-2", "org-ghost", CollabOfferStatus.OPEN);
        when(offerRepo.findExpiredOpenOffers(any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(offerRepo.findStaleOffersWithNoApplications(any(LocalDateTime.class))).thenReturn(List.of(staleOffer));
        when(offerRepo.findOpenOffersOfSuspendedOrgs()).thenReturn(Collections.emptyList());
        when(offerRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Org not found → notification is skipped gracefully
        when(orgRepo.findById("org-ghost")).thenReturn(Optional.empty());

        scheduler.processExpiredOffers();

        assertThat(staleOffer.getStatus()).isEqualTo(CollabOfferStatus.CLOSED);
        verify(notif, never()).notifyCollabOfferAutoExpired(any(), any(), any());
    }

    @Test
    @DisplayName("processExpiredOffers_suspendedOrg_cancelsOffers")
    void processExpiredOffers_suspendedOrg_cancelsOffers() {
        CollabOffer offer1 = buildOffer("offer-susp-1", "org-suspended", CollabOfferStatus.OPEN);
        CollabOffer offer2 = buildOffer("offer-susp-2", "org-suspended", CollabOfferStatus.OPEN);

        when(offerRepo.findExpiredOpenOffers(any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(offerRepo.findStaleOffersWithNoApplications(any(LocalDateTime.class))).thenReturn(Collections.emptyList());
        when(offerRepo.findOpenOffersOfSuspendedOrgs()).thenReturn(List.of(offer1, offer2));
        when(offerRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.processExpiredOffers();

        // Both offers cancelled
        assertThat(offer1.getStatus()).isEqualTo(CollabOfferStatus.CANCELLED);
        assertThat(offer2.getStatus()).isEqualTo(CollabOfferStatus.CANCELLED);
        verify(offerRepo, times(2)).save(any(CollabOffer.class));

        // Two audit logs saved for cancellation
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditRepo, times(2)).save(auditCaptor.capture());
        auditCaptor.getAllValues().forEach(log -> {
            assertThat(log.getAction()).isEqualTo("OFFER_CANCELLED_ORG_SUSPENDED");
            assertThat(log.getOrganizationId()).isEqualTo("org-suspended");
            assertThat(log.getPerformedByUserId()).isEqualTo("SYSTEM");
        });

        // No notification for suspended org cancellation
        verify(notif, never()).notifyCollabOfferAutoExpired(any(), any(), any());
    }

    @Test
    @DisplayName("processExpiredOffers_mixedRules_eachRuleTriggersIndependently")
    void processExpiredOffers_mixedRules_eachRuleTriggersIndependently() {
        CollabOffer deadlineOffer = buildOffer("offer-dl",    "org-dl",   CollabOfferStatus.OPEN);
        CollabOffer staleOffer    = buildOffer("offer-stale", "org-stale", CollabOfferStatus.OPEN);
        CollabOffer suspOffer     = buildOffer("offer-susp",  "org-susp",  CollabOfferStatus.OPEN);

        when(offerRepo.findExpiredOpenOffers(any(LocalDate.class))).thenReturn(List.of(deadlineOffer));
        when(offerRepo.findStaleOffersWithNoApplications(any(LocalDateTime.class))).thenReturn(List.of(staleOffer));
        when(offerRepo.findOpenOffersOfSuspendedOrgs()).thenReturn(List.of(suspOffer));
        when(offerRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Organization orgStale = buildOrg("org-stale");
        when(orgRepo.findById("org-stale")).thenReturn(Optional.of(orgStale));

        scheduler.processExpiredOffers();

        assertThat(deadlineOffer.getStatus()).isEqualTo(CollabOfferStatus.CLOSED);
        assertThat(staleOffer.getStatus()).isEqualTo(CollabOfferStatus.CLOSED);
        assertThat(suspOffer.getStatus()).isEqualTo(CollabOfferStatus.CANCELLED);

        // 3 saves total (one per offer)
        verify(offerRepo, times(3)).save(any(CollabOffer.class));

        // 3 audit logs
        verify(auditRepo, times(3)).save(any(AuditLog.class));

        // Notification only for stale
        verify(notif, times(1)).notifyCollabOfferAutoExpired("owner-org-stale", "Org org-stale", "Offer offer-stale");
    }

    @Test
    @DisplayName("processExpiredOffers_multipleDeadlineExpired_allClosed")
    void processExpiredOffers_multipleDeadlineExpired_allClosed() {
        CollabOffer o1 = buildOffer("o1", "org-x", CollabOfferStatus.OPEN);
        CollabOffer o2 = buildOffer("o2", "org-x", CollabOfferStatus.OPEN);
        CollabOffer o3 = buildOffer("o3", "org-y", CollabOfferStatus.OPEN);

        when(offerRepo.findExpiredOpenOffers(any(LocalDate.class))).thenReturn(List.of(o1, o2, o3));
        when(offerRepo.findStaleOffersWithNoApplications(any(LocalDateTime.class))).thenReturn(Collections.emptyList());
        when(offerRepo.findOpenOffersOfSuspendedOrgs()).thenReturn(Collections.emptyList());
        when(offerRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.processExpiredOffers();

        assertThat(o1.getStatus()).isEqualTo(CollabOfferStatus.CLOSED);
        assertThat(o2.getStatus()).isEqualTo(CollabOfferStatus.CLOSED);
        assertThat(o3.getStatus()).isEqualTo(CollabOfferStatus.CLOSED);
        verify(offerRepo, times(3)).save(any(CollabOffer.class));
        verify(auditRepo, times(3)).save(any(AuditLog.class));
    }
}
