package com.smartfreelance.microservice.organizationservice.controller;

import com.smartfreelance.microservice.organizationservice.dto.response.ApplicationResponse;
import com.smartfreelance.microservice.organizationservice.dto.response.RfqResponse;
import com.smartfreelance.microservice.organizationservice.service.ApplicationService;
import com.smartfreelance.microservice.organizationservice.service.RfqService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class UserOrganizationController {

    private final ApplicationService applicationService;
    private final RfqService rfqService;

    @GetMapping("/applications/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications(Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.ok(applicationService.getMyApplications(userId));
    }

    @GetMapping("/rfq/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RfqResponse>> getMyRfqs(Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.ok(rfqService.getMyRfqs(userId));
    }
}
