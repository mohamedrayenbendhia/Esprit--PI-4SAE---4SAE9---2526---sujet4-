package com.smartfreelance.microservice.organizationservice.controller;

import com.smartfreelance.microservice.organizationservice.dto.request.InviteMemberRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.InvitationResponse;
import com.smartfreelance.microservice.organizationservice.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations/{orgId}/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvitationResponse> invite(@PathVariable String orgId,
                                                      @Valid @RequestBody InviteMemberRequest request,
                                                      Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.status(HttpStatus.CREATED).body(invitationService.invite(orgId, request, userId));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<InvitationResponse>> list(@PathVariable String orgId, Pageable pageable) {
        return ResponseEntity.ok(invitationService.getOrgInvitations(orgId, pageable));
    }

    @PostMapping("/{invitationId}/respond")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvitationResponse> respond(@PathVariable String orgId,
                                                       @PathVariable String invitationId,
                                                      @RequestParam boolean accepted,
                                                      Authentication auth) {
        String userId = (String) auth.getDetails();
        String userEmail = auth.getName();
        return ResponseEntity.ok(invitationService.respond(invitationId, accepted, userId, userEmail));
    }

    @DeleteMapping("/{invitationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancel(@PathVariable String orgId,
                                        @PathVariable String invitationId,
                                        Authentication auth) {
        String userId = (String) auth.getDetails();
        invitationService.cancel(invitationId, userId);
        return ResponseEntity.noContent().build();
    }
}
