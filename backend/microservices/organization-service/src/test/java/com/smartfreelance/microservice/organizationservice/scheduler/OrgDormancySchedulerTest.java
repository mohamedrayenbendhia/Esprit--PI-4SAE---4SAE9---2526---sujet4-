package com.smartfreelance.microservice.organizationservice.scheduler;

import com.smartfreelance.microservice.organizationservice.entity.AuditLog;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationStatus;
import com.smartfreelance.microservice.organizationservice.notification.OrgNotificationService;
import com.smartfreelance.microservice.organizationservice.repository.AuditLogRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrgDormancyScheduler Unit Tests")
class OrgDormancySchedulerTest {

    /* ── Mocks ───────────────────────────────────────────────────────────── */

    @Mock private OrganizationRepository orgRepo;
    @Mock private AuditLogRepository     auditRepo;
    @Mock private OrgNotificationService notif;

    @InjectMocks
    private OrgDormancyScheduler scheduler;

    /* ── Helpers ─────────────────────────────────────────────────────────── */

    private Organization buildOrg(String id, OrganizationStatus status) {
        return Organization.builder()
                .id(id)
                .name("Org " + id)
                .ownerId("owner-" + id)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /* ── detectDormantOrgs() ─────────────────────────────────────────────── */

    @Test
    @DisplayName("detectDormantOrgs_inactive60days_sendsWarning")
    void detectDormantOrgs_inactive60days_sendsWarning() {
        Organization org = buildOrg("org-warn", OrganizationStatus.ACTIVE);
        when(orgRepo.findAllNonDissolved()).thenReturn(List.of(org));

        // Last activity exactly 65 days ago — falls in the warn range [60, 90)
        LocalDateTime lastActivity = LocalDateTime.now().minusDays(65);
        when(auditRepo.findLastActivityAt("org-warn")).thenReturn(Optional.of(lastActivity));

        scheduler.detectDormantOrgs();

        // Warning: org saved with admin note, notification sent, but status NOT changed
        verify(orgRepo).save(org);
        assertThat(org.getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
        assertThat(org.getAdminNote()).contains("avertissement automatique");
        verify(notif).notifyDormancyWarning(eq("owner-org-warn"), eq("Org org-warn"), anyLong());

        // No audit log created for simple warning
        verify(auditRepo, never()).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("detectDormantOrgs_inactive90days_setsAwaitingInfo")
    void detectDormantOrgs_inactive90days_setsAwaitingInfo() {
        Organization org = buildOrg("org-await", OrganizationStatus.ACTIVE);
        when(orgRepo.findAllNonDissolved()).thenReturn(List.of(org));

        // Last activity 95 days ago — falls in [90, 180)
        LocalDateTime lastActivity = LocalDateTime.now().minusDays(95);
        when(auditRepo.findLastActivityAt("org-await")).thenReturn(Optional.of(lastActivity));
        when(auditRepo.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        scheduler.detectDormantOrgs();

        assertThat(org.getStatus()).isEqualTo(OrganizationStatus.AWAITING_INFO);
        verify(orgRepo).save(org);
        verify(notif).notifyDormancyWarning(eq("owner-org-await"), eq("Org org-await"), anyLong());

        // Audit log created for AWAITING_INFO
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditRepo).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("DORMANCY_AWAITING_INFO");
        assertThat(auditCaptor.getValue().getOrganizationId()).isEqualTo("org-await");
        assertThat(auditCaptor.getValue().getPerformedByUserId()).isEqualTo("SYSTEM");
    }

    @Test
    @DisplayName("detectDormantOrgs_inactive180days_suspends")
    void detectDormantOrgs_inactive180days_suspends() {
        Organization org = buildOrg("org-suspend", OrganizationStatus.ACTIVE);
        when(orgRepo.findAllNonDissolved()).thenReturn(List.of(org));

        // Last activity 200 days ago — >= 180 days → auto-suspend
        LocalDateTime lastActivity = LocalDateTime.now().minusDays(200);
        when(auditRepo.findLastActivityAt("org-suspend")).thenReturn(Optional.of(lastActivity));
        when(auditRepo.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        scheduler.detectDormantOrgs();

        assertThat(org.getStatus()).isEqualTo(OrganizationStatus.SUSPENDED);
        verify(orgRepo).save(org);
        verify(notif).notifyOrganizationSuspended(
                eq("owner-org-suspend"), eq("Org org-suspend"), contains("Inactivité"));

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditRepo).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("DORMANCY_AUTO_SUSPEND");
        assertThat(auditCaptor.getValue().getOrganizationId()).isEqualTo("org-suspend");
    }

    @Test
    @DisplayName("detectDormantOrgs_alreadySuspended_skips")
    void detectDormantOrgs_alreadySuspended_skips() {
        // Organization already suspended — should NOT be processed again
        Organization org = buildOrg("org-already-suspended", OrganizationStatus.SUSPENDED);
        when(orgRepo.findAllNonDissolved()).thenReturn(List.of(org));

        // Last activity 250 days ago (would normally trigger suspension)
        LocalDateTime lastActivity = LocalDateTime.now().minusDays(250);
        when(auditRepo.findLastActivityAt("org-already-suspended")).thenReturn(Optional.of(lastActivity));

        scheduler.detectDormantOrgs();

        // Already suspended — no save, no notification, no audit
        verify(orgRepo, never()).save(any());
        verify(notif, never()).notifyOrganizationSuspended(any(), any(), any());
        verify(auditRepo, never()).save(any());
    }

    @Test
    @DisplayName("detectDormantOrgs_noActivity_usesCreatedAt")
    void detectDormantOrgs_noActivity_usesCreatedAt() {
        // Org created 100 days ago, no audit log found → uses createdAt as fallback
        Organization org = Organization.builder()
                .id("org-no-audit")
                .name("Org no-audit")
                .ownerId("owner-no-audit")
                .status(OrganizationStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusDays(100))
                .build();

        when(orgRepo.findAllNonDissolved()).thenReturn(List.of(org));
        // No audit log
        when(auditRepo.findLastActivityAt("org-no-audit")).thenReturn(Optional.empty());
        when(auditRepo.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        scheduler.detectDormantOrgs();

        // 100 days → AWAITING_INFO range [90, 180)
        assertThat(org.getStatus()).isEqualTo(OrganizationStatus.AWAITING_INFO);
        verify(orgRepo).save(org);
        verify(notif).notifyDormancyWarning(eq("owner-no-audit"), eq("Org no-audit"), anyLong());
    }

    @Test
    @DisplayName("detectDormantOrgs_noOrgs_doesNothing")
    void detectDormantOrgs_noOrgs_doesNothing() {
        when(orgRepo.findAllNonDissolved()).thenReturn(Collections.emptyList());

        scheduler.detectDormantOrgs();

        verify(orgRepo, never()).save(any());
        verify(notif, never()).notifyDormancyWarning(any(), any(), anyLong());
        verify(notif, never()).notifyOrganizationSuspended(any(), any(), any());
        verify(auditRepo, never()).save(any());
    }

    @Test
    @DisplayName("detectDormantOrgs_recentlyActive_doesNothing")
    void detectDormantOrgs_recentlyActive_doesNothing() {
        Organization org = buildOrg("org-active", OrganizationStatus.ACTIVE);
        when(orgRepo.findAllNonDissolved()).thenReturn(List.of(org));

        // Last activity only 5 days ago — no dormancy thresholds exceeded
        LocalDateTime recentActivity = LocalDateTime.now().minusDays(5);
        when(auditRepo.findLastActivityAt("org-active")).thenReturn(Optional.of(recentActivity));

        scheduler.detectDormantOrgs();

        verify(orgRepo, never()).save(any());
        verify(notif, never()).notifyDormancyWarning(any(), any(), anyLong());
        verify(auditRepo, never()).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("detectDormantOrgs_awaitingInfoOrg_at90days_skipsAlreadyAwaitingInfo")
    void detectDormantOrgs_awaitingInfoOrg_at90days_skipsAlreadyAwaitingInfo() {
        // An org with AWAITING_INFO status in the 90–180 day range is skipped (only ACTIVE triggers it)
        Organization org = buildOrg("org-awaiting", OrganizationStatus.AWAITING_INFO);
        when(orgRepo.findAllNonDissolved()).thenReturn(List.of(org));

        LocalDateTime lastActivity = LocalDateTime.now().minusDays(100);
        when(auditRepo.findLastActivityAt("org-awaiting")).thenReturn(Optional.of(lastActivity));

        scheduler.detectDormantOrgs();

        // Status stays AWAITING_INFO, no extra notifications, no audit
        assertThat(org.getStatus()).isEqualTo(OrganizationStatus.AWAITING_INFO);
        verify(orgRepo, never()).save(any());
        verify(notif, never()).notifyDormancyWarning(any(), any(), anyLong());
    }

    @Test
    @DisplayName("detectDormantOrgs_multipleOrgs_processesEachIndependently")
    void detectDormantOrgs_multipleOrgs_processesEachIndependently() {
        Organization activeOrg    = buildOrg("org-a", OrganizationStatus.ACTIVE);
        Organization warningOrg   = buildOrg("org-b", OrganizationStatus.ACTIVE);
        Organization suspendOrg   = buildOrg("org-c", OrganizationStatus.ACTIVE);

        when(orgRepo.findAllNonDissolved()).thenReturn(List.of(activeOrg, warningOrg, suspendOrg));

        when(auditRepo.findLastActivityAt("org-a")).thenReturn(Optional.of(LocalDateTime.now().minusDays(5)));
        when(auditRepo.findLastActivityAt("org-b")).thenReturn(Optional.of(LocalDateTime.now().minusDays(70)));
        when(auditRepo.findLastActivityAt("org-c")).thenReturn(Optional.of(LocalDateTime.now().minusDays(200)));
        when(auditRepo.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        scheduler.detectDormantOrgs();

        // org-a: no change
        assertThat(activeOrg.getStatus()).isEqualTo(OrganizationStatus.ACTIVE);

        // org-b: warning only
        assertThat(warningOrg.getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
        verify(notif).notifyDormancyWarning(eq("owner-org-b"), eq("Org org-b"), anyLong());

        // org-c: suspended
        assertThat(suspendOrg.getStatus()).isEqualTo(OrganizationStatus.SUSPENDED);
        verify(notif).notifyOrganizationSuspended(eq("owner-org-c"), eq("Org org-c"), anyString());
    }
}
