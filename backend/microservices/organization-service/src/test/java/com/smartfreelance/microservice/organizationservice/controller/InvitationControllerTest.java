package com.smartfreelance.microservice.organizationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfreelance.microservice.organizationservice.dto.request.InviteMemberRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.InvitationResponse;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.GlobalExceptionHandler;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.service.InvitationService;
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
class InvitationControllerTest {

    @Mock private InvitationService invitationService;
    @InjectMocks private InvitationController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String USER_ID = "owner-1";

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

    // ── POST /api/organizations/{orgId}/invitations ───────────────

    @Test
    void invite_shouldReturn201_withInvitationBody() {
        InviteMemberRequest req = new InviteMemberRequest();
        req.setInviteeEmail("newuser@example.com");

        InvitationResponse resp = InvitationResponse.builder().id("inv-1").build();
        when(invitationService.invite(eq("o1"), any(InviteMemberRequest.class), eq(USER_ID))).thenReturn(resp);

        var entity = controller.invite("o1", req, mockAuth());

        assert entity.getStatusCodeValue() == 201;
        assert "inv-1".equals(entity.getBody().getId());
        verify(invitationService).invite(eq("o1"), any(), eq(USER_ID));
    }

    @Test
    void invite_shouldFail_whenDuplicatePendingInvitation() {
        InviteMemberRequest req = new InviteMemberRequest();
        req.setInviteeEmail("already@example.com");

        doThrow(new BusinessRuleException("Une invitation est déjà en attente pour cet utilisateur"))
                .when(invitationService).invite(eq("o1"), any(), eq(USER_ID));

        try {
            controller.invite("o1", req, mockAuth());
            assert false : "Exception attendue";
        } catch (BusinessRuleException e) {
            assert e.getMessage().contains("invitation");
        }
    }

    @Test
    void invite_shouldFail_whenOrgNotFound() {
        InviteMemberRequest req = new InviteMemberRequest();
        req.setInviteeEmail("user@example.com");

        doThrow(new ResourceNotFoundException("Organisation introuvable"))
                .when(invitationService).invite(eq("missing"), any(), eq(USER_ID));

        try {
            controller.invite("missing", req, mockAuth());
            assert false : "Exception attendue";
        } catch (ResourceNotFoundException e) {
            assert e.getMessage().contains("Organisation");
        }
    }

    @Test
    void invite_shouldFail_whenUserIsAlreadyMember() {
        InviteMemberRequest req = new InviteMemberRequest();
        req.setInviteeEmail("member@example.com");

        doThrow(new BusinessRuleException("Cet utilisateur est déjà membre de l'organisation"))
                .when(invitationService).invite(eq("o1"), any(), eq(USER_ID));

        try {
            controller.invite("o1", req, mockAuth());
            assert false : "Exception attendue";
        } catch (BusinessRuleException e) {
            assert e.getMessage().contains("membre");
        }
    }

    // ── GET /api/organizations/{orgId}/invitations ────────────────

    @Test
    void list_shouldReturn200_withPagedInvitations() throws Exception {
        InvitationResponse inv = InvitationResponse.builder().id("inv-1").build();
        when(invitationService.getOrgInvitations(eq("o1"), any()))
                .thenReturn(new PageImpl<>(List.of(inv), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/organizations/o1/invitations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value("inv-1"));
    }

    @Test
    void list_shouldReturn200_withEmptyPage() throws Exception {
        when(invitationService.getOrgInvitations(eq("o1"), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/organizations/o1/invitations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ── POST /api/organizations/{orgId}/invitations/{id}/respond ──

    @Test
    void respond_accept_shouldReturn200() throws Exception {
        InvitationResponse resp = InvitationResponse.builder().id("inv-1").build();
        when(invitationService.respond("inv-1", true, USER_ID)).thenReturn(resp);

        mockMvc.perform(post("/api/organizations/o1/invitations/inv-1/respond")
                        .param("accepted", "true")
                        .header("X-User-Id", USER_ID)
                        .principal(mockAuth()))
                .andExpect(status().isOk());

        verify(invitationService).respond("inv-1", true, USER_ID);
    }

    @Test
    void respond_decline_shouldReturn200() throws Exception {
        InvitationResponse resp = InvitationResponse.builder().id("inv-1").build();
        when(invitationService.respond("inv-1", false, USER_ID)).thenReturn(resp);

        mockMvc.perform(post("/api/organizations/o1/invitations/inv-1/respond")
                        .param("accepted", "false")
                        .header("X-User-Id", USER_ID)
                        .principal(mockAuth()))
                .andExpect(status().isOk());

        verify(invitationService).respond("inv-1", false, USER_ID);
    }

    @Test
    void respond_shouldFail_whenInvitationNotFound() {
        when(invitationService.respond(eq("ghost"), anyBoolean(), eq(USER_ID)))
                .thenThrow(new ResourceNotFoundException("Invitation introuvable"));

        try {
            controller.respond("o1", "ghost", true, mockAuth());
            assert false : "Exception attendue";
        } catch (ResourceNotFoundException e) {
            assert e.getMessage().contains("Invitation");
        }
    }

    @Test
    void respond_shouldFail_whenInvitationAlreadyAnswered() {
        when(invitationService.respond(eq("inv-1"), anyBoolean(), eq(USER_ID)))
                .thenThrow(new BusinessRuleException("Cette invitation a déjà reçu une réponse"));

        try {
            controller.respond("o1", "inv-1", true, mockAuth());
            assert false : "Exception attendue";
        } catch (BusinessRuleException e) {
            assert e.getMessage().contains("réponse");
        }
    }

    @Test
    void respond_shouldFail_whenWrongUser() {
        when(invitationService.respond(eq("inv-1"), anyBoolean(), eq(USER_ID)))
                .thenThrow(new BusinessRuleException("Cette invitation ne vous est pas destinée"));

        try {
            controller.respond("o1", "inv-1", true, mockAuth());
            assert false : "Exception attendue";
        } catch (BusinessRuleException e) {
            assert e.getMessage().contains("destinée");
        }
    }

    // ── DELETE /api/organizations/{orgId}/invitations/{id} ────────

    @Test
    void cancel_shouldReturn204_onSuccess() {
        doNothing().when(invitationService).cancel("inv-1", USER_ID);

        var resp = controller.cancel("o1", "inv-1", mockAuth());

        assert resp.getStatusCodeValue() == 204;
        verify(invitationService).cancel("inv-1", USER_ID);
    }

    @Test
    void cancel_shouldFail_whenInvitationNotFound() {
        doThrow(new ResourceNotFoundException("Invitation introuvable"))
                .when(invitationService).cancel(eq("ghost"), eq(USER_ID));

        try {
            controller.cancel("o1", "ghost", mockAuth());
            assert false : "Exception attendue";
        } catch (ResourceNotFoundException e) {
            assert e.getMessage().contains("Invitation");
        }
    }

    @Test
    void cancel_shouldFail_whenNotOwner() {
        doThrow(new BusinessRuleException("Seul le propriétaire peut annuler une invitation"))
                .when(invitationService).cancel(eq("inv-1"), eq(USER_ID));

        try {
            controller.cancel("o1", "inv-1", mockAuth());
            assert false : "Exception attendue";
        } catch (BusinessRuleException e) {
            assert e.getMessage().contains("propriétaire");
        }
    }
}
