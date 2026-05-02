package com.smartfreelance.microservice.complaintservice.controller;

import com.smartfreelance.microservice.complaintservice.dto.advanced.UserRiskProfileResponse;
import com.smartfreelance.microservice.complaintservice.dto.advanced.UserSanctionResponse;
import com.smartfreelance.microservice.complaintservice.entity.UserSanction;
import com.smartfreelance.microservice.complaintservice.service.RiskScoringService;
import com.smartfreelance.microservice.complaintservice.service.SanctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
public class RiskController {

    private final RiskScoringService riskService;
    private final SanctionService    sanctionService;

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserRiskProfileResponse> getProfile(@PathVariable String userId) {
        return ResponseEntity.ok(riskService.getProfile(userId));
    }

    @PostMapping("/users/{userId}/compute")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserRiskProfileResponse> compute(@PathVariable String userId) {
        return ResponseEntity.ok(riskService.computeAndSave(userId));
    }

    @GetMapping("/high-risk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserRiskProfileResponse>> getHighRisk() {
        return ResponseEntity.ok(riskService.getHighRiskUsers());
    }

    @GetMapping("/users/{userId}/sanctions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserSanctionResponse>> getSanctions(@PathVariable String userId) {
        return ResponseEntity.ok(sanctionService.getForUser(userId));
    }

    @PostMapping("/users/{userId}/sanctions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserSanctionResponse> applySanction(
            @PathVariable String userId,
            @RequestHeader("X-User-Id") String adminId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(sanctionService.applyManual(
                userId,
                body.get("reason"),
                UserSanction.SanctionType.valueOf(body.get("type")),
                adminId));
    }

    @DeleteMapping("/sanctions/{sanctionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserSanctionResponse> liftSanction(
            @PathVariable String sanctionId,
            @RequestHeader("X-User-Id") String adminId) {
        return ResponseEntity.ok(sanctionService.liftSanction(sanctionId, adminId));
    }
}
