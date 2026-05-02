package com.smartfreelance.microservice.organizationservice.controller;

import com.smartfreelance.microservice.organizationservice.dto.request.CreateApplicationRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.RespondApplicationRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.ApplicationResponse;
import com.smartfreelance.microservice.organizationservice.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations/{orgId}/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @PreAuthorize("hasRole('FREELANCE')")
    public ResponseEntity<ApplicationResponse> apply(@PathVariable String orgId,
                                                      @Valid @RequestBody CreateApplicationRequest request,
                                                      Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.apply(orgId, request, userId));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ApplicationResponse>> list(@PathVariable String orgId, Pageable pageable) {
        return ResponseEntity.ok(applicationService.getOrgApplications(orgId, pageable));
    }

    @PostMapping("/{applicationId}/respond")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApplicationResponse> respond(@PathVariable String orgId,
                                                        @PathVariable String applicationId,
                                                        @Valid @RequestBody RespondApplicationRequest request,
                                                        Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.ok(applicationService.respond(applicationId, request, userId));
    }

    @DeleteMapping("/{applicationId}/withdraw")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> withdraw(@PathVariable String orgId,
                                          @PathVariable String applicationId,
                                          Authentication auth) {
        String userId = (String) auth.getDetails();
        applicationService.withdraw(applicationId, userId);
        return ResponseEntity.noContent().build();
    }
}
