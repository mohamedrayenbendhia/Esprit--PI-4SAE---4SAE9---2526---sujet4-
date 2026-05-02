package com.smartfreelance.microservice.organizationservice.controller;

import com.smartfreelance.microservice.organizationservice.dto.response.BadgeResponse;
import com.smartfreelance.microservice.organizationservice.enums.TrustBadge;
import com.smartfreelance.microservice.organizationservice.exception.GlobalExceptionHandler;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.service.BadgeService;
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

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BadgeController Unit Tests")
class BadgeControllerTest {

    @Mock private BadgeService badgeService;
    @InjectMocks private BadgeController controller;

    private MockMvc mockMvc;
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

    private BadgeResponse buildBadgeResponse(String orgId, List<TrustBadge> badges) {
        return BadgeResponse.builder()
                .organizationId(orgId)
                .badges(badges)
                .build();
    }

    // ── GET /api/organizations/{orgId}/badges ─────────────────────────────────

    @Test
    @DisplayName("get_existingOrg_returns200WithBadges")
    void get_existingOrg_returns200WithBadges() throws Exception {
        BadgeResponse response = buildBadgeResponse("org-1", List.of(TrustBadge.VERIFIED, TrustBadge.TOP_RATED));
        when(badgeService.getBadges("org-1")).thenReturn(response);

        mockMvc.perform(get("/api/organizations/org-1/badges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value("org-1"))
                .andExpect(jsonPath("$.badges[0]").value("VERIFIED"));
    }

    @Test
    @DisplayName("get_orgWithNoBadges_returns200WithEmptyList")
    void get_orgWithNoBadges_returns200WithEmptyList() throws Exception {
        BadgeResponse response = buildBadgeResponse("org-1", List.of());
        when(badgeService.getBadges("org-1")).thenReturn(response);

        mockMvc.perform(get("/api/organizations/org-1/badges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.badges").isEmpty());
    }

    @Test
    @DisplayName("get_orgNotFound_returns404")
    void get_orgNotFound_returns404() throws Exception {
        when(badgeService.getBadges("missing"))
                .thenThrow(new ResourceNotFoundException("Organization not found: missing"));

        mockMvc.perform(get("/api/organizations/missing/badges"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Organization not found: missing"));
    }

    // ── POST /api/organizations/{orgId}/badges/{badge} ────────────────────────

    @Test
    @DisplayName("add_validBadge_returns200")
    void add_validBadge_returns200() {
        BadgeResponse response = buildBadgeResponse("org-1", List.of(TrustBadge.VERIFIED));
        when(badgeService.addBadge("org-1", TrustBadge.VERIFIED, ADMIN_ID)).thenReturn(response);

        var result = controller.add("org-1", TrustBadge.VERIFIED, mockAdminAuth());

        assert result.getStatusCodeValue() == 200;
        assert result.getBody() != null;
        assert result.getBody().getBadges().contains(TrustBadge.VERIFIED);
        verify(badgeService).addBadge("org-1", TrustBadge.VERIFIED, ADMIN_ID);
    }

    @Test
    @DisplayName("add_orgNotFound_throwsResourceNotFoundException")
    void add_orgNotFound_throwsResourceNotFoundException() {
        when(badgeService.addBadge("missing", TrustBadge.VERIFIED, ADMIN_ID))
                .thenThrow(new ResourceNotFoundException("Organization not found: missing"));

        try {
            controller.add("missing", TrustBadge.VERIFIED, mockAdminAuth());
            assert false : "Exception expected";
        } catch (ResourceNotFoundException e) {
            assert e.getMessage().contains("Organization not found");
        }
    }

    @Test
    @DisplayName("add_multipleDistinctBadges_returnsAllBadges")
    void add_multipleDistinctBadges_returnsAllBadges() {
        BadgeResponse response = buildBadgeResponse("org-1", List.of(TrustBadge.VERIFIED, TrustBadge.TOP_RATED));
        when(badgeService.addBadge("org-1", TrustBadge.TOP_RATED, ADMIN_ID)).thenReturn(response);

        var result = controller.add("org-1", TrustBadge.TOP_RATED, mockAdminAuth());

        assert result.getStatusCodeValue() == 200;
        assert result.getBody().getBadges().size() == 2;
    }

    // ── DELETE /api/organizations/{orgId}/badges/{badge} ─────────────────────

    @Test
    @DisplayName("remove_existingBadge_returns200WithoutBadge")
    void remove_existingBadge_returns200WithoutBadge() {
        BadgeResponse response = buildBadgeResponse("org-1", List.of());
        when(badgeService.removeBadge("org-1", TrustBadge.VERIFIED, ADMIN_ID)).thenReturn(response);

        var result = controller.remove("org-1", TrustBadge.VERIFIED, mockAdminAuth());

        assert result.getStatusCodeValue() == 200;
        assert result.getBody().getBadges().isEmpty();
        verify(badgeService).removeBadge("org-1", TrustBadge.VERIFIED, ADMIN_ID);
    }

    @Test
    @DisplayName("remove_orgNotFound_throwsResourceNotFoundException")
    void remove_orgNotFound_throwsResourceNotFoundException() {
        when(badgeService.removeBadge("missing", TrustBadge.VERIFIED, ADMIN_ID))
                .thenThrow(new ResourceNotFoundException("Organization not found: missing"));

        try {
            controller.remove("missing", TrustBadge.VERIFIED, mockAdminAuth());
            assert false : "Exception expected";
        } catch (ResourceNotFoundException e) {
            assert e.getMessage().contains("Organization not found");
        }
    }

    @Test
    @DisplayName("remove_badgeNotPresent_returns200WithSameList")
    void remove_badgeNotPresent_returns200WithSameList() {
        // removeBadge is idempotent: removing a non-existent badge returns the same list
        BadgeResponse response = buildBadgeResponse("org-1", List.of(TrustBadge.TOP_RATED));
        when(badgeService.removeBadge("org-1", TrustBadge.VERIFIED, ADMIN_ID)).thenReturn(response);

        var result = controller.remove("org-1", TrustBadge.VERIFIED, mockAdminAuth());

        assert result.getStatusCodeValue() == 200;
        assert !result.getBody().getBadges().contains(TrustBadge.VERIFIED);
    }
}
