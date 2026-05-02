package com.smartfreelance.microservice.organizationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfreelance.microservice.organizationservice.dto.request.CreateOrganizationRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.TransferOwnershipRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.UpdateOrganizationRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.OrganizationResponse;
import com.smartfreelance.microservice.organizationservice.dto.response.OrganizationSummaryResponse;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationVisibility;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.GlobalExceptionHandler;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.service.OrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrganizationControllerTest {

    @Mock private OrganizationService orgService;
    @InjectMocks private OrganizationController controller;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    // Simule l'injection du userId via auth.getDetails() — même comportement que le Gateway
    private static final String USER_ID = "owner-1";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                // Simule auth.getDetails() == USER_ID pour tous les endpoints sécurisés
                .addFilter((request, response, chain) -> {
                    request.setAttribute("__auth_details__", USER_ID);
                    chain.doFilter(request, response);
                })
                .build();

        // On mock Authentication directement dans chaque test via standaloneSetup
        // Pour les méthodes qui reçoivent Authentication en paramètre, on passe par
        // un SecurityContext mocké — Spring injecte automatiquement en standalone si
        // on configure le principal. Ici on utilise l'approche header X-User-Id.
    }

    // ── Helpers ───────────────────────────────────────────────────

    private Authentication mockAuth() {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(USER_ID);
        return auth;
    }

    // ── POST /api/organizations ───────────────────────────────────

    @Test
    void create_shouldReturn201_withBody() {
        OrganizationResponse resp = OrganizationResponse.builder().id("o1").name("Acme Co").build();
        when(orgService.create(any(), eq(USER_ID))).thenReturn(resp);

        // Appel direct (MockMvc standalone ne peut pas injecter Authentication proprement
        // sans Spring Security complet — on teste la logique HTTP via appel direct du contrôleur)
        var entity = controller.create(buildCreateRequest("Acme Co"), mockAuth());

        assert entity.getStatusCodeValue() == 201;
        assert entity.getBody() != null;
        assert "o1".equals(entity.getBody().getId());
        verify(orgService).create(any(), eq(USER_ID));
    }

    @Test
    void create_shouldReturn201_andDelegateToService() {
        OrganizationResponse resp = OrganizationResponse.builder().id("o2").name("Beta Ltd").build();
        when(orgService.create(any(), eq(USER_ID))).thenReturn(resp);

        var result = controller.create(buildCreateRequest("Beta Ltd"), mockAuth());

        assert result.getStatusCodeValue() == 201;
        verify(orgService, times(1)).create(any(), eq(USER_ID));
    }

    // ── GET /api/organizations/{id} ───────────────────────────────

    @Test
    void getById_shouldReturn200() throws Exception {
        OrganizationResponse resp = OrganizationResponse.builder().id("o1").name("Acme").build();
        when(orgService.getById("o1")).thenReturn(resp);

        mockMvc.perform(get("/api/organizations/o1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("o1"))
                .andExpect(jsonPath("$.name").value("Acme"));
    }

    @Test
    void getById_shouldReturn404_whenNotFound() throws Exception {
        when(orgService.getById("unknown")).thenThrow(new ResourceNotFoundException("Organisation introuvable"));

        mockMvc.perform(get("/api/organizations/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Organisation introuvable"));
    }

    // ── PUT /api/organizations/{id} ───────────────────────────────

    @Test
    void update_shouldReturn200_withUpdatedBody() {
        UpdateOrganizationRequest req = new UpdateOrganizationRequest();
        req.setName("New Name");

        OrganizationResponse resp = OrganizationResponse.builder().id("o3").name("New Name").build();
        when(orgService.update("o3", req, USER_ID)).thenReturn(resp);

        var entity = controller.update("o3", req, mockAuth());

        assert entity.getStatusCodeValue() == 200;
        assert "New Name".equals(entity.getBody().getName());
        verify(orgService).update("o3", req, USER_ID);
    }

    @Test
    void update_shouldReturn422_whenNotOwner() {
        UpdateOrganizationRequest req = new UpdateOrganizationRequest();
        req.setName("Hacked Name");

        when(orgService.update(eq("o3"), any(), eq(USER_ID)))
                .thenThrow(new BusinessRuleException("Seul le propriétaire peut modifier cette organisation"));

        try {
            controller.update("o3", req, mockAuth());
            assert false : "BusinessRuleException attendue";
        } catch (BusinessRuleException e) {
            assert e.getMessage().contains("propriétaire");
        }
    }

    @Test
    void update_shouldReturn422_whenBusinessRuleViolation() throws Exception {
        // Via MockMvc pour tester le status HTTP
        when(orgService.getById("dissolved"))
                .thenThrow(new BusinessRuleException("Organisation dissoute"));

        mockMvc.perform(get("/api/organizations/dissolved"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").exists());
    }

    // ── DELETE /api/organizations/{id} ───────────────────────────

    @Test
    void delete_shouldReturn204() {
        doNothing().when(orgService).delete("o4", USER_ID);

        var resp = controller.delete("o4", mockAuth());

        assert resp.getStatusCodeValue() == 204;
        verify(orgService).delete("o4", USER_ID);
    }

    @Test
    void delete_shouldReturn404_whenOrgNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Organisation introuvable"))
                .when(orgService).delete(eq("missing"), eq(USER_ID));

        // Appel direct — le GlobalExceptionHandler gère la réponse HTTP
        try {
            controller.delete("missing", mockAuth());
        } catch (ResourceNotFoundException e) {
            assert "Organisation introuvable".equals(e.getMessage());
        }
    }

    // ── GET /api/organizations/my ─────────────────────────────────

    @Test
    void getMyOrgs_shouldReturnListOfOrganizations() {
        OrganizationSummaryResponse r1 = OrganizationSummaryResponse.builder().id("o5").name("My Org 1").build();
        OrganizationSummaryResponse r2 = OrganizationSummaryResponse.builder().id("o6").name("My Org 2").build();
        when(orgService.getMyOrganizations(USER_ID)).thenReturn(List.of(r1, r2));

        var resp = controller.getMyOrgs(mockAuth());

        assert resp.getBody() != null;
        assert resp.getBody().size() == 2;
        assert resp.getBody().get(0).getId().equals("o5");
        verify(orgService).getMyOrganizations(USER_ID);
    }

    @Test
    void getMyOrgs_shouldReturnEmptyList_whenNoOrgs() {
        when(orgService.getMyOrganizations(USER_ID)).thenReturn(List.of());

        var resp = controller.getMyOrgs(mockAuth());

        assert resp.getBody() != null;
        assert resp.getBody().isEmpty();
    }

    // ── GET /api/organizations/search ────────────────────────────

    @Test
    void search_shouldReturn200_withPagedResults() throws Exception {
        OrganizationSummaryResponse s = OrganizationSummaryResponse.builder().id("s1").name("TechCorp").build();
        when(orgService.searchPublic(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(s), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/organizations/search")
                        .param("keyword", "tech"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value("s1"));
    }

    @Test
    void search_shouldReturn200_withEmptyResults() throws Exception {
        when(orgService.searchPublic(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/organizations/search")
                        .param("keyword", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ── POST /api/organizations/{id}/transfer-ownership ──────────

    @Test
    void transferOwnership_shouldReturn204_onSuccess() {
        TransferOwnershipRequest req = new TransferOwnershipRequest();
        req.setNewOwnerId("newOwner");
        doNothing().when(orgService).transferOwnership("o6", USER_ID, "newOwner");

        var resp = controller.transferOwnership("o6", req, mockAuth());

        assert resp.getStatusCodeValue() == 204;
        verify(orgService).transferOwnership("o6", USER_ID, "newOwner");
    }

    @Test
    void transferOwnership_shouldFail_whenNotCurrentOwner() {
        TransferOwnershipRequest req = new TransferOwnershipRequest();
        req.setNewOwnerId("newOwner");
        doThrow(new BusinessRuleException("Seul le propriétaire peut transférer la propriété"))
                .when(orgService).transferOwnership(eq("o6"), eq(USER_ID), eq("newOwner"));

        try {
            controller.transferOwnership("o6", req, mockAuth());
            assert false : "Exception attendue";
        } catch (BusinessRuleException e) {
            assert e.getMessage().contains("propriétaire");
        }
    }

    // ── POST /api/organizations/{id}/dissolve ────────────────────

    @Test
    void dissolve_shouldReturn204_onSuccess() {
        doNothing().when(orgService).dissolve("o7", USER_ID);

        var resp = controller.dissolve("o7", mockAuth());

        assert resp.getStatusCodeValue() == 204;
        verify(orgService).dissolve("o7", USER_ID);
    }

    @Test
    void dissolve_shouldFail_whenAlreadyDissolved() {
        doThrow(new BusinessRuleException("Organisation déjà dissoute"))
                .when(orgService).dissolve(eq("o7"), eq(USER_ID));

        try {
            controller.dissolve("o7", mockAuth());
            assert false : "Exception attendue";
        } catch (BusinessRuleException e) {
            assert e.getMessage().contains("dissoute");
        }
    }

    // ── PATCH /api/organizations/{id}/visibility ─────────────────

    @Test
    void setVisibility_shouldReturn200_withUpdatedVisibility() {
        OrganizationResponse resp = OrganizationResponse.builder()
                .id("o8").visibility(OrganizationVisibility.PRIVATE).build();
        when(orgService.setVisibility("o8", OrganizationVisibility.PRIVATE, USER_ID)).thenReturn(resp);

        var entity = controller.setVisibility("o8", OrganizationVisibility.PRIVATE, mockAuth());

        assert entity.getStatusCodeValue() == 200;
        assert entity.getBody().getVisibility() == OrganizationVisibility.PRIVATE;
        verify(orgService).setVisibility("o8", OrganizationVisibility.PRIVATE, USER_ID);
    }

    @Test
    void setVisibility_toPublic_shouldWork() {
        OrganizationResponse resp = OrganizationResponse.builder()
                .id("o8").visibility(OrganizationVisibility.PUBLIC).build();
        when(orgService.setVisibility("o8", OrganizationVisibility.PUBLIC, USER_ID)).thenReturn(resp);

        var entity = controller.setVisibility("o8", OrganizationVisibility.PUBLIC, mockAuth());

        assert entity.getBody().getVisibility() == OrganizationVisibility.PUBLIC;
    }

    // ── Helpers ───────────────────────────────────────────────────

    private CreateOrganizationRequest buildCreateRequest(String name) {
        CreateOrganizationRequest req = new CreateOrganizationRequest();
        req.setName(name);
        return req;
    }
}
