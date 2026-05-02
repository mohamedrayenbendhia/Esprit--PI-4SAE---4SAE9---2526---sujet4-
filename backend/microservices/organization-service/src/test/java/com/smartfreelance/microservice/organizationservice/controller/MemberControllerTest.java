package com.smartfreelance.microservice.organizationservice.controller;

import com.smartfreelance.microservice.organizationservice.dto.response.MemberResponse;
import com.smartfreelance.microservice.organizationservice.enums.MemberRole;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.GlobalExceptionHandler;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    @Mock private MemberService memberService;
    @InjectMocks private MemberController controller;

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

    // ── GET /api/organizations/{orgId}/members ────────────────────

    @Test
    void getMembers_shouldReturn200_withPagedList() throws Exception {
        MemberResponse m1 = MemberResponse.builder().id("m1").build();
        MemberResponse m2 = MemberResponse.builder().id("m2").build();
        when(memberService.getMembers(eq("o1"), any())).thenReturn(new PageImpl<>(List.of(m1, m2), PageRequest.of(0, 20), 2));

        mockMvc.perform(get("/api/organizations/o1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value("m1"));
    }

    @Test
    void getMembers_shouldReturn200_withEmptyList() throws Exception {
        when(memberService.getMembers(eq("o1"), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/organizations/o1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getMembers_shouldReturn404_whenOrgNotFound() throws Exception {
        when(memberService.getMembers(eq("missing"), any()))
                .thenThrow(new ResourceNotFoundException("Organisation introuvable"));

        mockMvc.perform(get("/api/organizations/missing/members"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Organisation introuvable"));
    }

    // ── PATCH /api/organizations/{orgId}/members/{memberId}/role ──

    @Test
    void updateRole_shouldReturn200_withUpdatedMember() {
        MemberResponse updated = MemberResponse.builder().id("m1").build();
        when(memberService.updateRole("o1", "m1", MemberRole.MANAGER, USER_ID)).thenReturn(updated);

        var resp = controller.updateRole("o1", "m1", MemberRole.MANAGER, mockAuth());

        assert resp.getStatusCodeValue() == 200;
        assert "m1".equals(resp.getBody().getId());
        verify(memberService).updateRole("o1", "m1", MemberRole.MANAGER, USER_ID);
    }

    @Test
    void updateRole_shouldFail_whenRequesterIsNotOwner() {
        doThrow(new BusinessRuleException("Seul le propriétaire peut changer les rôles"))
                .when(memberService).updateRole(eq("o1"), eq("m1"), eq(MemberRole.MANAGER), eq(USER_ID));

        try {
            controller.updateRole("o1", "m1", MemberRole.MANAGER, mockAuth());
            assert false : "Exception attendue";
        } catch (BusinessRuleException e) {
            assert e.getMessage().contains("propriétaire");
        }
    }

    @Test
    void updateRole_shouldFail_whenMemberNotFound() {
        doThrow(new ResourceNotFoundException("Membre introuvable"))
                .when(memberService).updateRole(eq("o1"), eq("unknown"), any(), eq(USER_ID));

        try {
            controller.updateRole("o1", "unknown", MemberRole.MEMBER, mockAuth());
            assert false : "Exception attendue";
        } catch (ResourceNotFoundException e) {
            assert e.getMessage().contains("Membre");
        }
    }

    // ── DELETE /api/organizations/{orgId}/members/{memberId} ──────

    @Test
    void removeMember_shouldReturn204_onSuccess() {
        doNothing().when(memberService).removeMember("o1", "m1", USER_ID);

        var resp = controller.removeMember("o1", "m1", mockAuth());

        assert resp.getStatusCodeValue() == 204;
        verify(memberService).removeMember("o1", "m1", USER_ID);
    }

    @Test
    void removeMember_shouldFail_whenTryingToRemoveOwner() {
        doThrow(new BusinessRuleException("Impossible de retirer le propriétaire de l'organisation"))
                .when(memberService).removeMember(eq("o1"), eq("owner-id"), eq(USER_ID));

        try {
            controller.removeMember("o1", "owner-id", mockAuth());
            assert false : "Exception attendue";
        } catch (BusinessRuleException e) {
            assert e.getMessage().contains("propriétaire");
        }
    }

    @Test
    void removeMember_shouldFail_whenMemberNotFound() {
        doThrow(new ResourceNotFoundException("Membre introuvable"))
                .when(memberService).removeMember(eq("o1"), eq("ghost"), eq(USER_ID));

        try {
            controller.removeMember("o1", "ghost", mockAuth());
            assert false : "Exception attendue";
        } catch (ResourceNotFoundException e) {
            assert e.getMessage().contains("Membre");
        }
    }

    // ── POST /api/organizations/{orgId}/members/leave ────────────

    @Test
    void leave_shouldReturn204_onSuccess() {
        doNothing().when(memberService).leave("o1", USER_ID);

        var resp = controller.leave("o1", mockAuth());

        assert resp.getStatusCodeValue() == 204;
        verify(memberService).leave("o1", USER_ID);
    }

    @Test
    void leave_shouldFail_whenOwnerTriesToLeave() {
        doThrow(new BusinessRuleException("Le propriétaire ne peut pas quitter l'organisation sans transférer la propriété"))
                .when(memberService).leave(eq("o1"), eq(USER_ID));

        try {
            controller.leave("o1", mockAuth());
            assert false : "Exception attendue";
        } catch (BusinessRuleException e) {
            assert e.getMessage().contains("propriétaire");
        }
    }

    // ── GET /api/organizations/{orgId}/members/my ─────────────────

    @Test
    void myMemberships_shouldReturnList() {
        MemberResponse m = MemberResponse.builder().id("m1").build();
        when(memberService.getMyMemberships(USER_ID)).thenReturn(List.of(m));

        var resp = controller.myMemberships(mockAuth());

        assert resp.getBody() != null;
        assert resp.getBody().size() == 1;
        verify(memberService).getMyMemberships(USER_ID);
    }

    @Test
    void myMemberships_shouldReturnEmptyList_whenNoMemberships() {
        when(memberService.getMyMemberships(USER_ID)).thenReturn(List.of());

        var resp = controller.myMemberships(mockAuth());

        assert resp.getBody() != null;
        assert resp.getBody().isEmpty();
    }

    // ── PATCH /api/organizations/{orgId}/members/profile-display ──

    @Test
    void toggleProfileDisplay_shouldReturn200_withUpdatedMember() {
        MemberResponse updated = MemberResponse.builder().id("m1").build();
        when(memberService.toggleProfileDisplay("o1", USER_ID, true)).thenReturn(updated);

        var resp = controller.toggleProfileDisplay("o1", true, mockAuth());

        assert resp.getStatusCodeValue() == 200;
        assert "m1".equals(resp.getBody().getId());
        verify(memberService).toggleProfileDisplay("o1", USER_ID, true);
    }

    @Test
    void toggleProfileDisplay_hide_shouldWork() {
        MemberResponse updated = MemberResponse.builder().id("m1").build();
        when(memberService.toggleProfileDisplay("o1", USER_ID, false)).thenReturn(updated);

        var resp = controller.toggleProfileDisplay("o1", false, mockAuth());

        assert resp.getStatusCodeValue() == 200;
        verify(memberService).toggleProfileDisplay("o1", USER_ID, false);
    }

    @Test
    void toggleProfileDisplay_shouldFail_whenNotMember() {
        doThrow(new ResourceNotFoundException("Membre introuvable"))
                .when(memberService).toggleProfileDisplay(eq("o1"), eq(USER_ID), anyBoolean());

        try {
            controller.toggleProfileDisplay("o1", true, mockAuth());
            assert false : "Exception attendue";
        } catch (ResourceNotFoundException e) {
            assert e.getMessage().contains("Membre");
        }
    }
}
