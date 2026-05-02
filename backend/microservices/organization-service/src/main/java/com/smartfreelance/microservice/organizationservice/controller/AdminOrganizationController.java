package com.smartfreelance.microservice.organizationservice.controller;

import com.smartfreelance.microservice.organizationservice.dto.request.AdminSuspendRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.AdminVerifyRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.AuditLogResponse;
import com.smartfreelance.microservice.organizationservice.dto.response.OrganizationDashboardStats;
import com.smartfreelance.microservice.organizationservice.dto.response.OrganizationResponse;
import com.smartfreelance.microservice.organizationservice.entity.AuditLog;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationStatus;
import com.smartfreelance.microservice.organizationservice.repository.AuditLogRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationRepository;
import com.smartfreelance.microservice.organizationservice.service.AdminOrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/organizations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrganizationController {

    private final AdminOrganizationService adminService;
    private final OrganizationRepository orgRepo;
    private final AuditLogRepository auditLogRepo;

    @GetMapping
    public ResponseEntity<Page<OrganizationResponse>> listAll(
            @RequestParam(required = false) OrganizationStatus status, Pageable pageable) {
        return ResponseEntity.ok(adminService.listAll(status, pageable));
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<OrganizationResponse> verify(@PathVariable String id,
                                                        @RequestBody AdminVerifyRequest request,
                                                        Authentication auth) {
        String adminId = (String) auth.getDetails();
        return ResponseEntity.ok(adminService.verify(id, request, adminId));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<OrganizationResponse> suspend(@PathVariable String id,
                                                         @Valid @RequestBody AdminSuspendRequest request,
                                                         Authentication auth) {
        String adminId = (String) auth.getDetails();
        return ResponseEntity.ok(adminService.suspend(id, request, adminId));
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<OrganizationResponse> reactivate(@PathVariable String id, Authentication auth) {
        String adminId = (String) auth.getDetails();
        return ResponseEntity.ok(adminService.reactivate(id, adminId));
    }

    @GetMapping("/stats")
    public ResponseEntity<OrganizationDashboardStats> stats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/pending")
    public ResponseEntity<Page<OrganizationResponse>> getPending(Pageable pageable) {
        return ResponseEntity.ok(adminService.listAll(OrganizationStatus.PENDING_VERIFICATION, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> hardDelete(@PathVariable String id) {
        orgRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/force-dissolve")
    public ResponseEntity<Void> forceDissolve(@PathVariable String id, Authentication auth) {
        String adminId = (String) auth.getDetails();
        Organization org = orgRepo.findById(id)
                .orElseThrow(() -> new com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException("Organization not found: " + id));
        org.setStatus(OrganizationStatus.DISSOLVED);
        org.setDissolvedAt(java.time.LocalDateTime.now());
        orgRepo.save(org);
        auditLogRepo.save(AuditLog.builder()
                .organizationId(id).performedByUserId(adminId)
                .action("FORCE_DISSOLVED").details("Force dissolved by admin").build());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/audit")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLog(@PathVariable String id, Pageable pageable) {
        Page<AuditLogResponse> page = auditLogRepo.findByOrganizationIdOrderByCreatedAtDesc(id, pageable)
                .map(log -> AuditLogResponse.builder()
                        .id(log.getId()).organizationId(log.getOrganizationId())
                        .performedByUserId(log.getPerformedByUserId()).action(log.getAction())
                        .details(log.getDetails()).createdAt(log.getCreatedAt()).build());
        return ResponseEntity.ok(page);
    }
}
