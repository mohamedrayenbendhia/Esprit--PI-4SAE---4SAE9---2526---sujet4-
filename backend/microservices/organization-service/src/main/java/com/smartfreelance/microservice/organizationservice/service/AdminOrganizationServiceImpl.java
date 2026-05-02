package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.AdminSuspendRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.AdminVerifyRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.OrganizationDashboardStats;
import com.smartfreelance.microservice.organizationservice.dto.response.OrganizationResponse;
import com.smartfreelance.microservice.organizationservice.entity.AuditLog;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationStatus;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.AuditLogRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminOrganizationServiceImpl implements AdminOrganizationService {

    private final OrganizationRepository orgRepo;
    private final AuditLogRepository auditRepo;

    @Override
    @Transactional(readOnly = true)
    public Page<OrganizationResponse> listAll(OrganizationStatus status, Pageable pageable) {
        if (status != null) return orgRepo.findByStatus(status, pageable).map(this::toResponse);
        return orgRepo.findAll(pageable).map(this::toResponse);
    }

    @Override
    public OrganizationResponse verify(String orgId, AdminVerifyRequest request, String adminId) {
        Organization org = findOrThrow(orgId);

        // Résolution du nouveau statut selon la décision du frontend
        String decision = request.getDecision();
        if ("REJECT".equalsIgnoreCase(decision)) {
            org.setStatus(OrganizationStatus.REJECTED);
        } else if ("AWAITING_INFO".equalsIgnoreCase(decision)) {
            org.setStatus(OrganizationStatus.AWAITING_INFO);
        } else {
            // APPROVE (ou absence de décision → compatibilité ancienne API)
            org.setStatus(OrganizationStatus.ACTIVE);
        }

        String note = request.getEffectiveNote();
        if (note != null) org.setAdminNote(note);
        orgRepo.save(org);
        audit(orgId, adminId, "ORGANIZATION_VERIFIED", "Decision: " + (decision != null ? decision : "APPROVE"));
        return toResponse(org);
    }

    @Override
    public OrganizationResponse suspend(String orgId, AdminSuspendRequest request, String adminId) {
        Organization org = findOrThrow(orgId);
        org.setStatus(OrganizationStatus.SUSPENDED);
        org.setAdminNote(request.getReason());
        orgRepo.save(org);
        audit(orgId, adminId, "ORGANIZATION_SUSPENDED", "Reason: " + request.getReason());
        return toResponse(org);
    }

    @Override
    public OrganizationResponse reactivate(String orgId, String adminId) {
        Organization org = findOrThrow(orgId);
        org.setStatus(OrganizationStatus.ACTIVE);
        orgRepo.save(org);
        audit(orgId, adminId, "ORGANIZATION_REACTIVATED", "Organization reactivated by admin");
        return toResponse(org);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationDashboardStats getDashboardStats() {
        return OrganizationDashboardStats.builder()
                .totalOrganizations(orgRepo.count())
                .activeOrganizations(orgRepo.countByStatus(OrganizationStatus.ACTIVE))
                .pendingVerification(orgRepo.countByStatus(OrganizationStatus.PENDING_VERIFICATION))
                .suspended(orgRepo.countByStatus(OrganizationStatus.SUSPENDED))
                .dissolved(orgRepo.countByStatus(OrganizationStatus.DISSOLVED))
                .build();
    }

    private Organization findOrThrow(String id) {
        return orgRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));
    }

    private void audit(String orgId, String userId, String action, String details) {
        auditRepo.save(AuditLog.builder()
                .organizationId(orgId).performedByUserId(userId)
                .action(action).details(details).build());
    }

    private OrganizationResponse toResponse(Organization org) {
        return OrganizationResponse.builder()
                .id(org.getId()).name(org.getName()).description(org.getDescription())
                .logoUrl(org.getLogoUrl()).website(org.getWebsite()).type(org.getType())
                .specialties(org.getSpecialties()).location(org.getLocation()).siret(org.getSiret())
                .size(org.getSize()).status(org.getStatus()).visibility(org.getVisibility())
                .ownerId(org.getOwnerId()).averageRating(org.getAverageRating())
                .completedProjectsCount(org.getCompletedProjectsCount()).reviewCount(org.getReviewCount())
                .trustLevel(org.getTrustLevel()).adminNote(org.getAdminNote())
                .createdAt(org.getCreatedAt()).updatedAt(org.getUpdatedAt())
                .build();
    }
}
