package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.CreateRfqRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.RfqResponseRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.RfqResponse;
import com.smartfreelance.microservice.organizationservice.entity.OrgRfq;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.entity.OrganizationMember;
import com.smartfreelance.microservice.organizationservice.enums.MemberRole;
import com.smartfreelance.microservice.organizationservice.enums.RfqStatus;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.OrgRfqRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RfqServiceImpl Unit Tests")
class RfqServiceImplTest {

    @Mock private OrgRfqRepository rfqRepo;
    @Mock private OrganizationRepository orgRepo;
    @Mock private OrganizationMemberRepository memberRepo;
    @Mock private TrustScoreService trustScoreService;

    @InjectMocks private RfqServiceImpl rfqService;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Organization buildOrg(String orgId, String ownerId) {
        return Organization.builder()
                .id(orgId)
                .name("Acme Corp")
                .ownerId(ownerId)
                .build();
    }

    private OrgRfq buildRfq(String id, String orgId, String requesterId, RfqStatus status) {
        return OrgRfq.builder()
                .id(id)
                .organizationId(orgId)
                .requesterId(requesterId)
                .title("Need a Java developer")
                .description("We need a Java developer for 3 months")
                .status(status)
                .build();
    }

    private CreateRfqRequest buildCreateRequest() {
        CreateRfqRequest req = new CreateRfqRequest();
        req.setTitle("Need a Java developer");
        req.setDescription("We need a Java developer for 3 months");
        return req;
    }

    // ── create() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create_validRequest_savesAndReturnsRfq")
    void create_validRequest_savesAndReturnsRfq() {
        String orgId = "org-1";
        String requesterId = "user-1";
        CreateRfqRequest req = buildCreateRequest();

        OrgRfq saved = buildRfq("rfq-1", orgId, requesterId, RfqStatus.PENDING);
        when(rfqRepo.save(any(OrgRfq.class))).thenReturn(saved);

        RfqResponse response = rfqService.create(orgId, req, requesterId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("rfq-1");
        assertThat(response.getOrganizationId()).isEqualTo(orgId);
        assertThat(response.getRequesterId()).isEqualTo(requesterId);
        assertThat(response.getStatus()).isEqualTo(RfqStatus.PENDING);

        ArgumentCaptor<OrgRfq> captor = ArgumentCaptor.forClass(OrgRfq.class);
        verify(rfqRepo).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Need a Java developer");
        assertThat(captor.getValue().getOrganizationId()).isEqualTo(orgId);
    }

    @Test
    @DisplayName("create_withSkills_setsSkillsNeeded")
    void create_withSkills_setsSkillsNeeded() {
        String orgId = "org-1";
        String requesterId = "user-1";
        CreateRfqRequest req = buildCreateRequest();
        req.setSkillsNeeded(List.of("Java", "Spring Boot"));

        OrgRfq saved = buildRfq("rfq-1", orgId, requesterId, RfqStatus.PENDING);
        when(rfqRepo.save(any(OrgRfq.class))).thenReturn(saved);

        RfqResponse response = rfqService.create(orgId, req, requesterId);

        assertThat(response).isNotNull();
        ArgumentCaptor<OrgRfq> captor = ArgumentCaptor.forClass(OrgRfq.class);
        verify(rfqRepo).save(captor.capture());
        assertThat(captor.getValue().getSkillsNeeded()).contains("Java", "Spring Boot");
    }

    @Test
    @DisplayName("create_withNullSkills_setsEmptyList")
    void create_withNullSkills_setsEmptyList() {
        String orgId = "org-1";
        String requesterId = "user-1";
        CreateRfqRequest req = buildCreateRequest();
        req.setSkillsNeeded(null);

        OrgRfq saved = buildRfq("rfq-1", orgId, requesterId, RfqStatus.PENDING);
        when(rfqRepo.save(any(OrgRfq.class))).thenReturn(saved);

        rfqService.create(orgId, req, requesterId);

        ArgumentCaptor<OrgRfq> captor = ArgumentCaptor.forClass(OrgRfq.class);
        verify(rfqRepo).save(captor.capture());
        assertThat(captor.getValue().getSkillsNeeded()).isNotNull().isEmpty();
    }

    // ── getByOrg() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getByOrg_returnsPage")
    void getByOrg_returnsPage() {
        String orgId = "org-1";
        Pageable pageable = PageRequest.of(0, 10);

        OrgRfq rfq = buildRfq("rfq-1", orgId, "user-1", RfqStatus.PENDING);
        when(rfqRepo.findByOrganizationId(orgId, pageable))
                .thenReturn(new PageImpl<>(List.of(rfq)));

        Page<RfqResponse> result = rfqService.getByOrg(orgId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo("rfq-1");
    }

    @Test
    @DisplayName("getByOrg_emptyOrg_returnsEmptyPage")
    void getByOrg_emptyOrg_returnsEmptyPage() {
        String orgId = "org-1";
        Pageable pageable = PageRequest.of(0, 10);

        when(rfqRepo.findByOrganizationId(orgId, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        Page<RfqResponse> result = rfqService.getByOrg(orgId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("getByOrg_multipleRfqs_returnsAll")
    void getByOrg_multipleRfqs_returnsAll() {
        String orgId = "org-1";
        Pageable pageable = PageRequest.of(0, 10);

        OrgRfq r1 = buildRfq("rfq-1", orgId, "user-1", RfqStatus.PENDING);
        OrgRfq r2 = buildRfq("rfq-2", orgId, "user-2", RfqStatus.RESPONDED);
        when(rfqRepo.findByOrganizationId(orgId, pageable))
                .thenReturn(new PageImpl<>(List.of(r1, r2)));

        Page<RfqResponse> result = rfqService.getByOrg(orgId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    // ── respond() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("respond_ownerResponds_setsStatusAndRecomputes")
    void respond_ownerResponds_setsStatusAndRecomputes() {
        String rfqId = "rfq-1";
        String orgId = "org-1";
        String ownerId = "owner-1";

        OrgRfq rfq = buildRfq(rfqId, orgId, "requester-1", RfqStatus.PENDING);
        when(rfqRepo.findById(rfqId)).thenReturn(Optional.of(rfq));

        Organization org = buildOrg(orgId, ownerId);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));

        OrgRfq saved = buildRfq(rfqId, orgId, "requester-1", RfqStatus.RESPONDED);
        saved.setResponseMessage("We can help you!");
        saved.setRespondedById(ownerId);
        when(rfqRepo.save(any(OrgRfq.class))).thenReturn(saved);

        RfqResponseRequest req = new RfqResponseRequest();
        req.setResponseMessage("We can help you!");

        RfqResponse response = rfqService.respond(rfqId, req, ownerId);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(RfqStatus.RESPONDED);
        verify(trustScoreService).recompute(orgId);
    }

    @Test
    @DisplayName("respond_rfqNotFound_throwsResourceNotFoundException")
    void respond_rfqNotFound_throwsResourceNotFoundException() {
        when(rfqRepo.findById("missing")).thenReturn(Optional.empty());

        RfqResponseRequest req = new RfqResponseRequest();
        req.setResponseMessage("Reply");

        assertThatThrownBy(() -> rfqService.respond("missing", req, "user-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RFQ not found");
    }

    @Test
    @DisplayName("respond_rfqNotPending_throwsBusinessRuleException")
    void respond_rfqNotPending_throwsBusinessRuleException() {
        String rfqId = "rfq-1";
        OrgRfq rfq = buildRfq(rfqId, "org-1", "user-1", RfqStatus.RESPONDED);
        when(rfqRepo.findById(rfqId)).thenReturn(Optional.of(rfq));

        RfqResponseRequest req = new RfqResponseRequest();
        req.setResponseMessage("Reply");

        assertThatThrownBy(() -> rfqService.respond(rfqId, req, "responder-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no longer pending");
    }

    @Test
    @DisplayName("respond_nonMember_throwsBusinessRuleException")
    void respond_nonMember_throwsBusinessRuleException() {
        String rfqId = "rfq-1";
        String orgId = "org-1";
        String ownerId = "owner-1";
        String nonMemberId = "stranger";

        OrgRfq rfq = buildRfq(rfqId, orgId, "requester-1", RfqStatus.PENDING);
        when(rfqRepo.findById(rfqId)).thenReturn(Optional.of(rfq));

        Organization org = buildOrg(orgId, ownerId);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));

        when(memberRepo.findByOrganizationIdAndUserId(orgId, nonMemberId))
                .thenReturn(Optional.empty());

        RfqResponseRequest req = new RfqResponseRequest();
        req.setResponseMessage("Unauthorized reply");

        assertThatThrownBy(() -> rfqService.respond(rfqId, req, nonMemberId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("owner or manager");
    }

    // ── close() ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("close_byOwner_setsStatusClosedAndRecomputes")
    void close_byOwner_setsStatusClosedAndRecomputes() {
        String rfqId = "rfq-1";
        String orgId = "org-1";
        String ownerId = "owner-1";

        OrgRfq rfq = buildRfq(rfqId, orgId, "requester-1", RfqStatus.PENDING);
        when(rfqRepo.findById(rfqId)).thenReturn(Optional.of(rfq));

        Organization org = buildOrg(orgId, ownerId);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));

        OrgRfq saved = buildRfq(rfqId, orgId, "requester-1", RfqStatus.CLOSED);
        when(rfqRepo.save(any())).thenReturn(saved);

        rfqService.close(rfqId, ownerId);

        ArgumentCaptor<OrgRfq> captor = ArgumentCaptor.forClass(OrgRfq.class);
        verify(rfqRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(RfqStatus.CLOSED);
        verify(trustScoreService).recompute(orgId);
    }

    @Test
    @DisplayName("close_rfqNotFound_throwsResourceNotFoundException")
    void close_rfqNotFound_throwsResourceNotFoundException() {
        when(rfqRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rfqService.close("missing", "user-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RFQ not found");
    }

    @Test
    @DisplayName("close_byManager_closesSuccessfully")
    void close_byManager_closesSuccessfully() {
        String rfqId = "rfq-1";
        String orgId = "org-1";
        String managerId = "manager-1";

        OrgRfq rfq = buildRfq(rfqId, orgId, "requester-1", RfqStatus.PENDING);
        when(rfqRepo.findById(rfqId)).thenReturn(Optional.of(rfq));

        Organization org = buildOrg(orgId, "owner-1");
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));

        OrganizationMember member = OrganizationMember.builder()
                .organizationId(orgId).userId(managerId).role(MemberRole.MANAGER).build();
        when(memberRepo.findByOrganizationIdAndUserId(orgId, managerId))
                .thenReturn(Optional.of(member));

        OrgRfq saved = buildRfq(rfqId, orgId, "requester-1", RfqStatus.CLOSED);
        when(rfqRepo.save(any())).thenReturn(saved);

        rfqService.close(rfqId, managerId);

        verify(rfqRepo).save(any());
        verify(trustScoreService).recompute(orgId);
    }

    @Test
    @DisplayName("close_byNonMember_throwsBusinessRuleException")
    void close_byNonMember_throwsBusinessRuleException() {
        String rfqId = "rfq-1";
        String orgId = "org-1";
        String nonMemberId = "stranger";

        OrgRfq rfq = buildRfq(rfqId, orgId, "requester-1", RfqStatus.PENDING);
        when(rfqRepo.findById(rfqId)).thenReturn(Optional.of(rfq));

        Organization org = buildOrg(orgId, "owner-1");
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));

        when(memberRepo.findByOrganizationIdAndUserId(orgId, nonMemberId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> rfqService.close(rfqId, nonMemberId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("owner or a manager");
    }

    // ── getMyRfqs() ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getMyRfqs_withRfqs_returnsAllForRequester")
    void getMyRfqs_withRfqs_returnsAllForRequester() {
        String requesterId = "user-1";
        OrgRfq r1 = buildRfq("rfq-1", "org-1", requesterId, RfqStatus.PENDING);
        OrgRfq r2 = buildRfq("rfq-2", "org-2", requesterId, RfqStatus.RESPONDED);
        when(rfqRepo.findByRequesterId(requesterId)).thenReturn(List.of(r1, r2));

        List<RfqResponse> result = rfqService.getMyRfqs(requesterId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(RfqResponse::getRequesterId).containsOnly(requesterId);
    }

    @Test
    @DisplayName("getMyRfqs_noRfqs_returnsEmptyList")
    void getMyRfqs_noRfqs_returnsEmptyList() {
        String requesterId = "user-unknown";
        when(rfqRepo.findByRequesterId(requesterId)).thenReturn(List.of());

        List<RfqResponse> result = rfqService.getMyRfqs(requesterId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getMyRfqs_singleRfq_returnsSingleElement")
    void getMyRfqs_singleRfq_returnsSingleElement() {
        String requesterId = "user-1";
        OrgRfq rfq = buildRfq("rfq-1", "org-1", requesterId, RfqStatus.CLOSED);
        when(rfqRepo.findByRequesterId(requesterId)).thenReturn(List.of(rfq));

        List<RfqResponse> result = rfqService.getMyRfqs(requesterId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(RfqStatus.CLOSED);
    }
}
