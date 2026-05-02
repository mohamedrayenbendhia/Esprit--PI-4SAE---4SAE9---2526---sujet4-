package com.smartfreelance.microservice.organizationservice.controller;

import com.smartfreelance.microservice.organizationservice.dto.request.CreateOrganizationRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.TransferOwnershipRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.UpdateOrganizationRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.OrganizationResponse;
import com.smartfreelance.microservice.organizationservice.dto.response.OrganizationSummaryResponse;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationSize;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationType;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationVisibility;
import com.smartfreelance.microservice.organizationservice.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService orgService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")   // Tout utilisateur authentifié peut créer une organisation
    public ResponseEntity<OrganizationResponse> create(@Valid @RequestBody CreateOrganizationRequest request,
                                                        Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.status(HttpStatus.CREATED).body(orgService.create(request, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(orgService.getById(id));
    }

    @GetMapping("/name-available")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Boolean>> isNameAvailable(@RequestParam String name) {
        return ResponseEntity.ok(Map.of("available", orgService.isNameAvailable(name)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrganizationResponse> update(@PathVariable String id,
                                                        @Valid @RequestBody UpdateOrganizationRequest request,
                                                        Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.ok(orgService.update(id, request, userId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication auth) {
        String userId = (String) auth.getDetails();
        orgService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrganizationSummaryResponse>> getMyOrgs(Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.ok(orgService.getMyOrganizations(userId));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<OrganizationSummaryResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) OrganizationType type,
            @RequestParam(required = false) OrganizationSize orgSize,
            Pageable pageable) {
        return ResponseEntity.ok(orgService.searchPublic(keyword, type, orgSize, pageable));
    }

    @PostMapping("/{id}/transfer-ownership")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> transferOwnership(@PathVariable String id,
                                                   @Valid @RequestBody TransferOwnershipRequest request,
                                                   Authentication auth) {
        String userId = (String) auth.getDetails();
        orgService.transferOwnership(id, userId, request.getNewOwnerId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/dissolve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> dissolve(@PathVariable String id, Authentication auth) {
        String userId = (String) auth.getDetails();
        orgService.dissolve(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/visibility")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrganizationResponse> setVisibility(@PathVariable String id,
                                                               @RequestParam OrganizationVisibility visibility,
                                                               Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.ok(orgService.setVisibility(id, visibility, userId));
    }
}
