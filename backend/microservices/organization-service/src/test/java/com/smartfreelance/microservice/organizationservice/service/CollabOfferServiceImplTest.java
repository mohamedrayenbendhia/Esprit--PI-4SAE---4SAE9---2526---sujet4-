package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.ApplyCollabOfferRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.CreateCollabOfferRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.RespondCollabApplicationRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.CollabApplicationResponse;
import com.smartfreelance.microservice.organizationservice.dto.response.CollabOfferResponse;
import com.smartfreelance.microservice.organizationservice.entity.CollabApplication;
import com.smartfreelance.microservice.organizationservice.entity.CollabOffer;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.enums.CollabApplicationStatus;
import com.smartfreelance.microservice.organizationservice.enums.CollabOfferStatus;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationStatus;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.AuditLogRepository;
import com.smartfreelance.microservice.organizationservice.repository.CollabApplicationRepository;
import com.smartfreelance.microservice.organizationservice.repository.CollabOfferRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationMemberRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CollabOfferServiceImpl Unit Tests")
class CollabOfferServiceImplTest {

    @Mock private CollabOfferRepository offerRepo;
    @Mock private CollabApplicationRepository applicationRepo;
    @Mock private OrganizationRepository orgRepo;
    @Mock private OrganizationMemberRepository memberRepo;
    @Mock private AuditLogRepository auditRepo;

    @InjectMocks private CollabOfferServiceImpl collabOfferService;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Organization buildActiveOrg(String orgId, String ownerId) {
        return Organization.builder()
                .id(orgId).name("Acme Corp").ownerId(ownerId)
                .status(OrganizationStatus.ACTIVE).build();
    }

    private Organization buildInactiveOrg(String orgId, String ownerId) {
        return Organization.builder()
                .id(orgId).name("Acme Corp").ownerId(ownerId)
                .status(OrganizationStatus.SUSPENDED).build();
    }

    private CollabOffer buildOffer(String id, String orgId, String createdBy, CollabOfferStatus status) {
        return CollabOffer.builder()
                .id(id).organizationId(orgId).createdBy(createdBy)
                .title("Need a designer").description("UI/UX design project")
                .status(status).build();
    }

    private CollabApplication buildApplication(String id, String offerId, String orgId, String applicantId,
                                                CollabApplicationStatus status) {
        return CollabApplication.builder()
                .id(id).offerId(offerId).organizationId(orgId)
                .applicantId(applicantId).message("I am interested")
                .status(status).build();
    }

    private void mockOwnerOrManager(String orgId, String userId) {
        Organization org = buildActiveOrg(orgId, userId);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
    }

    // ── createOffer() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("createOffer_ownerCreates_savesAndAudits")
    void createOffer_ownerCreates_savesAndAudits() {
        String orgId = "org-1";
        String ownerId = "owner-1";

        // requireActiveOrg
        Organization activeOrg = buildActiveOrg(orgId, ownerId);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(activeOrg));
        // requireOwnerOrManager → second call to orgRepo
        when(memberRepo.findByOrganizationIdAndUserId(orgId, ownerId)).thenReturn(Optional.empty());

        CollabOffer saved = buildOffer("offer-1", orgId, ownerId, CollabOfferStatus.OPEN);
        when(offerRepo.save(any(CollabOffer.class))).thenReturn(saved);
        when(auditRepo.save(any())).thenReturn(null);
        // toOfferResponse counts
        when(applicationRepo.countByOfferIdAndStatus(eq("offer-1"), any())).thenReturn(0L);

        CreateCollabOfferRequest req = new CreateCollabOfferRequest();
        req.setTitle("Need a designer");
        req.setDescription("UI/UX design project");

        CollabOfferResponse response = collabOfferService.createOffer(orgId, req, ownerId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("offer-1");
        assertThat(response.getStatus()).isEqualTo(CollabOfferStatus.OPEN);
        verify(offerRepo).save(any(CollabOffer.class));
        verify(auditRepo).save(any());
    }

    @Test
    @DisplayName("createOffer_orgNotActive_throwsBusinessRuleException")
    void createOffer_orgNotActive_throwsBusinessRuleException() {
        String orgId = "org-1";
        Organization inactiveOrg = buildInactiveOrg(orgId, "owner-1");
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(inactiveOrg));

        CreateCollabOfferRequest req = new CreateCollabOfferRequest();
        req.setTitle("Need a designer");
        req.setDescription("UI/UX design project");

        assertThatThrownBy(() -> collabOfferService.createOffer(orgId, req, "owner-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("active");

        verify(offerRepo, never()).save(any());
    }

    @Test
    @DisplayName("createOffer_orgNotFound_throwsResourceNotFoundException")
    void createOffer_orgNotFound_throwsResourceNotFoundException() {
        when(orgRepo.findById("missing")).thenReturn(Optional.empty());

        CreateCollabOfferRequest req = new CreateCollabOfferRequest();
        req.setTitle("Need a designer");
        req.setDescription("Description");

        assertThatThrownBy(() -> collabOfferService.createOffer("missing", req, "owner-1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getOffer() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getOffer_existingOffer_returnsResponse")
    void getOffer_existingOffer_returnsResponse() {
        CollabOffer offer = buildOffer("offer-1", "org-1", "owner-1", CollabOfferStatus.OPEN);
        when(offerRepo.findById("offer-1")).thenReturn(Optional.of(offer));
        when(applicationRepo.countByOfferIdAndStatus(eq("offer-1"), any())).thenReturn(0L);

        CollabOfferResponse response = collabOfferService.getOffer("offer-1");

        assertThat(response.getId()).isEqualTo("offer-1");
        assertThat(response.getStatus()).isEqualTo(CollabOfferStatus.OPEN);
    }

    @Test
    @DisplayName("getOffer_notFound_throwsResourceNotFoundException")
    void getOffer_notFound_throwsResourceNotFoundException() {
        when(offerRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collabOfferService.getOffer("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Offre introuvable");
    }

    @Test
    @DisplayName("getOffer_closedOffer_returnsClosedStatus")
    void getOffer_closedOffer_returnsClosedStatus() {
        CollabOffer offer = buildOffer("offer-2", "org-1", "owner-1", CollabOfferStatus.CLOSED);
        when(offerRepo.findById("offer-2")).thenReturn(Optional.of(offer));
        when(applicationRepo.countByOfferIdAndStatus(eq("offer-2"), any())).thenReturn(0L);

        CollabOfferResponse response = collabOfferService.getOffer("offer-2");

        assertThat(response.getStatus()).isEqualTo(CollabOfferStatus.CLOSED);
    }

    // ── closeOffer() ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("closeOffer_openOffer_closesAndAudits")
    void closeOffer_openOffer_closesAndAudits() {
        String orgId = "org-1";
        String ownerId = "owner-1";
        CollabOffer offer = buildOffer("offer-1", orgId, ownerId, CollabOfferStatus.OPEN);
        when(offerRepo.findById("offer-1")).thenReturn(Optional.of(offer));

        Organization org = buildActiveOrg(orgId, ownerId);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(memberRepo.findByOrganizationIdAndUserId(orgId, ownerId)).thenReturn(Optional.empty());

        CollabOffer saved = buildOffer("offer-1", orgId, ownerId, CollabOfferStatus.CLOSED);
        when(offerRepo.save(any())).thenReturn(saved);
        when(auditRepo.save(any())).thenReturn(null);
        when(applicationRepo.countByOfferIdAndStatus(eq("offer-1"), any())).thenReturn(0L);

        CollabOfferResponse response = collabOfferService.closeOffer("offer-1", ownerId);

        assertThat(response.getStatus()).isEqualTo(CollabOfferStatus.CLOSED);
        verify(auditRepo).save(any());
    }

    @Test
    @DisplayName("closeOffer_notOpenOffer_throwsBusinessRuleException")
    void closeOffer_notOpenOffer_throwsBusinessRuleException() {
        String orgId = "org-1";
        String ownerId = "owner-1";
        CollabOffer offer = buildOffer("offer-1", orgId, ownerId, CollabOfferStatus.CLOSED);
        when(offerRepo.findById("offer-1")).thenReturn(Optional.of(offer));

        Organization org = buildActiveOrg(orgId, ownerId);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(memberRepo.findByOrganizationIdAndUserId(orgId, ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collabOfferService.closeOffer("offer-1", ownerId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ouverte");
    }

    @Test
    @DisplayName("closeOffer_offerNotFound_throwsResourceNotFoundException")
    void closeOffer_offerNotFound_throwsResourceNotFoundException() {
        when(offerRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collabOfferService.closeOffer("missing", "owner-1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── cancelOffer() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelOffer_openOffer_cancelsAndAudits")
    void cancelOffer_openOffer_cancelsAndAudits() {
        String orgId = "org-1";
        String ownerId = "owner-1";
        CollabOffer offer = buildOffer("offer-1", orgId, ownerId, CollabOfferStatus.OPEN);
        when(offerRepo.findById("offer-1")).thenReturn(Optional.of(offer));

        Organization org = buildActiveOrg(orgId, ownerId);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(memberRepo.findByOrganizationIdAndUserId(orgId, ownerId)).thenReturn(Optional.empty());

        CollabOffer saved = buildOffer("offer-1", orgId, ownerId, CollabOfferStatus.CANCELLED);
        when(offerRepo.save(any())).thenReturn(saved);
        when(auditRepo.save(any())).thenReturn(null);
        when(applicationRepo.countByOfferIdAndStatus(eq("offer-1"), any())).thenReturn(0L);

        CollabOfferResponse response = collabOfferService.cancelOffer("offer-1", ownerId);

        assertThat(response.getStatus()).isEqualTo(CollabOfferStatus.CANCELLED);
        verify(auditRepo).save(any());
    }

    @Test
    @DisplayName("cancelOffer_alreadyCancelled_throwsBusinessRuleException")
    void cancelOffer_alreadyCancelled_throwsBusinessRuleException() {
        String orgId = "org-1";
        String ownerId = "owner-1";
        CollabOffer offer = buildOffer("offer-1", orgId, ownerId, CollabOfferStatus.CANCELLED);
        when(offerRepo.findById("offer-1")).thenReturn(Optional.of(offer));

        Organization org = buildActiveOrg(orgId, ownerId);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(memberRepo.findByOrganizationIdAndUserId(orgId, ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collabOfferService.cancelOffer("offer-1", ownerId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("annulée");
    }

    @Test
    @DisplayName("cancelOffer_offerNotFound_throwsResourceNotFoundException")
    void cancelOffer_offerNotFound_throwsResourceNotFoundException() {
        when(offerRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collabOfferService.cancelOffer("missing", "owner-1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── apply() ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("apply_validRequest_savesApplicationAndAudits")
    void apply_validRequest_savesApplicationAndAudits() {
        String orgId = "org-1";
        String ownerId = "owner-1";
        String applicantId = "applicant-1";
        CollabOffer offer = buildOffer("offer-1", orgId, ownerId, CollabOfferStatus.OPEN);
        when(offerRepo.findById("offer-1")).thenReturn(Optional.of(offer));
        when(applicationRepo.countByOfferIdAndStatus("offer-1", CollabApplicationStatus.ACCEPTED)).thenReturn(0L);
        when(applicationRepo.existsByOfferIdAndApplicantIdAndStatus(
                "offer-1", applicantId, CollabApplicationStatus.PENDING)).thenReturn(false);

        CollabApplication saved = buildApplication("app-1", "offer-1", orgId, applicantId,
                CollabApplicationStatus.PENDING);
        when(applicationRepo.save(any())).thenReturn(saved);
        when(auditRepo.save(any())).thenReturn(null);

        ApplyCollabOfferRequest req = new ApplyCollabOfferRequest();
        req.setMessage("I am very interested");

        CollabApplicationResponse response = collabOfferService.apply("offer-1", req, applicantId);

        assertThat(response.getId()).isEqualTo("app-1");
        assertThat(response.getStatus()).isEqualTo(CollabApplicationStatus.PENDING);
        verify(applicationRepo).save(any(CollabApplication.class));
    }

    @Test
    @DisplayName("apply_offerNotOpen_throwsBusinessRuleException")
    void apply_offerNotOpen_throwsBusinessRuleException() {
        CollabOffer offer = buildOffer("offer-1", "org-1", "owner-1", CollabOfferStatus.CLOSED);
        when(offerRepo.findById("offer-1")).thenReturn(Optional.of(offer));

        ApplyCollabOfferRequest req = new ApplyCollabOfferRequest();
        req.setMessage("I want to apply");

        assertThatThrownBy(() -> collabOfferService.apply("offer-1", req, "applicant-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ouverte");
    }

    @Test
    @DisplayName("apply_deadlinePassed_throwsBusinessRuleException")
    void apply_deadlinePassed_throwsBusinessRuleException() {
        CollabOffer offer = buildOffer("offer-1", "org-1", "owner-1", CollabOfferStatus.OPEN);
        offer.setDeadlineDate(LocalDate.now().minusDays(1)); // past deadline
        when(offerRepo.findById("offer-1")).thenReturn(Optional.of(offer));

        ApplyCollabOfferRequest req = new ApplyCollabOfferRequest();
        req.setMessage("Applying after deadline");

        assertThatThrownBy(() -> collabOfferService.apply("offer-1", req, "applicant-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("date limite");
    }

    @Test
    @DisplayName("apply_duplicateApplication_throwsBusinessRuleException")
    void apply_duplicateApplication_throwsBusinessRuleException() {
        CollabOffer offer = buildOffer("offer-1", "org-1", "owner-1", CollabOfferStatus.OPEN);
        when(offerRepo.findById("offer-1")).thenReturn(Optional.of(offer));
        when(applicationRepo.countByOfferIdAndStatus("offer-1", CollabApplicationStatus.ACCEPTED)).thenReturn(0L);
        when(applicationRepo.existsByOfferIdAndApplicantIdAndStatus(
                "offer-1", "applicant-1", CollabApplicationStatus.PENDING)).thenReturn(true);

        ApplyCollabOfferRequest req = new ApplyCollabOfferRequest();
        req.setMessage("Applying again");

        assertThatThrownBy(() -> collabOfferService.apply("offer-1", req, "applicant-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("déjà une candidature");
    }

    @Test
    @DisplayName("apply_ownOfferCreator_throwsBusinessRuleException")
    void apply_ownOfferCreator_throwsBusinessRuleException() {
        String ownerId = "owner-1";
        CollabOffer offer = buildOffer("offer-1", "org-1", ownerId, CollabOfferStatus.OPEN);
        when(offerRepo.findById("offer-1")).thenReturn(Optional.of(offer));
        when(applicationRepo.countByOfferIdAndStatus("offer-1", CollabApplicationStatus.ACCEPTED)).thenReturn(0L);
        when(applicationRepo.existsByOfferIdAndApplicantIdAndStatus(
                "offer-1", ownerId, CollabApplicationStatus.PENDING)).thenReturn(false);

        ApplyCollabOfferRequest req = new ApplyCollabOfferRequest();
        req.setMessage("Applying to my own offer");

        assertThatThrownBy(() -> collabOfferService.apply("offer-1", req, ownerId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("propre offre");
    }

    // ── withdraw() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("withdraw_pendingApplication_setsWithdrawnAndAudits")
    void withdraw_pendingApplication_setsWithdrawnAndAudits() {
        String applicantId = "applicant-1";
        CollabApplication app = buildApplication("app-1", "offer-1", "org-1", applicantId,
                CollabApplicationStatus.PENDING);
        when(applicationRepo.findById("app-1")).thenReturn(Optional.of(app));
        when(applicationRepo.save(any())).thenReturn(app);
        when(auditRepo.save(any())).thenReturn(null);

        collabOfferService.withdraw("app-1", applicantId);

        ArgumentCaptor<CollabApplication> captor = ArgumentCaptor.forClass(CollabApplication.class);
        verify(applicationRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CollabApplicationStatus.WITHDRAWN);
    }

    @Test
    @DisplayName("withdraw_notApplicant_throwsBusinessRuleException")
    void withdraw_notApplicant_throwsBusinessRuleException() {
        CollabApplication app = buildApplication("app-1", "offer-1", "org-1", "applicant-1",
                CollabApplicationStatus.PENDING);
        when(applicationRepo.findById("app-1")).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> collabOfferService.withdraw("app-1", "other-user"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("propre candidature");
    }

    @Test
    @DisplayName("withdraw_notPendingApplication_throwsBusinessRuleException")
    void withdraw_notPendingApplication_throwsBusinessRuleException() {
        CollabApplication app = buildApplication("app-1", "offer-1", "org-1", "applicant-1",
                CollabApplicationStatus.ACCEPTED);
        when(applicationRepo.findById("app-1")).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> collabOfferService.withdraw("app-1", "applicant-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("en attente");
    }

    // ── getMyApplications() ───────────────────────────────────────────────────

    @Test
    @DisplayName("getMyApplications_withApplications_returnsList")
    void getMyApplications_withApplications_returnsList() {
        String applicantId = "applicant-1";
        CollabApplication app1 = buildApplication("app-1", "offer-1", "org-1", applicantId,
                CollabApplicationStatus.PENDING);
        CollabApplication app2 = buildApplication("app-2", "offer-2", "org-1", applicantId,
                CollabApplicationStatus.ACCEPTED);
        when(applicationRepo.findByApplicantId(applicantId)).thenReturn(List.of(app1, app2));
        when(offerRepo.findById("offer-1")).thenReturn(Optional.of(
                buildOffer("offer-1", "org-1", "owner-1", CollabOfferStatus.OPEN)));
        when(offerRepo.findById("offer-2")).thenReturn(Optional.of(
                buildOffer("offer-2", "org-1", "owner-1", CollabOfferStatus.CLOSED)));

        List<CollabApplicationResponse> result = collabOfferService.getMyApplications(applicantId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CollabApplicationResponse::getApplicantId)
                .containsOnly(applicantId);
    }

    @Test
    @DisplayName("getMyApplications_noApplications_returnsEmptyList")
    void getMyApplications_noApplications_returnsEmptyList() {
        String applicantId = "applicant-unknown";
        when(applicationRepo.findByApplicantId(applicantId)).thenReturn(List.of());

        List<CollabApplicationResponse> result = collabOfferService.getMyApplications(applicantId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getMyApplications_offerTitleMissing_usesEmptyString")
    void getMyApplications_offerTitleMissing_usesEmptyString() {
        String applicantId = "applicant-1";
        CollabApplication app = buildApplication("app-1", "offer-gone", "org-1", applicantId,
                CollabApplicationStatus.PENDING);
        when(applicationRepo.findByApplicantId(applicantId)).thenReturn(List.of(app));
        when(offerRepo.findById("offer-gone")).thenReturn(Optional.empty());

        List<CollabApplicationResponse> result = collabOfferService.getMyApplications(applicantId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOfferTitle()).isEmpty();
    }

    // ── getApplicationsForOffer() ─────────────────────────────────────────────

    @Test
    @DisplayName("getApplicationsForOffer_asManager_returnsPage")
    void getApplicationsForOffer_asManager_returnsPage() {
        String orgId = "org-1";
        String ownerId = "owner-1";
        CollabOffer offer = buildOffer("offer-1", orgId, ownerId, CollabOfferStatus.OPEN);
        when(offerRepo.findById("offer-1")).thenReturn(Optional.of(offer));

        Organization org = buildActiveOrg(orgId, ownerId);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(memberRepo.findByOrganizationIdAndUserId(orgId, ownerId)).thenReturn(Optional.empty());

        CollabApplication app = buildApplication("app-1", "offer-1", orgId, "applicant-1",
                CollabApplicationStatus.PENDING);
        Pageable pageable = PageRequest.of(0, 10);
        when(applicationRepo.findByOfferId("offer-1", pageable))
                .thenReturn(new PageImpl<>(List.of(app)));

        Page<CollabApplicationResponse> result = collabOfferService.getApplicationsForOffer("offer-1", pageable, ownerId);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo("app-1");
    }

    @Test
    @DisplayName("getApplicationsForOffer_offerNotFound_throwsResourceNotFoundException")
    void getApplicationsForOffer_offerNotFound_throwsResourceNotFoundException() {
        when(offerRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collabOfferService.getApplicationsForOffer(
                "missing", PageRequest.of(0, 10), "owner-1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
