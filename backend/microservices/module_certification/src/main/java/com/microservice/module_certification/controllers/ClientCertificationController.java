package com.microservice.module_certification.controllers;

import com.microservice.module_certification.dto.CertificationResponse;
import com.microservice.module_certification.services.UserTestResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/client/certifications")
@RequiredArgsConstructor
public class ClientCertificationController {

    private final UserTestResultService userTestResultService;

    // GET /api/client/certifications/{userId}
    // Voir les certifications d'un freelancer
    @GetMapping("/{userId}")
    public ResponseEntity<List<CertificationResponse>> getFreelancerCertifications(
            @PathVariable String userId) {
        return ResponseEntity.ok(
                userTestResultService.getCertificationsByUserId(userId));
    }
}