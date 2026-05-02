package com.smartfreelance.microservice.organizationservice.controller;

import com.smartfreelance.microservice.organizationservice.dto.request.CreatePortfolioItemRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.PortfolioItemResponse;
import com.smartfreelance.microservice.organizationservice.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations/{orgId}/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PortfolioItemResponse> add(@PathVariable String orgId,
                                                      @Valid @RequestBody CreatePortfolioItemRequest request,
                                                      Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.status(HttpStatus.CREATED).body(portfolioService.add(orgId, request, userId));
    }

    @GetMapping
    public ResponseEntity<List<PortfolioItemResponse>> list(@PathVariable String orgId) {
        return ResponseEntity.ok(portfolioService.getByOrg(orgId));
    }

    @PutMapping("/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PortfolioItemResponse> update(@PathVariable String orgId,
                                                         @PathVariable String itemId,
                                                         @Valid @RequestBody CreatePortfolioItemRequest request,
                                                         Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.ok(portfolioService.update(itemId, orgId, request, userId));
    }

    @DeleteMapping("/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable String orgId,
                                        @PathVariable String itemId,
                                        Authentication auth) {
        String userId = (String) auth.getDetails();
        portfolioService.delete(itemId, orgId, userId);
        return ResponseEntity.noContent().build();
    }
}
