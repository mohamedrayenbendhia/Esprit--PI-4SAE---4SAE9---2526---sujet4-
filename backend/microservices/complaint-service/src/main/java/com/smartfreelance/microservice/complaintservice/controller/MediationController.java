package com.smartfreelance.microservice.complaintservice.controller;

import com.smartfreelance.microservice.complaintservice.dto.advanced.MediationDecisionRequest;
import com.smartfreelance.microservice.complaintservice.dto.advanced.MediationSessionResponse;
import com.smartfreelance.microservice.complaintservice.dto.advanced.OpenMediationRequest;
import com.smartfreelance.microservice.complaintservice.dto.advanced.SubmitEvidenceRequest;
import com.smartfreelance.microservice.complaintservice.entity.MediationSession;
import com.smartfreelance.microservice.complaintservice.service.MediationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mediation")
@RequiredArgsConstructor
public class MediationController {

    private final MediationService mediationService;

    @PostMapping("/{complaintId}/open")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MediationSessionResponse> openSession(
            @PathVariable String complaintId,
            @RequestHeader("X-User-Id") String adminId,
            @Valid @RequestBody OpenMediationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mediationService.openSession(complaintId, adminId, req));
    }

    @GetMapping("/{complaintId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT_AGENT','CLIENT','FREELANCE')")
    public ResponseEntity<MediationSessionResponse> getSession(@PathVariable String complaintId) {
        return ResponseEntity.ok(mediationService.getSession(complaintId));
    }

    @PostMapping("/sessions/{sessionId}/evidence")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCE')")
    public ResponseEntity<MediationSessionResponse> submitEvidence(
            @PathVariable String sessionId,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody SubmitEvidenceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mediationService.submitEvidence(sessionId, userId, req));
    }

    @PostMapping("/sessions/{sessionId}/decide")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MediationSessionResponse> decide(
            @PathVariable String sessionId,
            @RequestHeader("X-User-Id") String adminId,
            @Valid @RequestBody MediationDecisionRequest req) {
        return ResponseEntity.ok(mediationService.decide(sessionId, adminId, req));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MediationSessionResponse>> getByStatus(
            @PathVariable MediationSession.MediationStatus status) {
        return ResponseEntity.ok(mediationService.getSessionsByStatus(status));
    }
}
