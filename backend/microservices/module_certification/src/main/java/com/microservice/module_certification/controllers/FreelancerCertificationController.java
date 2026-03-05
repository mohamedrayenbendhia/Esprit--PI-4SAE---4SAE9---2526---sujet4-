package com.microservice.module_certification.controllers;

import com.microservice.module_certification.dto.*;
import com.microservice.module_certification.services.*;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/freelancer")
@RequiredArgsConstructor
public class FreelancerCertificationController {

    private final TestService testService;
    private final UserTestResultService userTestResultService;

    @GetMapping("/tests/skill/{skillId}")
    public ResponseEntity<TestPublicResponse> getTestBySkill(
            @PathVariable Long skillId) {
        return ResponseEntity.ok(testService.getBySkillIdPublic(skillId));
    }

    @GetMapping("/tests/results/me")
    public ResponseEntity<List<UserTestResultResponse>> getMyResults(
            Authentication authentication) {
        return ResponseEntity.ok(
                userTestResultService.getByUserId(extractUserId(authentication)));
    }

    @PostMapping("/tests/submit")
    public ResponseEntity<UserTestResultResponse> submitTest(
            @Valid @RequestBody SubmitTestRequest request,
            Authentication authentication) {
        request.setUserId(extractUserId(authentication)); // ✅ set par JWT
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userTestResultService.submitTest(request));
    }

    @GetMapping("/certifications/me")
    public ResponseEntity<List<CertificationResponse>> getMyCertifications(
            Authentication authentication) {
        return ResponseEntity.ok(
                userTestResultService.getCertificationsByUserId(extractUserId(authentication)));
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
}