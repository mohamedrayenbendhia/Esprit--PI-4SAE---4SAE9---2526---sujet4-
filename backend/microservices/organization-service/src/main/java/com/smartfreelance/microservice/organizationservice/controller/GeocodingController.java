package com.smartfreelance.microservice.organizationservice.controller;

import com.smartfreelance.microservice.organizationservice.dto.response.GeoLocation;
import com.smartfreelance.microservice.organizationservice.dto.response.GeoLocationResponse;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationRepository;
import com.smartfreelance.microservice.organizationservice.service.GeocodingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class GeocodingController {

    private final GeocodingService geocodingService;
    private final OrganizationRepository orgRepo;

    /**
     * Triggers on-demand geocoding for an organization, persists the result, and returns it.
     * Secured: authenticated users only.
     */
    @GetMapping("/{orgId}/geocode")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GeoLocationResponse> geocodeOrganization(@PathVariable String orgId) {
        Organization org = findOrThrow(orgId);

        Optional<GeoLocation> geo = Optional.empty();
        if (org.getLocation() != null && !org.getLocation().isBlank()) {
            geo = geocodingService.geocode(org.getLocation());
            geo.ifPresent(g -> {
                org.setLatitude(g.getLatitude());
                org.setLongitude(g.getLongitude());
                orgRepo.save(org);
            });
        }

        return ResponseEntity.ok(toGeoLocationResponse(org, geo.isPresent()));
    }

    /**
     * Returns the currently stored latitude/longitude for an organization.
     * Secured: authenticated users only.
     */
    @GetMapping("/{orgId}/location")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GeoLocationResponse> getLocation(@PathVariable String orgId) {
        Organization org = findOrThrow(orgId);
        boolean geocoded = org.getLatitude() != null && org.getLongitude() != null;
        return ResponseEntity.ok(toGeoLocationResponse(org, geocoded));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Organization findOrThrow(String orgId) {
        return orgRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + orgId));
    }

    private GeoLocationResponse toGeoLocationResponse(Organization org, boolean geocoded) {
        return GeoLocationResponse.builder()
                .organizationId(org.getId())
                .organizationName(org.getName())
                .address(org.getLocation())
                .latitude(org.getLatitude())
                .longitude(org.getLongitude())
                .geocoded(geocoded)
                .build();
    }
}
