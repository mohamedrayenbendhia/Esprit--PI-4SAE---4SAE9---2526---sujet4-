package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.CreateApplicationRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.RespondApplicationRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.ApplicationResponse;
import com.smartfreelance.microservice.organizationservice.entity.AuditLog;
import com.smartfreelance.microservice.organizationservice.entity.OrgApplication;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.entity.OrganizationMember;
import com.smartfreelance.microservice.organizationservice.enums.ApplicationStatus;
import com.smartfreelance.microservice.organizationservice.enums.MemberRole;
import com.smartfreelance.microservice.organizationservice.enums.MemberStatus;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationStatus;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.AuditLogRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrgApplicationRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationMemberRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationServiceImpl Unit Tests")
class ApplicationServiceImplTest {

    /* ── Mocks ───────────────────────────────────────────────────────────── */

    @Mock private OrgApplicationRepository      appRepo;
    @Mock private OrganizationMemberRepository  memberRepo;
    @Mock private OrganizationRepository        orgRepo;
    @Mock private AuditLogRepository            auditRepo;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    /* ── Helpers ─────────────────────────────────────────────────────────── */

    private Organization buildActiveOrg(String orgId, String ownerId) {
        return Organization.builder()
                .id(orgId)
                .name("Acme")
                .ownerId(ownerId)
                .status(OrganizationStatus.ACTIVE)
                .build();
    }

    private OrgApplication buildPendingApp(String appId, String orgId, String applicantId) {
        return OrgApplication.builder()
                .id(appId)
                .organizationId(orgId)
                .applicantId(applicantId)
                .message("I would like to join.")
                .cvUrl("https://cv.example.com")
                .status(ApplicationStatus.PENDING)
                .build();
    }

    private CreateApplicationRequest buildCreateRequest() {
        CreateApplicationRequest req = new CreateApplicationRequest();
        req.setMessage("I would like to join.");
        req.setCvUrl("https://cv.example.com");
        return req;
    }

    /* ── apply() ─────────────────────────────────────────────────────────── */

    @Test
    @DisplayName("apply_newApplication_savesAndAudits")
    void apply_newApplication_savesAndAudits() {
        String orgId      = "org-1";
        String applicantId = "user-1";
        String ownerId    = "owner-1";

        Organization org = buildActiveOrg(orgId, ownerId);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(memberRepo.findByOrganizationIdAndUserId(orgId, applicantId)).thenReturn(Optional.empty());
        when(appRepo.existsByOrganizationIdAndApplicantIdAndStatus(orgId, applicantId, ApplicationStatus.PENDING))
                .thenReturn(false);

        OrgApplication saved = buildPendingApp("app-1", orgId, applicantId);
        when(appRepo.save(any(OrgApplication.class))).thenReturn(saved);
        when(auditRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ApplicationResponse response = applicationService.apply(orgId, buildCreateRequest(), applicantId);

        assertThat(response).isNotNull();
        assertThat(response.getApplicantId()).isEqualTo(applicantId);
        assertThat(response.getOrganizationId()).isEqualTo(orgId);
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.PENDING);

        // Verify entity passed to save
        ArgumentCaptor<OrgApplication> captor = ArgumentCaptor.forClass(OrgApplication.class);
        verify(appRepo).save(captor.capture());
        OrgApplication captured = captor.getValue();
        assertThat(captured.getApplicantId()).isEqualTo(applicantId);
        assertThat(captured.getOrganizationId()).isEqualTo(orgId);
        assertThat(captured.getMessage()).isEqualTo("I would like to join.");

        // Audit log saved
        verify(auditRepo).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("apply_orgNotFound_throwsResourceNotFoundException")
    void apply_orgNotFound_throwsResourceNotFoundException() {
        when(orgRepo.findById("missing-org")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.apply("missing-org", buildCreateRequest(), "user-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Organization not found");

        verify(appRepo, never()).save(any());
    }

    @Test
    @DisplayName("apply_inactiveOrg_throwsBusinessRuleException")
    void apply_inactiveOrg_throwsBusinessRuleException() {
        Organization suspendedOrg = Organization.builder()
                .id("org-susp")
                .ownerId("owner-1")
                .status(OrganizationStatus.SUSPENDED)
                .build();
        when(orgRepo.findById("org-susp")).thenReturn(Optional.of(suspendedOrg));

        assertThatThrownBy(() -> applicationService.apply("org-susp", buildCreateRequest(), "user-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not active");

        verify(appRepo, never()).save(any());
    }

    @Test
    @DisplayName("apply_alreadyMember_throwsBusinessRuleException")
    void apply_alreadyMember_throwsBusinessRuleException() {
        String orgId = "org-1";
        String applicantId = "member-already";

        Organization org = buildActiveOrg(orgId, "owner-1");
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));

        OrganizationMember existingMember = OrganizationMember.builder()
                .id("mem-1").organizationId(orgId).userId(applicantId).build();
        when(memberRepo.findByOrganizationIdAndUserId(orgId, applicantId))
                .thenReturn(Optional.of(existingMember));

        assertThatThrownBy(() -> applicationService.apply(orgId, buildCreateRequest(), applicantId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already a member");

        verify(appRepo, never()).save(any());
    }

    @Test
    @DisplayName("apply_duplicatePending_throwsBusinessRuleException")
    void apply_duplicatePending_throwsBusinessRuleException() {
        String orgId = "org-1";
        String applicantId = "user-pending";

        Organization org = buildActiveOrg(orgId, "owner-1");
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(memberRepo.findByOrganizationIdAndUserId(orgId, applicantId)).thenReturn(Optional.empty());
        when(appRepo.existsByOrganizationIdAndApplicantIdAndStatus(orgId, applicantId, ApplicationStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> applicationService.apply(orgId, buildCreateRequest(), applicantId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("pending application");

        verify(appRepo, never()).save(any());
    }

    /* ── respond() ───────────────────────────────────────────────────────── */

    @Test
    @DisplayName("respond_acceptsApplication_updatesStatusAndCreatesMember")
    void respond_acceptsApplication_updatesStatusAndCreatesMember() {
        String appId      = "app-1";
        String orgId      = "org-1";
        String applicantId = "user-1";
        String ownerId    = "owner-1";

        OrgApplication app = buildPendingApp(appId, orgId, applicantId);
        when(appRepo.findById(appId)).thenReturn(Optional.of(app));

        Organization org = buildActiveOrg(orgId, ownerId);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(memberRepo.findByOrganizationIdAndUserId(orgId, ownerId)).thenReturn(Optional.empty());

        when(appRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RespondApplicationRequest request = new RespondApplicationRequest();
        request.setStatus(ApplicationStatus.ACCEPTED);

        ApplicationResponse response = applicationService.respond(appId, request, ownerId);

        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);

        // New member created
        ArgumentCaptor<OrganizationMember> memberCaptor = ArgumentCaptor.forClass(OrganizationMember.class);
        verify(memberRepo).save(memberCaptor.capture());
        OrganizationMember newMember = memberCaptor.getValue();
        assertThat(newMember.getUserId()).isEqualTo(applicantId);
        assertThat(newMember.getOrganizationId()).isEqualTo(orgId);
        assertThat(newMember.getRole()).isEqualTo(MemberRole.MEMBER);
        assertThat(newMember.getStatus()).isEqualTo(MemberStatus.ACTIVE);

        // App saved with ACCEPTED status and respondedAt set
        ArgumentCaptor<OrgApplication> appCaptor = ArgumentCaptor.forClass(OrgApplication.class);
        verify(appRepo).save(appCaptor.capture());
        assertThat(appCaptor.getValue().getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        assertThat(appCaptor.getValue().getRespondedAt()).isNotNull();

        // Audit log saved
        verify(auditRepo).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("respond_rejectsApplication_updatesStatus_noMemberCreated")
    void respond_rejectsApplication_updatesStatus_noMemberCreated() {
        String appId      = "app-2";
        String orgId      = "org-1";
        String applicantId = "user-2";
        String ownerId    = "owner-1";

        OrgApplication app = buildPendingApp(appId, orgId, applicantId);
        when(appRepo.findById(appId)).thenReturn(Optional.of(app));

        Organization org = buildActiveOrg(orgId, ownerId);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(memberRepo.findByOrganizationIdAndUserId(orgId, ownerId)).thenReturn(Optional.empty());

        when(appRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RespondApplicationRequest request = new RespondApplicationRequest();
        request.setStatus(ApplicationStatus.REJECTED);
        request.setRejectionReason("Not a good fit.");

        ApplicationResponse response = applicationService.respond(appId, request, ownerId);

        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.REJECTED);

        // No member created for rejected application
        verify(memberRepo, never()).save(any(OrganizationMember.class));

        // App saved with REJECTED
        ArgumentCaptor<OrgApplication> appCaptor = ArgumentCaptor.forClass(OrgApplication.class);
        verify(appRepo).save(appCaptor.capture());
        assertThat(appCaptor.getValue().getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(appCaptor.getValue().getRejectionReason()).isEqualTo("Not a good fit.");

        verify(auditRepo).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("respond_managerResponds_allowedToRespond")
    void respond_managerResponds_allowedToRespond() {
        String appId       = "app-3";
        String orgId       = "org-1";
        String applicantId = "user-3";
        String managerId   = "manager-1";
        String ownerId     = "owner-1";

        OrgApplication app = buildPendingApp(appId, orgId, applicantId);
        when(appRepo.findById(appId)).thenReturn(Optional.of(app));

        Organization org = buildActiveOrg(orgId, ownerId);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));

        // managerId is NOT the owner but has MANAGER role
        OrganizationMember manager = OrganizationMember.builder()
                .id("mem-manager").organizationId(orgId).userId(managerId)
                .role(MemberRole.MANAGER).status(MemberStatus.ACTIVE).build();
        when(memberRepo.findByOrganizationIdAndUserId(orgId, managerId)).thenReturn(Optional.of(manager));

        when(appRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RespondApplicationRequest request = new RespondApplicationRequest();
        request.setStatus(ApplicationStatus.ACCEPTED);

        ApplicationResponse response = applicationService.respond(appId, request, managerId);

        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        verify(memberRepo).save(any(OrganizationMember.class));
    }

    @Test
    @DisplayName("respond_unauthorizedUser_throwsBusinessRuleException")
    void respond_unauthorizedUser_throwsBusinessRuleException() {
        String appId       = "app-4";
        String orgId       = "org-1";
        String applicantId = "user-4";
        String ownerId     = "owner-1";
        String randomUser  = "random-user";

        OrgApplication app = buildPendingApp(appId, orgId, applicantId);
        when(appRepo.findById(appId)).thenReturn(Optional.of(app));

        Organization org = buildActiveOrg(orgId, ownerId);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));

        // randomUser is not the owner and has no MANAGER role
        when(memberRepo.findByOrganizationIdAndUserId(orgId, randomUser)).thenReturn(Optional.empty());

        RespondApplicationRequest request = new RespondApplicationRequest();
        request.setStatus(ApplicationStatus.ACCEPTED);

        assertThatThrownBy(() -> applicationService.respond(appId, request, randomUser))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("owner or a manager");

        verify(appRepo, never()).save(any());
    }

    @Test
    @DisplayName("respond_applicationNotPending_throwsBusinessRuleException")
    void respond_applicationNotPending_throwsBusinessRuleException() {
        String appId = "app-5";

        OrgApplication app = buildPendingApp(appId, "org-1", "user-5");
        app.setStatus(ApplicationStatus.ACCEPTED); // not pending anymore
        when(appRepo.findById(appId)).thenReturn(Optional.of(app));

        RespondApplicationRequest request = new RespondApplicationRequest();
        request.setStatus(ApplicationStatus.REJECTED);

        assertThatThrownBy(() -> applicationService.respond(appId, request, "owner-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no longer pending");

        verify(orgRepo, never()).findById(any());
        verify(appRepo, never()).save(any());
    }

    @Test
    @DisplayName("respond_applicationNotFound_throwsResourceNotFoundException")
    void respond_applicationNotFound_throwsResourceNotFoundException() {
        when(appRepo.findById("ghost-app")).thenReturn(Optional.empty());

        RespondApplicationRequest request = new RespondApplicationRequest();
        request.setStatus(ApplicationStatus.ACCEPTED);

        assertThatThrownBy(() -> applicationService.respond("ghost-app", request, "owner-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ghost-app");
    }

    @Test
    @DisplayName("respond_invalidStatus_throwsBusinessRuleException")
    void respond_invalidStatus_throwsBusinessRuleException() {
        String appId = "app-6";
        String orgId = "org-1";

        OrgApplication app = buildPendingApp(appId, orgId, "user-6");
        when(appRepo.findById(appId)).thenReturn(Optional.of(app));

        // Status validation throws BEFORE org/member checks — no stubs needed for them
        RespondApplicationRequest request = new RespondApplicationRequest();
        request.setStatus(ApplicationStatus.PENDING); // PENDING is not a valid response status

        assertThatThrownBy(() -> applicationService.respond(appId, request, "owner-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ACCEPTED or REJECTED");
    }

    /* ── withdraw() ──────────────────────────────────────────────────────── */

    @Test
    @DisplayName("withdraw_ownApplication_updatesStatusToWithdrawn")
    void withdraw_ownApplication_updatesStatusToWithdrawn() {
        String appId      = "app-w";
        String orgId      = "org-1";
        String applicantId = "user-w";

        OrgApplication app = buildPendingApp(appId, orgId, applicantId);
        when(appRepo.findById(appId)).thenReturn(Optional.of(app));
        when(appRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        applicationService.withdraw(appId, applicantId);

        ArgumentCaptor<OrgApplication> captor = ArgumentCaptor.forClass(OrgApplication.class);
        verify(appRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
        verify(auditRepo).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("withdraw_notOwnerOfApplication_throwsBusinessRuleException")
    void withdraw_notOwnerOfApplication_throwsBusinessRuleException() {
        String appId      = "app-w2";
        OrgApplication app = buildPendingApp(appId, "org-1", "real-owner");
        when(appRepo.findById(appId)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> applicationService.withdraw(appId, "imposter"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("your own application");

        verify(appRepo, never()).save(any());
    }

    @Test
    @DisplayName("withdraw_nonPendingApplication_throwsBusinessRuleException")
    void withdraw_nonPendingApplication_throwsBusinessRuleException() {
        String appId      = "app-w3";
        String applicantId = "user-w3";
        OrgApplication app = buildPendingApp(appId, "org-1", applicantId);
        app.setStatus(ApplicationStatus.ACCEPTED);
        when(appRepo.findById(appId)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> applicationService.withdraw(appId, applicantId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("pending applications");

        verify(appRepo, never()).save(any());
    }

    /* ── getOrgApplications() ────────────────────────────────────────────── */

    @Test
    @DisplayName("getOrgApplications_returnsPage")
    void getOrgApplications_returnsPage() {
        String orgId   = "org-1";
        Pageable pageable = PageRequest.of(0, 10);

        OrgApplication app = buildPendingApp("app-1", orgId, "user-1");
        Page<OrgApplication> page = new PageImpl<>(List.of(app));
        when(appRepo.findByOrganizationId(orgId, pageable)).thenReturn(page);

        Page<ApplicationResponse> result = applicationService.getOrgApplications(orgId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo("app-1");
    }

    /* ── getMyApplications() ─────────────────────────────────────────────── */

    @Test
    @DisplayName("getMyApplications_returnsAllForApplicant")
    void getMyApplications_returnsAllForApplicant() {
        String applicantId = "user-me";
        OrgApplication a1 = buildPendingApp("app-a1", "org-1", applicantId);
        OrgApplication a2 = buildPendingApp("app-a2", "org-2", applicantId);
        when(appRepo.findByApplicantId(applicantId)).thenReturn(List.of(a1, a2));

        List<ApplicationResponse> results = applicationService.getMyApplications(applicantId);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(ApplicationResponse::getApplicantId)
                .containsOnly(applicantId);
    }
}
