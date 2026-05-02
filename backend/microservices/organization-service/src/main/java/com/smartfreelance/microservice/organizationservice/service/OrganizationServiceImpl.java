package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.CreateOrganizationRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.UpdateOrganizationRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.GeoLocation;
import com.smartfreelance.microservice.organizationservice.dto.response.OrganizationResponse;
import com.smartfreelance.microservice.organizationservice.dto.response.OrganizationSummaryResponse;
import com.smartfreelance.microservice.organizationservice.entity.AuditLog;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.entity.OrganizationMember;
import com.smartfreelance.microservice.organizationservice.enums.*;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.AuditLogRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationMemberRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository orgRepo;
    private final OrganizationMemberRepository memberRepo;
    private final AuditLogRepository auditRepo;
    private final GeocodingService geocodingService;

    @Override
    public OrganizationResponse create(CreateOrganizationRequest request, String ownerId) {
        String name = request.getName().trim();
        if (!isNameAvailable(name)) {
            throw new BusinessRuleException("An organization with this name already exists.");
        }
        Organization org = Organization.builder()
                .name(name)
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl())
                .website(request.getWebsite())
                .type(request.getType())
                .specialties(request.getSpecialties() != null ? request.getSpecialties() : List.of())
                .location(request.getLocation())
                .siret(request.getSiret())
                .size(request.getSize() != null ? request.getSize() : OrganizationSize.SMALL)
                .visibility(request.getVisibility() != null ? request.getVisibility() : OrganizationVisibility.PUBLIC)
                .ownerId(ownerId)
                .status(OrganizationStatus.PENDING_VERIFICATION)
                .build();
        org = orgRepo.save(org);

        // Geocode the location after initial save
        if (org.getLocation() != null && !org.getLocation().isBlank()) {
            geocodeAndPersist(org);
        }

        OrganizationMember owner = OrganizationMember.builder()
                .organizationId(org.getId())
                .userId(ownerId)
                .role(MemberRole.OWNER)
                .status(MemberStatus.ACTIVE)
                .build();
        memberRepo.save(owner);

        audit(org.getId(), ownerId, "ORGANIZATION_CREATED", "Created organization: " + org.getName());
        return toResponse(org);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isNameAvailable(String name) {
        return name != null && !name.isBlank() && !orgRepo.existsByNameIgnoreCase(name.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse getById(String id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public OrganizationResponse update(String id, UpdateOrganizationRequest request, String userId) {
        Organization org = findOrThrow(id);
        if (!org.getOwnerId().equals(userId)) {
            throw new BusinessRuleException("Only the owner can update the organization.");
        }
        if (request.getName() != null) org.setName(request.getName());
        if (request.getDescription() != null) org.setDescription(request.getDescription());
        if (request.getLogoUrl() != null) org.setLogoUrl(request.getLogoUrl());
        if (request.getWebsite() != null) org.setWebsite(request.getWebsite());
        if (request.getSpecialties() != null) org.setSpecialties(request.getSpecialties());
        boolean locationChanged = request.getLocation() != null
                && !request.getLocation().equals(org.getLocation());
        if (request.getLocation() != null) org.setLocation(request.getLocation());
        if (request.getSiret() != null) org.setSiret(request.getSiret());
        if (request.getSize() != null) org.setSize(request.getSize());
        if (request.getVisibility() != null) org.setVisibility(request.getVisibility());
        audit(id, userId, "ORGANIZATION_UPDATED", "Updated organization profile");
        org = orgRepo.save(org);

        // Re-geocode only when the location field actually changed
        if (locationChanged && org.getLocation() != null && !org.getLocation().isBlank()) {
            geocodeAndPersist(org);
        }

        return toResponse(org);
    }

    @Override
    public void delete(String id, String userId) {
        Organization org = findOrThrow(id);
        if (!org.getOwnerId().equals(userId)) {
            throw new BusinessRuleException("Only the owner can delete the organization.");
        }
        orgRepo.delete(org);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationSummaryResponse> getMyOrganizations(String userId) {
        List<OrganizationSummaryResponse> result = new ArrayList<>();

        // Organisations dont l'utilisateur est propriétaire → rôle OWNER
        orgRepo.findByOwnerId(userId).forEach(org ->
                result.add(toSummaryWithRole(org, MemberRole.OWNER,
                        memberRepo.countByOrganizationIdAndStatus(org.getId(), MemberStatus.ACTIVE)))
        );

        // Organisations où l'utilisateur est membre ACTIF (hors propriétaire)
        memberRepo.findByUserIdAndStatus(userId, MemberStatus.ACTIVE).stream()
                .filter(m -> !m.getRole().equals(MemberRole.OWNER)) // OWNER déjà traité ci-dessus
                .forEach(m -> orgRepo.findById(m.getOrganizationId()).ifPresent(org -> {
                    // Évite les doublons si l'org est aussi dans 'owned'
                    boolean alreadyAdded = result.stream().anyMatch(r -> r.getId().equals(org.getId()));
                    if (!alreadyAdded) {
                        result.add(toSummaryWithRole(org, m.getRole(),
                                memberRepo.countByOrganizationIdAndStatus(org.getId(), MemberStatus.ACTIVE)));
                    }
                }));

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrganizationSummaryResponse> searchPublic(String keyword, OrganizationType type, OrganizationSize size, Pageable pageable) {
        return orgRepo.searchPublic(keyword, type, size, pageable).map(this::toSummary);
    }

    @Override
    public void transferOwnership(String orgId, String currentOwnerId, String newOwnerId) {
        Organization org = findOrThrow(orgId);
        if (!org.getOwnerId().equals(currentOwnerId)) {
            throw new BusinessRuleException("Only the current owner can transfer ownership.");
        }
        OrganizationMember newOwnerMember = memberRepo.findByOrganizationIdAndUserId(orgId, newOwnerId)
                .orElseThrow(() -> new BusinessRuleException("The new owner must already be an active member of the organization."));

        // Demote current owner to MANAGER
        memberRepo.findByOrganizationIdAndUserId(orgId, org.getOwnerId()).ifPresent(m -> {
            m.setRole(MemberRole.MANAGER);
            memberRepo.save(m);
        });

        // Promote new owner
        newOwnerMember.setRole(MemberRole.OWNER);
        memberRepo.save(newOwnerMember);

        org.setOwnerId(newOwnerId);
        orgRepo.save(org);
        audit(orgId, currentOwnerId, "OWNERSHIP_TRANSFERRED", "Transferred ownership to: " + newOwnerId);
    }

    @Override
    public void dissolve(String orgId, String ownerId) {
        Organization org = findOrThrow(orgId);
        if (!org.getOwnerId().equals(ownerId)) {
            throw new BusinessRuleException("Only the owner can dissolve the organization.");
        }
        if (org.getStatus() == OrganizationStatus.DISSOLVED) {
            throw new BusinessRuleException("Organization is already dissolved.");
        }
        org.setStatus(OrganizationStatus.DISSOLVED);
        org.setDissolvedAt(LocalDateTime.now());
        orgRepo.save(org);
        audit(orgId, ownerId, "ORGANIZATION_DISSOLVED", "Organization dissolved");
    }

    @Override
    public OrganizationResponse setVisibility(String id, OrganizationVisibility visibility, String userId) {
        Organization org = findOrThrow(id);
        boolean isManagerOrOwner = org.getOwnerId().equals(userId) ||
                memberRepo.findByOrganizationIdAndUserId(id, userId)
                        .map(m -> m.getRole() == MemberRole.MANAGER || m.getRole() == MemberRole.OWNER)
                        .orElse(false);
        if (!isManagerOrOwner) {
            throw new BusinessRuleException("Only the owner or a manager can change visibility.");
        }
        org.setVisibility(visibility);
        audit(id, userId, "VISIBILITY_CHANGED", "Visibility set to: " + visibility);
        return toResponse(orgRepo.save(org));
    }

    /**
     * Calls Nominatim, and if a result is returned, persists lat/lon on the entity.
     */
    private void geocodeAndPersist(Organization org) {
        Optional<GeoLocation> geo = geocodingService.geocode(org.getLocation());
        geo.ifPresent(g -> {
            org.setLatitude(g.getLatitude());
            org.setLongitude(g.getLongitude());
            orgRepo.save(org);
            log.info("Persisted geocoordinates for org {} → {}, {}",
                    org.getId(), g.getLatitude(), g.getLongitude());
        });
    }

    private Organization findOrThrow(String id) {
        return orgRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));
    }

    private void audit(String orgId, String userId, String action, String details) {
        auditRepo.save(AuditLog.builder()
                .organizationId(orgId)
                .performedByUserId(userId)
                .action(action)
                .details(details)
                .build());
    }

    private OrganizationResponse toResponse(Organization org) {
        return OrganizationResponse.builder()
                .id(org.getId()).name(org.getName()).description(org.getDescription())
                .logoUrl(org.getLogoUrl()).website(org.getWebsite()).type(org.getType())
                .specialties(org.getSpecialties()).location(org.getLocation())
                .latitude(org.getLatitude()).longitude(org.getLongitude())
                .siret(org.getSiret())
                .size(org.getSize()).status(org.getStatus()).visibility(org.getVisibility())
                .ownerId(org.getOwnerId()).averageRating(org.getAverageRating())
                .completedProjectsCount(org.getCompletedProjectsCount()).reviewCount(org.getReviewCount())
                .trustLevel(org.getTrustLevel()).createdAt(org.getCreatedAt()).updatedAt(org.getUpdatedAt())
                .build();
    }

    private OrganizationSummaryResponse toSummary(Organization org) {
        long memberCount = memberRepo.countByOrganizationIdAndStatus(org.getId(), MemberStatus.ACTIVE);
        return OrganizationSummaryResponse.builder()
                .id(org.getId()).name(org.getName()).logoUrl(org.getLogoUrl())
                .type(org.getType()).status(org.getStatus()).location(org.getLocation())
                .averageRating(org.getAverageRating()).size(org.getSize())
                .reviewCount(org.getReviewCount())
                .memberCount((int) memberCount)
                .build();
    }

    private OrganizationSummaryResponse toSummaryWithRole(Organization org, MemberRole role, long memberCount) {
        return OrganizationSummaryResponse.builder()
                .id(org.getId()).name(org.getName()).logoUrl(org.getLogoUrl())
                .type(org.getType()).status(org.getStatus()).location(org.getLocation())
                .averageRating(org.getAverageRating()).size(org.getSize())
                .reviewCount(org.getReviewCount())
                .memberCount((int) memberCount)
                .myRole(role)
                .build();
    }
}
