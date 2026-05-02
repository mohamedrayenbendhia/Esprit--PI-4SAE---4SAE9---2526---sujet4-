package com.smartfreelance.microservice.complaintservice.controller;

import com.smartfreelance.microservice.complaintservice.dto.advanced.CreateSlaRuleRequest;
import com.smartfreelance.microservice.complaintservice.dto.advanced.SlaRuleResponse;
import com.smartfreelance.microservice.complaintservice.dto.advanced.SlaTrackingResponse;
import com.smartfreelance.microservice.complaintservice.service.SlaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sla")
@RequiredArgsConstructor
public class SlaController {

    private final SlaService slaService;

    // ── Règles (admin only) ───────────────────────────────────────────────

    @GetMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SlaRuleResponse>> getAllRules() {
        return ResponseEntity.ok(slaService.getAllRules());
    }

    @PostMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SlaRuleResponse> createRule(@Valid @RequestBody CreateSlaRuleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(slaService.createRule(req));
    }

    @PutMapping("/rules/{ruleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SlaRuleResponse> updateRule(@PathVariable String ruleId,
                                                       @Valid @RequestBody CreateSlaRuleRequest req) {
        return ResponseEntity.ok(slaService.updateRule(ruleId, req));
    }

    @DeleteMapping("/rules/{ruleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRule(@PathVariable String ruleId) {
        slaService.deleteRule(ruleId);
        return ResponseEntity.noContent().build();
    }

    // ── Tracking (admin + agent) ──────────────────────────────────────────

    @GetMapping("/tracking/{complaintId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT_AGENT')")
    public ResponseEntity<SlaTrackingResponse> getTracking(@PathVariable String complaintId) {
        return ResponseEntity.ok(slaService.getTracking(complaintId));
    }
}
