package com.smartfreelance.microservice.organizationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartfreelance.microservice.organizationservice.dto.request.ApplyCollabOfferRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.CreateCollabOfferRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.RespondCollabApplicationRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.CollabApplicationResponse;
import com.smartfreelance.microservice.organizationservice.dto.response.CollabOfferResponse;
import com.smartfreelance.microservice.organizationservice.enums.CollabApplicationStatus;
import com.smartfreelance.microservice.organizationservice.enums.CollabOfferStatus;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.GlobalExceptionHandler;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.service.CollabOfferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CollabOfferController Unit Tests")
class CollabOfferControllerTest {

    @Mock private CollabOfferService collabOfferService;
    @InjectMocks private CollabOfferController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private static final String USER_ID = "user-1";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Authentication mockAuth() {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(USER_ID);
        return auth;
    }

    private CollabOfferResponse buildOfferResponse(String id, CollabOfferStatus status) {
        return CollabOfferResponse.builder()
                .id(id)
                .organizationId("org-1")
                .createdBy(USER_ID)
                .title("Need a designer")
                .status(status)
                .build();
    }

    private CollabApplicationResponse buildApplicationResponse(String id, CollabApplicationStatus status) {
        return CollabApplicationResponse.builder()
                .id(id)
                .offerId("offer-1")
                .applicantId(USER_ID)
                .status(status)
                .offerTitle("Need a designer")
                .build();
    }

    private CreateCollabOfferRequest buildCreateOfferRequest() {
        CreateCollabOfferRequest req = new CreateCollabOfferRequest();
        req.setTitle("Need a designer");
        req.setDescription("We need a UI/UX designer for 3 weeks");
        return req;
    }

    // ── POST /api/organizations/{orgId}/collab-offers ─────────────────────────

    @Test
    @DisplayName("createOffer_validRequest_returns201")
    void createOffer_validRequest_returns201() throws Exception {
        CreateCollabOfferRequest req = buildCreateOfferRequest();
        CollabOfferResponse response = buildOfferResponse("offer-1", CollabOfferStatus.OPEN);
        when(collabOfferService.createOffer(eq("org-1"), any(CreateCollabOfferRequest.class), eq(USER_ID)))
                .thenReturn(response);

        mockMvc.perform(post("/api/organizations/org-1/collab-offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("offer-1"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @DisplayName("createOffer_orgNotActive_returns422")
    void createOffer_orgNotActive_returns422() throws Exception {
        CreateCollabOfferRequest req = buildCreateOfferRequest();
        when(collabOfferService.createOffer(eq("org-1"), any(), eq(USER_ID)))
                .thenThrow(new BusinessRuleException("L'organisation n'est pas active."));

        mockMvc.perform(post("/api/organizations/org-1/collab-offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("createOffer_missingTitle_returns400")
    void createOffer_missingTitle_returns400() throws Exception {
        CreateCollabOfferRequest req = new CreateCollabOfferRequest();
        req.setTitle(""); // blank title
        req.setDescription("some desc");

        mockMvc.perform(post("/api/organizations/org-1/collab-offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/organizations/{orgId}/collab-offers ──────────────────────────

    @Test
    @DisplayName("listOffers_asManager_returnsAllOffers")
    void listOffers_asManager_returnsAllOffers() throws Exception {
        CollabOfferResponse r1 = buildOfferResponse("offer-1", CollabOfferStatus.OPEN);
        CollabOfferResponse r2 = buildOfferResponse("offer-2", CollabOfferStatus.CLOSED);
        when(collabOfferService.getOrgOffers(eq("org-1"), any(), eq(USER_ID)))
                .thenReturn(new PageImpl<>(List.of(r1, r2), PageRequest.of(0, 10), 2));

        mockMvc.perform(get("/api/organizations/org-1/collab-offers")
                        .principal(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("listOffers_asPublicUser_returnsOnlyOpenOffers")
    void listOffers_asPublicUser_returnsOnlyOpenOffers() throws Exception {
        CollabOfferResponse r = buildOfferResponse("offer-1", CollabOfferStatus.OPEN);
        when(collabOfferService.getOrgOffers(eq("org-1"), any(), eq(USER_ID)))
                .thenReturn(new PageImpl<>(List.of(r), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/organizations/org-1/collab-offers")
                        .principal(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"));
    }

    @Test
    @DisplayName("listOffers_emptyOrg_returnsEmptyPage")
    void listOffers_emptyOrg_returnsEmptyPage() throws Exception {
        when(collabOfferService.getOrgOffers(eq("org-1"), any(), eq(USER_ID)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/organizations/org-1/collab-offers")
                        .principal(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ── GET /api/organizations/{orgId}/collab-offers/{offerId} ───────────────

    @Test
    @DisplayName("getOffer_existingOffer_returns200")
    void getOffer_existingOffer_returns200() throws Exception {
        CollabOfferResponse response = buildOfferResponse("offer-1", CollabOfferStatus.OPEN);
        when(collabOfferService.getOffer("offer-1")).thenReturn(response);

        mockMvc.perform(get("/api/organizations/org-1/collab-offers/offer-1")
                        .principal(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("offer-1"));
    }

    @Test
    @DisplayName("getOffer_notFound_returns404")
    void getOffer_notFound_returns404() throws Exception {
        when(collabOfferService.getOffer("missing"))
                .thenThrow(new ResourceNotFoundException("Offre introuvable : missing"));

        mockMvc.perform(get("/api/organizations/org-1/collab-offers/missing")
                        .principal(mockAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("getOffer_closedOffer_returns200WithClosedStatus")
    void getOffer_closedOffer_returns200WithClosedStatus() throws Exception {
        CollabOfferResponse response = buildOfferResponse("offer-2", CollabOfferStatus.CLOSED);
        when(collabOfferService.getOffer("offer-2")).thenReturn(response);

        mockMvc.perform(get("/api/organizations/org-1/collab-offers/offer-2")
                        .principal(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    // ── POST /api/organizations/{orgId}/collab-offers/{offerId}/close ─────────

    @Test
    @DisplayName("closeOffer_openOffer_returns200WithClosedStatus")
    void closeOffer_openOffer_returns200WithClosedStatus() throws Exception {
        CollabOfferResponse response = buildOfferResponse("offer-1", CollabOfferStatus.CLOSED);
        when(collabOfferService.closeOffer("offer-1", USER_ID)).thenReturn(response);

        mockMvc.perform(post("/api/organizations/org-1/collab-offers/offer-1/close")
                        .principal(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    @DisplayName("closeOffer_offerNotFound_returns404")
    void closeOffer_offerNotFound_returns404() throws Exception {
        when(collabOfferService.closeOffer("missing", USER_ID))
                .thenThrow(new ResourceNotFoundException("Offre introuvable : missing"));

        mockMvc.perform(post("/api/organizations/org-1/collab-offers/missing/close")
                        .principal(mockAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("closeOffer_alreadyClosed_returns422")
    void closeOffer_alreadyClosed_returns422() throws Exception {
        when(collabOfferService.closeOffer("offer-1", USER_ID))
                .thenThrow(new BusinessRuleException("Seule une offre ouverte peut être clôturée."));

        mockMvc.perform(post("/api/organizations/org-1/collab-offers/offer-1/close")
                        .principal(mockAuth()))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── POST /api/organizations/{orgId}/collab-offers/{offerId}/cancel ────────

    @Test
    @DisplayName("cancelOffer_openOffer_returns200WithCancelledStatus")
    void cancelOffer_openOffer_returns200WithCancelledStatus() throws Exception {
        CollabOfferResponse response = buildOfferResponse("offer-1", CollabOfferStatus.CANCELLED);
        when(collabOfferService.cancelOffer("offer-1", USER_ID)).thenReturn(response);

        mockMvc.perform(post("/api/organizations/org-1/collab-offers/offer-1/cancel")
                        .principal(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("cancelOffer_alreadyCancelled_returns422")
    void cancelOffer_alreadyCancelled_returns422() throws Exception {
        when(collabOfferService.cancelOffer("offer-1", USER_ID))
                .thenThrow(new BusinessRuleException("L'offre est déjà annulée."));

        mockMvc.perform(post("/api/organizations/org-1/collab-offers/offer-1/cancel")
                        .principal(mockAuth()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("cancelOffer_offerNotFound_returns404")
    void cancelOffer_offerNotFound_returns404() throws Exception {
        when(collabOfferService.cancelOffer("missing", USER_ID))
                .thenThrow(new ResourceNotFoundException("Offre introuvable : missing"));

        mockMvc.perform(post("/api/organizations/org-1/collab-offers/missing/cancel")
                        .principal(mockAuth()))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/collab-offers/{offerId}/apply ───────────────────────────────

    @Test
    @DisplayName("apply_validRequest_returns201")
    void apply_validRequest_returns201() throws Exception {
        ApplyCollabOfferRequest req = new ApplyCollabOfferRequest();
        req.setMessage("I am very interested in this opportunity!");
        req.setPortfolioUrl("https://portfolio.example.com");

        CollabApplicationResponse response = buildApplicationResponse("app-1", CollabApplicationStatus.PENDING);
        when(collabOfferService.apply(eq("offer-1"), any(ApplyCollabOfferRequest.class), eq(USER_ID)))
                .thenReturn(response);

        mockMvc.perform(post("/api/collab-offers/offer-1/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("app-1"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("apply_offerNotOpen_returns422")
    void apply_offerNotOpen_returns422() throws Exception {
        ApplyCollabOfferRequest req = new ApplyCollabOfferRequest();
        req.setMessage("I want to apply!");

        when(collabOfferService.apply(eq("offer-1"), any(), eq(USER_ID)))
                .thenThrow(new BusinessRuleException("Cette offre n'est plus ouverte aux candidatures."));

        mockMvc.perform(post("/api/collab-offers/offer-1/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("apply_duplicateApplication_returns422")
    void apply_duplicateApplication_returns422() throws Exception {
        ApplyCollabOfferRequest req = new ApplyCollabOfferRequest();
        req.setMessage("Applying again!");

        when(collabOfferService.apply(eq("offer-1"), any(), eq(USER_ID)))
                .thenThrow(new BusinessRuleException("Vous avez déjà une candidature en attente pour cette offre."));

        mockMvc.perform(post("/api/collab-offers/offer-1/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── GET /api/collab-offers/{offerId}/applications ─────────────────────────

    @Test
    @DisplayName("getApplicationsForOffer_asManager_returnsPage")
    void getApplicationsForOffer_asManager_returnsPage() throws Exception {
        CollabApplicationResponse r = buildApplicationResponse("app-1", CollabApplicationStatus.PENDING);
        when(collabOfferService.getApplicationsForOffer(eq("offer-1"), any(), eq(USER_ID)))
                .thenReturn(new PageImpl<>(List.of(r), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/collab-offers/offer-1/applications")
                        .principal(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value("app-1"));
    }

    @Test
    @DisplayName("getApplicationsForOffer_notManager_returns422")
    void getApplicationsForOffer_notManager_returns422() throws Exception {
        when(collabOfferService.getApplicationsForOffer(eq("offer-1"), any(), eq(USER_ID)))
                .thenThrow(new BusinessRuleException("Seul un propriétaire ou un gestionnaire peut effectuer cette action."));

        mockMvc.perform(get("/api/collab-offers/offer-1/applications")
                        .principal(mockAuth()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("getApplicationsForOffer_offerNotFound_returns404")
    void getApplicationsForOffer_offerNotFound_returns404() throws Exception {
        when(collabOfferService.getApplicationsForOffer(eq("missing"), any(), eq(USER_ID)))
                .thenThrow(new ResourceNotFoundException("Offre introuvable : missing"));

        mockMvc.perform(get("/api/collab-offers/missing/applications")
                        .principal(mockAuth()))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/collab-offers/applications/{applicationId}/respond ──────────

    @Test
    @DisplayName("respond_acceptApplication_returns200")
    void respond_acceptApplication_returns200() throws Exception {
        RespondCollabApplicationRequest req = new RespondCollabApplicationRequest();
        req.setStatus(CollabApplicationStatus.ACCEPTED);

        CollabApplicationResponse response = buildApplicationResponse("app-1", CollabApplicationStatus.ACCEPTED);
        when(collabOfferService.respond(eq("app-1"), any(RespondCollabApplicationRequest.class), eq(USER_ID)))
                .thenReturn(response);

        mockMvc.perform(post("/api/collab-offers/applications/app-1/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("respond_rejectApplication_returns200")
    void respond_rejectApplication_returns200() throws Exception {
        RespondCollabApplicationRequest req = new RespondCollabApplicationRequest();
        req.setStatus(CollabApplicationStatus.REJECTED);
        req.setRejectionReason("Not the right fit");

        CollabApplicationResponse response = buildApplicationResponse("app-1", CollabApplicationStatus.REJECTED);
        response.setRejectionReason("Not the right fit");
        when(collabOfferService.respond(eq("app-1"), any(), eq(USER_ID)))
                .thenReturn(response);

        mockMvc.perform(post("/api/collab-offers/applications/app-1/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("respond_applicationNotFound_returns404")
    void respond_applicationNotFound_returns404() throws Exception {
        RespondCollabApplicationRequest req = new RespondCollabApplicationRequest();
        req.setStatus(CollabApplicationStatus.ACCEPTED);

        when(collabOfferService.respond(eq("missing"), any(), eq(USER_ID)))
                .thenThrow(new ResourceNotFoundException("Candidature introuvable : missing"));

        mockMvc.perform(post("/api/collab-offers/applications/missing/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/collab-offers/applications/{applicationId}/withdraw ────────

    @Test
    @DisplayName("withdraw_pendingApplication_returns204")
    void withdraw_pendingApplication_returns204() throws Exception {
        doNothing().when(collabOfferService).withdraw("app-1", USER_ID);

        mockMvc.perform(delete("/api/collab-offers/applications/app-1/withdraw")
                        .principal(mockAuth()))
                .andExpect(status().isNoContent());

        verify(collabOfferService).withdraw("app-1", USER_ID);
    }

    @Test
    @DisplayName("withdraw_applicationNotFound_returns404")
    void withdraw_applicationNotFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Candidature introuvable : missing"))
                .when(collabOfferService).withdraw("missing", USER_ID);

        mockMvc.perform(delete("/api/collab-offers/applications/missing/withdraw")
                        .principal(mockAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("withdraw_notPending_returns422")
    void withdraw_notPending_returns422() throws Exception {
        doThrow(new BusinessRuleException("Seules les candidatures en attente peuvent être retirées."))
                .when(collabOfferService).withdraw("app-1", USER_ID);

        mockMvc.perform(delete("/api/collab-offers/applications/app-1/withdraw")
                        .principal(mockAuth()))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── GET /api/collab-offers/my-applications ────────────────────────────────

    @Test
    @DisplayName("myApplications_withApplications_returnsList")
    void myApplications_withApplications_returnsList() throws Exception {
        List<CollabApplicationResponse> apps = List.of(
                buildApplicationResponse("app-1", CollabApplicationStatus.PENDING),
                buildApplicationResponse("app-2", CollabApplicationStatus.ACCEPTED)
        );
        when(collabOfferService.getMyApplications(USER_ID)).thenReturn(apps);

        mockMvc.perform(get("/api/collab-offers/my-applications")
                        .principal(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("myApplications_noApplications_returnsEmptyList")
    void myApplications_noApplications_returnsEmptyList() throws Exception {
        when(collabOfferService.getMyApplications(USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/collab-offers/my-applications")
                        .principal(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("myApplications_userIdExtractedFromAuth")
    void myApplications_userIdExtractedFromAuth() throws Exception {
        when(collabOfferService.getMyApplications(USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/collab-offers/my-applications")
                        .principal(mockAuth()))
                .andExpect(status().isOk());

        verify(collabOfferService).getMyApplications(USER_ID);
    }
}
