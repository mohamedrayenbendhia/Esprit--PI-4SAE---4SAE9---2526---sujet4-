package com.smartfreelance.microservice.organizationservice.controller;

import com.smartfreelance.microservice.organizationservice.dto.response.InvitationResponse;
import com.smartfreelance.microservice.organizationservice.service.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/org-invitations")
@RequiredArgsConstructor
public class StandaloneInvitationController {

    private final InvitationService invitationService;

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InvitationResponse>> getMyPending(Authentication auth) {
        String userId = (String) auth.getDetails();
        String userEmail = auth.getName();
        return ResponseEntity.ok(invitationService.getMyPendingInvitations(userId, userEmail));
    }

    @PostMapping("/token/{token}/respond")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvitationResponse> respondByToken(@PathVariable String token,
                                                              @RequestParam boolean accepted,
                                                              Authentication auth) {
        String userId = (String) auth.getDetails();
        String userEmail = auth.getName();
        return ResponseEntity.ok(invitationService.respondByToken(token, accepted, userId, userEmail));
    }
}
