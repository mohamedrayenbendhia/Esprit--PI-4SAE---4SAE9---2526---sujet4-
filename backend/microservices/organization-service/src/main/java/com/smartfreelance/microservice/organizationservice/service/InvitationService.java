package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.InviteMemberRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.InvitationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InvitationService {
    InvitationResponse invite(String orgId, InviteMemberRequest request, String inviterId);
    InvitationResponse respond(String invitationId, boolean accepted, String userId);
    InvitationResponse respond(String invitationId, boolean accepted, String userId, String userEmail);
    Page<InvitationResponse> getOrgInvitations(String orgId, Pageable pageable);
    void cancel(String invitationId, String requesterId);
    List<InvitationResponse> getMyPendingInvitations(String userId);
    List<InvitationResponse> getMyPendingInvitations(String userId, String userEmail);
    InvitationResponse respondByToken(String token, boolean accepted, String userId);
    InvitationResponse respondByToken(String token, boolean accepted, String userId, String userEmail);
}
