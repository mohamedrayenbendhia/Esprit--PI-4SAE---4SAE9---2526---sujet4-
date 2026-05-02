package com.smartfreelance.microservice.organizationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfreelance.microservice.organizationservice.dto.request.AdminSuspendRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.AdminVerifyRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.OrganizationDashboardStats;
import com.smartfreelance.microservice.organizationservice.dto.response.OrganizationResponse;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationStatus;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.GlobalExceptionHandler;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.AuditLogRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationRepository;
import com.smartfreelance.microservice.organizationservice.service.AdminOrganizationService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminOrganizationControllerTest {

    @Mock private AdminOrganizationService adminService;
    @Mock private OrganizationRepository orgRepo;
    @Mock private AuditLogRepository auditLogRepo;
    @InjectMocks private AdminOrganizationController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String ADMIN_ID = "admin-1";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Authentication mockAdminAuth() {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(ADMIN_ID);
        return auth;
    }

    // ── GET /api/admin/organizations ─────────────────────────────

    @Test
    void listAll_shouldReturn200_withAllOrgs() throws Exception {
        OrganizationResponse org = OrganizationResponse.builder().id("o1").build();
        when(adminService.listAll(isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(org), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value("o1"));
    }

    @Test
    void listAll_shouldReturn200_filteredByStatus() throws Exception {
        OrganizationResponse org = OrganizationResponse.builder().id("o2").build();
        when(adminService.listAll(eq(OrganizationStatus.SUSPENDED), any()))
                .thenReturn(new PageImpl<>(List.of(org), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/organizations")
                        .param("status", "SUSPENDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ── POST /api/admin/organizations/{id}/verify ─────────────────

    @Test
    void verify_approve_shouldReturn200() {
        String requestJson = "{\"decision\":\"APPROVE\",\"adminNote\":\"Tout est en ordre\"}";
        OrganizationResponse resp = OrganizationResponse.builder().id("o1")
                .status(OrganizationStatus.ACTIVE).build();
        when(adminService.verify(eq("o1"), any(AdminVerifyRequest.class), eq(ADMIN_ID))).thenReturn(resp);

        var entity = controller.verify("o1", buildVerifyRequest("APPROVE", "Tout est en ordre"), mockAdminAuth());

        assert entity.getStatusCodeValue() == 200;
        assert entity.getBody().getStatus() == OrganizationStatus.ACTIVE;
        verify(adminService).verify(eq("o1"), any(), eq(ADMIN_ID));
    }

    @Test
    void verify_reject_shouldReturn200() {
        OrganizationResponse resp = OrganizationResponse.builder().id("o1")
                .status(OrganizationStatus.REJECTED).build();
        when(adminService.verify(eq("o1"), any(AdminVerifyRequest.class), eq(ADMIN_ID))).thenReturn(resp);

        var entity = controller.verify("o1", buildVerifyRequest("REJECT", "Documents manquants"), mockAdminAuth());

        assert entity.getStatusCodeValue() == 200;
        assert entity.getBody().getStatus() == OrganizationStatus.REJECTED;
    }

    @Test
    void verify_shouldFail_whenOrgNotFound() {
        doThrow(new ResourceNotFoundException("Organisation introuvable"))
                .when(adminService).verify(eq("missing"), any(), eq(ADMIN_ID));

        try {
            controller.verify("missing", buildVerifyRequest("APPROVE", "ok"), mockAdminAuth());
            assert false : "Exception attendue";
        } catch (ResourceNotFoundException e) {
            assert e.getMessage().contains("Organisation");
        }
    }

    @Test
    void verify_shouldFail_whenAlreadyVerified() {
        doThrow(new BusinessRuleException("Organisation déjà vérifiée"))
                .when(adminService).verify(eq("o1"), any(), eq(ADMIN_ID));

        try {
            controller.verify("o1", buildVerifyRequest("APPROVE", "ok"), mockAdminAuth());
            assert false : "Exception attendue";
        } catch (BusinessRuleException e) {
            assert e.getMessage().contains("vérifiée");
        }
    }

    // ── POST /api/admin/organizations/{id}/suspend ────────────────

    @Test
    void suspend_shouldReturn200_withSuspendedOrg() {
        OrganizationResponse resp = OrganizationResponse.builder().id("o1")
                .status(OrganizationStatus.SUSPENDED).build();
        when(adminService.suspend(eq("o1"), any(AdminSuspendRequest.class), eq(ADMIN_ID))).thenReturn(resp);

        AdminSuspendRequest req = new AdminSuspendRequest();
        req.setReason("Violation des conditions d'utilisation");
        var entity = controller.suspend("o1", req, mockAdminAuth());

        assert entity.getStatusCodeValue() == 200;
        assert entity.getBody().getStatus() == OrganizationStatus.SUSPENDED;
        verify(adminService).suspend(eq("o1"), any(), eq(ADMIN_ID));
    }

    @Test
    void suspend_shouldFail_whenOrgAlreadySuspended() {
        AdminSuspendRequest req = new AdminSuspendRequest();
        req.setReason("Raison");
        doThrow(new BusinessRuleException("Organisation déjà suspendue"))
                .when(adminService).suspend(eq("o1"), any(), eq(ADMIN_ID));

        try {
            controller.suspend("o1", req, mockAdminAuth());
            assert false : "Exception attendue";
        } catch (BusinessRuleException e) {
            assert e.getMessage().contains("suspendue");
        }
    }

    // ── POST /api/admin/organizations/{id}/reactivate ─────────────

    @Test
    void reactivate_shouldReturn200_withActiveOrg() {
        OrganizationResponse resp = OrganizationResponse.builder().id("o1")
                .status(OrganizationStatus.ACTIVE).build();
        when(adminService.reactivate("o1", ADMIN_ID)).thenReturn(resp);

        var entity = controller.reactivate("o1", mockAdminAuth());

        assert entity.getStatusCodeValue() == 200;
        assert entity.getBody().getStatus() == OrganizationStatus.ACTIVE;
        verify(adminService).reactivate("o1", ADMIN_ID);
    }

    @Test
    void reactivate_shouldFail_whenOrgNotSuspended() {
        doThrow(new BusinessRuleException("Seules les organisations suspendues peuvent être réactivées"))
                .when(adminService).reactivate(eq("o1"), eq(ADMIN_ID));

        try {
            controller.reactivate("o1", mockAdminAuth());
            assert false : "Exception attendue";
        } catch (BusinessRuleException e) {
            assert e.getMessage().contains("suspendues");
        }
    }

    // ── GET /api/admin/organizations/stats ────────────────────────

    @Test
    void stats_shouldReturn200_withDashboardStats() {
        OrganizationDashboardStats stats = OrganizationDashboardStats.builder()
                .totalOrganizations(10L).activeOrganizations(7L).build();
        when(adminService.getDashboardStats()).thenReturn(stats);

        var entity = controller.stats();

        assert entity.getStatusCodeValue() == 200;
        assert entity.getBody().getTotalOrganizations() == 10L;
        verify(adminService).getDashboardStats();
    }

    // ── GET /api/admin/organizations/pending ──────────────────────

    @Test
    void getPending_shouldReturn200_withPendingOrgs() throws Exception {
        OrganizationResponse org = OrganizationResponse.builder().id("o1")
                .status(OrganizationStatus.PENDING_VERIFICATION).build();
        when(adminService.listAll(eq(OrganizationStatus.PENDING_VERIFICATION), any()))
                .thenReturn(new PageImpl<>(List.of(org), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/organizations/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value("o1"));
    }

    @Test
    void getPending_shouldReturn200_withEmptyPage_whenNoPending() throws Exception {
        when(adminService.listAll(eq(OrganizationStatus.PENDING_VERIFICATION), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/admin/organizations/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ── Helpers ───────────────────────────────────────────────────

    private AdminVerifyRequest buildVerifyRequest(String decision, String note) {
        AdminVerifyRequest req = new AdminVerifyRequest();
        req.setDecision(decision);
        req.setAdminNote(note);
        return req;
    }
}
