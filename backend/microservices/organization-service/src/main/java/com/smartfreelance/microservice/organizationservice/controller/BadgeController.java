package com.smartfreelance.microservice.organizationservice.controller;

import com.smartfreelance.microservice.organizationservice.dto.response.BadgeResponse;
import com.smartfreelance.microservice.organizationservice.enums.TrustBadge;
import com.smartfreelance.microservice.organizationservice.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations/{orgId}/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    @GetMapping
    public ResponseEntity<BadgeResponse> get(@PathVariable String orgId) {
        return ResponseEntity.ok(badgeService.getBadges(orgId));
    }

    @PostMapping("/{badge}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BadgeResponse> add(@PathVariable String orgId,
                                              @PathVariable TrustBadge badge,
                                              Authentication auth) {
        String adminId = (String) auth.getDetails();
        return ResponseEntity.ok(badgeService.addBadge(orgId, badge, adminId));
    }

    @DeleteMapping("/{badge}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BadgeResponse> remove(@PathVariable String orgId,
                                                 @PathVariable TrustBadge badge,
                                                 Authentication auth) {
        String adminId = (String) auth.getDetails();
        return ResponseEntity.ok(badgeService.removeBadge(orgId, badge, adminId));
    }
}
