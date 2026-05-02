package com.smartfreelance.microservice.organizationservice.controller;

import com.smartfreelance.microservice.organizationservice.dto.request.CreateRfqRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.RfqResponseRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.RfqResponse;
import com.smartfreelance.microservice.organizationservice.service.RfqService;
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
@RequestMapping("/api/organizations/{orgId}/rfq")
@RequiredArgsConstructor
public class RfqController {

    private final RfqService rfqService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RfqResponse> create(@PathVariable String orgId,
                                               @Valid @RequestBody CreateRfqRequest request,
                                               Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.status(HttpStatus.CREATED).body(rfqService.create(orgId, request, userId));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<RfqResponse>> list(@PathVariable String orgId, Pageable pageable) {
        return ResponseEntity.ok(rfqService.getByOrg(orgId, pageable));
    }

    @PostMapping("/{rfqId}/respond")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RfqResponse> respond(@PathVariable String orgId,
                                                @PathVariable String rfqId,
                                                @Valid @RequestBody RfqResponseRequest request,
                                                Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.ok(rfqService.respond(rfqId, request, userId));
    }

    @PostMapping("/{rfqId}/close")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> close(@PathVariable String orgId,
                                       @PathVariable String rfqId,
                                       Authentication auth) {
        String userId = (String) auth.getDetails();
        rfqService.close(rfqId, userId);
        return ResponseEntity.noContent().build();
    }
}
