package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.AdminSuspendRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.AdminVerifyRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.OrganizationDashboardStats;
import com.smartfreelance.microservice.organizationservice.dto.response.OrganizationResponse;
import com.smartfreelance.microservice.organizationservice.entity.AuditLog;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationStatus;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.AuditLogRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminOrganizationServiceImpl Unit Tests")
class AdminOrganizationServiceImplTest {

    @Mock private OrganizationRepository orgRepo;
    @Mock private AuditLogRepository auditRepo;

    @InjectMocks private AdminOrganizationServiceImpl adminService;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Organization buildOrg(String orgId, OrganizationStatus status) {
        return Organization.builder()
                .id(orgId).name("Acme Corp").ownerId("owner-1")
                .status(status).build();
    }

    private AdminVerifyRequest buildVerifyRequest(String decision, String note) {
        AdminVerifyRequest req = new AdminVerifyRequest();
        req.setDecision(decision);
        req.setAdminNote(note);
        return req;
    }

    // ── listAll() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("listAll_withNoFilter_returnsAllOrgs")
    void listAll_withNoFilter_returnsAllOrgs() {
        Pageable pageable = PageRequest.of(0, 20);
        Organization o1 = buildOrg("org-1", OrganizationStatus.ACTIVE);
        Organization o2 = buildOrg("org-2", OrganizationStatus.SUSPENDED);
        when(orgRepo.findAll(pageable)).thenReturn(new PageImpl<>(List.of(o1, o2)));

        Page<OrganizationResponse> result = adminService.listAll(null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("listAll_withStatusFilter_returnsFilteredOrgs")
    void listAll_withStatusFilter_returnsFilteredOrgs() {
        Pageable pageable = PageRequest.of(0, 20);
        Organization o1 = buildOrg("org-1", OrganizationStatus.SUSPENDED);
        when(orgRepo.findByStatus(OrganizationStatus.SUSPENDED, pageable))
                .thenReturn(new PageImpl<>(List.of(o1)));

        Page<OrganizationResponse> result = adminService.listAll(OrganizationStatus.SUSPENDED, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(OrganizationStatus.SUSPENDED);
    }

    @Test
    @DisplayName("listAll_emptyRepository_returnsEmptyPage")
    void listAll_emptyRepository_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(orgRepo.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

        Page<OrganizationResponse> result = adminService.listAll(null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    // ── verify() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("verify_approveDecision_setsActiveStatusAndAudits")
    void verify_approveDecision_setsActiveStatusAndAudits() {
        String orgId = "org-1";
        Organization org = buildOrg(orgId, OrganizationStatus.PENDING_VERIFICATION);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(orgRepo.save(any())).thenReturn(org);
        when(auditRepo.save(any())).thenReturn(null);

        AdminVerifyRequest req = buildVerifyRequest("APPROVE", "Looks good");
        OrganizationResponse response = adminService.verify(orgId, req, "admin-1");

        ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
        verify(orgRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
        verify(auditRepo).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("verify_rejectDecision_setsRejectedStatus")
    void verify_rejectDecision_setsRejectedStatus() {
        String orgId = "org-1";
        Organization org = buildOrg(orgId, OrganizationStatus.PENDING_VERIFICATION);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(orgRepo.save(any())).thenReturn(org);
        when(auditRepo.save(any())).thenReturn(null);

        AdminVerifyRequest req = buildVerifyRequest("REJECT", "Missing documents");
        adminService.verify(orgId, req, "admin-1");

        ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
        verify(orgRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrganizationStatus.REJECTED);
    }

    @Test
    @DisplayName("verify_awaitingInfoDecision_setsAwaitingInfoStatus")
    void verify_awaitingInfoDecision_setsAwaitingInfoStatus() {
        String orgId = "org-1";
        Organization org = buildOrg(orgId, OrganizationStatus.PENDING_VERIFICATION);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(orgRepo.save(any())).thenReturn(org);
        when(auditRepo.save(any())).thenReturn(null);

        AdminVerifyRequest req = buildVerifyRequest("AWAITING_INFO", "Need more info");
        adminService.verify(orgId, req, "admin-1");

        ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
        verify(orgRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrganizationStatus.AWAITING_INFO);
    }

    @Test
    @DisplayName("verify_orgNotFound_throwsResourceNotFoundException")
    void verify_orgNotFound_throwsResourceNotFoundException() {
        when(orgRepo.findById("missing")).thenReturn(Optional.empty());

        AdminVerifyRequest req = buildVerifyRequest("APPROVE", "ok");
        assertThatThrownBy(() -> adminService.verify("missing", req, "admin-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Organization not found");
    }

    // ── suspend() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("suspend_activeOrg_setsSuspendedStatusAndAudits")
    void suspend_activeOrg_setsSuspendedStatusAndAudits() {
        String orgId = "org-1";
        Organization org = buildOrg(orgId, OrganizationStatus.ACTIVE);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(orgRepo.save(any())).thenReturn(org);
        when(auditRepo.save(any())).thenReturn(null);

        AdminSuspendRequest req = new AdminSuspendRequest();
        req.setReason("Policy violation");

        OrganizationResponse response = adminService.suspend(orgId, req, "admin-1");

        ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
        verify(orgRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrganizationStatus.SUSPENDED);
        assertThat(captor.getValue().getAdminNote()).isEqualTo("Policy violation");
        verify(auditRepo).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("suspend_orgNotFound_throwsResourceNotFoundException")
    void suspend_orgNotFound_throwsResourceNotFoundException() {
        when(orgRepo.findById("missing")).thenReturn(Optional.empty());

        AdminSuspendRequest req = new AdminSuspendRequest();
        req.setReason("Reason");

        assertThatThrownBy(() -> adminService.suspend("missing", req, "admin-1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("suspend_setsAdminNoteFromReason")
    void suspend_setsAdminNoteFromReason() {
        String orgId = "org-1";
        Organization org = buildOrg(orgId, OrganizationStatus.ACTIVE);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(orgRepo.save(any())).thenReturn(org);
        when(auditRepo.save(any())).thenReturn(null);

        AdminSuspendRequest req = new AdminSuspendRequest();
        req.setReason("Fraudulent activity");

        adminService.suspend(orgId, req, "admin-1");

        ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
        verify(orgRepo).save(captor.capture());
        assertThat(captor.getValue().getAdminNote()).isEqualTo("Fraudulent activity");
    }

    // ── reactivate() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("reactivate_suspendedOrg_setsActiveStatusAndAudits")
    void reactivate_suspendedOrg_setsActiveStatusAndAudits() {
        String orgId = "org-1";
        Organization org = buildOrg(orgId, OrganizationStatus.SUSPENDED);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(orgRepo.save(any())).thenReturn(org);
        when(auditRepo.save(any())).thenReturn(null);

        OrganizationResponse response = adminService.reactivate(orgId, "admin-1");

        ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
        verify(orgRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
        verify(auditRepo).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("reactivate_orgNotFound_throwsResourceNotFoundException")
    void reactivate_orgNotFound_throwsResourceNotFoundException() {
        when(orgRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.reactivate("missing", "admin-1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("reactivate_alreadyActiveOrg_stillSavesAsActive")
    void reactivate_alreadyActiveOrg_stillSavesAsActive() {
        String orgId = "org-1";
        Organization org = buildOrg(orgId, OrganizationStatus.ACTIVE);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(orgRepo.save(any())).thenReturn(org);
        when(auditRepo.save(any())).thenReturn(null);

        OrganizationResponse response = adminService.reactivate(orgId, "admin-1");

        ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
        verify(orgRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
    }

    // ── getDashboardStats() ───────────────────────────────────────────────────

    @Test
    @DisplayName("getDashboardStats_returnsAllCounts")
    void getDashboardStats_returnsAllCounts() {
        when(orgRepo.count()).thenReturn(100L);
        when(orgRepo.countByStatus(OrganizationStatus.ACTIVE)).thenReturn(70L);
        when(orgRepo.countByStatus(OrganizationStatus.PENDING_VERIFICATION)).thenReturn(15L);
        when(orgRepo.countByStatus(OrganizationStatus.SUSPENDED)).thenReturn(10L);
        when(orgRepo.countByStatus(OrganizationStatus.DISSOLVED)).thenReturn(5L);

        OrganizationDashboardStats stats = adminService.getDashboardStats();

        assertThat(stats.getTotalOrganizations()).isEqualTo(100L);
        assertThat(stats.getActiveOrganizations()).isEqualTo(70L);
        assertThat(stats.getPendingVerification()).isEqualTo(15L);
        assertThat(stats.getSuspended()).isEqualTo(10L);
        assertThat(stats.getDissolved()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getDashboardStats_emptyRepo_returnsAllZeros")
    void getDashboardStats_emptyRepo_returnsAllZeros() {
        when(orgRepo.count()).thenReturn(0L);
        when(orgRepo.countByStatus(any(OrganizationStatus.class))).thenReturn(0L);

        OrganizationDashboardStats stats = adminService.getDashboardStats();

        assertThat(stats.getTotalOrganizations()).isEqualTo(0L);
        assertThat(stats.getActiveOrganizations()).isEqualTo(0L);
    }

    @Test
    @DisplayName("getDashboardStats_doesNotRequireAuthentication")
    void getDashboardStats_doesNotRequireAuthentication() {
        when(orgRepo.count()).thenReturn(5L);
        when(orgRepo.countByStatus(any())).thenReturn(1L);

        OrganizationDashboardStats stats = adminService.getDashboardStats();

        assertThat(stats).isNotNull();
        verify(orgRepo).count();
    }
}
