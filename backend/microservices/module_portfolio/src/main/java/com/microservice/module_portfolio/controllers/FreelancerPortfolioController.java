package com.microservice.module_portfolio.controllers;

import com.microservice.module_portfolio.dto.*;
import com.microservice.module_portfolio.services.PortfolioService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/freelancer/portfolios")
@RequiredArgsConstructor
public class FreelancerPortfolioController {

    private final PortfolioService portfolioService;

    @PostMapping
    public ResponseEntity<PortfolioResponse> create(
            @Valid @RequestBody PortfolioRequest request,
            Authentication authentication) {
        request.setUserId(extractUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(portfolioService.createPortfolio(request));
    }

    @GetMapping("/me")
    public ResponseEntity<PortfolioResponse> getMyPortfolio(
            Authentication authentication) {
        return ResponseEntity.ok(
                portfolioService.getByUserId(extractUserId(authentication)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PortfolioResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PortfolioUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(portfolioService.updatePortfolio(id, request));
    }

    @PostMapping("/{portfolioId}/projects")
    public ResponseEntity<ProjectResponse> addProject(
            @PathVariable Long portfolioId,
            @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(portfolioService.addProject(portfolioId, request));
    }

    @GetMapping("/{portfolioId}/projects")
    public ResponseEntity<List<ProjectResponse>> getProjects(
            @PathVariable Long portfolioId) {
        return ResponseEntity.ok(portfolioService.getProjects(portfolioId));
    }

    @PutMapping("/projects/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.ok(portfolioService.updateProject(projectId, request));
    }

    @DeleteMapping("/projects/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId) {
        portfolioService.deleteProject(projectId);
        return ResponseEntity.noContent().build();
    }

    private String extractUserId(Authentication authentication) {
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof Claims claims) {
                return claims.get("userId", String.class);
            }
        }
        throw new RuntimeException("Unable to extract userId from token");
    }

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<PortfolioResponse> toggleVisibility(@PathVariable Long id) {
        return ResponseEntity.ok(portfolioService.toggleVisibility(id));
    }
}