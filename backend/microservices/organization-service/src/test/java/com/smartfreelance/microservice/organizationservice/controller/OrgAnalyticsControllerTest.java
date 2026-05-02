package com.smartfreelance.microservice.organizationservice.controller;

import com.smartfreelance.microservice.organizationservice.dto.response.OrgAnalyticsResponse;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.GlobalExceptionHandler;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.service.OrgAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrgAnalyticsController Unit Tests")
class OrgAnalyticsControllerTest {

    @Mock private OrgAnalyticsService analyticsService;
    @InjectMocks private OrgAnalyticsController controller;

    private MockMvc mockMvc;
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

    private OrgAnalyticsResponse buildAnalyticsResponse(String orgId) {
        return OrgAnalyticsResponse.builder()
                .organizationId(orgId)
                .organizationName("Acme Corp")
                .averageRating(4.5)
                .trustLevel(3)
                .daysActive(30)
                .createdAt(LocalDateTime.now().minusDays(30))
                .members(OrgAnalyticsResponse.MemberStats.builder()
                        .total(5).active(4).inactive(1).owners(1).managers(1).members(3).build())
                .applications(OrgAnalyticsResponse.ApplicationStats.builder()
                        .total(10).pending(2).accepted(7).rejected(1).withdrawn(0).acceptanceRate(87.5).build())
                .reviews(OrgAnalyticsResponse.ReviewStats.builder()
                        .total(8).average(4.5).withReply(3).reported(0).build())
                .collab(OrgAnalyticsResponse.CollabStats.builder()
                        .totalOffers(3).openOffers(1).closedOffers(2).build())
                .rfq(OrgAnalyticsResponse.RfqStats.builder()
                        .total(5).pending(1).responded(3).closed(1).responseRate(80.0).build())
                .invitations(OrgAnalyticsResponse.InvitationStats.builder()
                        .total(6).pending(1).accepted(4).declined(1).acceptanceRate(80.0).build())
                .build();
    }

    // ── GET /api/organizations/{orgId}/analytics ──────────────────────────────

    @Test
    @DisplayName("getAnalytics_ownerRequest_returns200WithFullDashboard")
    void getAnalytics_ownerRequest_returns200WithFullDashboard() throws Exception {
        OrgAnalyticsResponse response = buildAnalyticsResponse("org-1");
        when(analyticsService.getAnalytics("org-1", USER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/organizations/org-1/analytics")
                        .principal(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value("org-1"))
                .andExpect(jsonPath("$.organizationName").value("Acme Corp"))
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.trustLevel").value(3))
                .andExpect(jsonPath("$.daysActive").value(30));
    }

    @Test
    @DisplayName("getAnalytics_userIdExtractedFromAuth_passedToService")
    void getAnalytics_userIdExtractedFromAuth_passedToService() throws Exception {
        OrgAnalyticsResponse response = buildAnalyticsResponse("org-1");
        when(analyticsService.getAnalytics("org-1", USER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/organizations/org-1/analytics")
                        .principal(mockAuth()))
                .andExpect(status().isOk());

        verify(analyticsService).getAnalytics("org-1", USER_ID);
    }

    @Test
    @DisplayName("getAnalytics_orgNotFound_returns404")
    void getAnalytics_orgNotFound_returns404() throws Exception {
        when(analyticsService.getAnalytics("missing", USER_ID))
                .thenThrow(new ResourceNotFoundException("Organisation introuvable : missing"));

        mockMvc.perform(get("/api/organizations/missing/analytics")
                        .principal(mockAuth()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Organisation introuvable : missing"));
    }

    @Test
    @DisplayName("getAnalytics_nonOwnerNonManager_returns422")
    void getAnalytics_nonOwnerNonManager_returns422() throws Exception {
        when(analyticsService.getAnalytics("org-1", USER_ID))
                .thenThrow(new BusinessRuleException("Accès refusé : seuls les propriétaires et gestionnaires peuvent consulter le dashboard."));

        mockMvc.perform(get("/api/organizations/org-1/analytics")
                        .principal(mockAuth()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        "Accès refusé : seuls les propriétaires et gestionnaires peuvent consulter le dashboard."));
    }

    @Test
    @DisplayName("getAnalytics_containsMembersSection")
    void getAnalytics_containsMembersSection() throws Exception {
        OrgAnalyticsResponse response = buildAnalyticsResponse("org-1");
        when(analyticsService.getAnalytics("org-1", USER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/organizations/org-1/analytics")
                        .principal(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.total").value(5))
                .andExpect(jsonPath("$.members.active").value(4));
    }

    @Test
    @DisplayName("getAnalytics_containsApplicationsSection")
    void getAnalytics_containsApplicationsSection() throws Exception {
        OrgAnalyticsResponse response = buildAnalyticsResponse("org-1");
        when(analyticsService.getAnalytics("org-1", USER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/organizations/org-1/analytics")
                        .principal(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applications.total").value(10))
                .andExpect(jsonPath("$.applications.acceptanceRate").value(87.5));
    }

    @Test
    @DisplayName("getAnalytics_containsRfqSection")
    void getAnalytics_containsRfqSection() throws Exception {
        OrgAnalyticsResponse response = buildAnalyticsResponse("org-1");
        when(analyticsService.getAnalytics("org-1", USER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/organizations/org-1/analytics")
                        .principal(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rfq.total").value(5))
                .andExpect(jsonPath("$.rfq.responseRate").value(80.0));
    }
}
