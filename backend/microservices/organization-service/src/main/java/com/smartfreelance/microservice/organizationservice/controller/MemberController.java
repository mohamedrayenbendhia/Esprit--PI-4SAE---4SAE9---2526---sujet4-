package com.smartfreelance.microservice.organizationservice.controller;

import com.smartfreelance.microservice.organizationservice.dto.response.MemberResponse;
import com.smartfreelance.microservice.organizationservice.enums.MemberRole;
import com.smartfreelance.microservice.organizationservice.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations/{orgId}/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    public ResponseEntity<Page<MemberResponse>> getMembers(@PathVariable String orgId, Pageable pageable) {
        return ResponseEntity.ok(memberService.getMembers(orgId, pageable));
    }

    @PatchMapping("/{memberId}/role")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MemberResponse> updateRole(@PathVariable String orgId,
                                                      @PathVariable String memberId,
                                                      @RequestParam MemberRole role,
                                                      Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.ok(memberService.updateRole(orgId, memberId, role, userId));
    }

    @DeleteMapping("/{memberId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeMember(@PathVariable String orgId,
                                              @PathVariable String memberId,
                                              Authentication auth) {
        String userId = (String) auth.getDetails();
        memberService.removeMember(orgId, memberId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/leave")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> leave(@PathVariable String orgId, Authentication auth) {
        String userId = (String) auth.getDetails();
        memberService.leave(orgId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MemberResponse>> myMemberships(Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.ok(memberService.getMyMemberships(userId));
    }

    @PatchMapping("/profile-display")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MemberResponse> toggleProfileDisplay(@PathVariable String orgId,
                                                                @RequestParam boolean display,
                                                                Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.ok(memberService.toggleProfileDisplay(orgId, userId, display));
    }
}
