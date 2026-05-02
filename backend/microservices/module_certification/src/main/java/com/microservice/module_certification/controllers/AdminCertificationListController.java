package com.microservice.module_certification.controllers;

import com.microservice.module_certification.dto.CertificationResponse;
import com.microservice.module_certification.repositories.CertificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/certifications")
@RequiredArgsConstructor
public class AdminCertificationListController {

    private final CertificationRepository certificationRepository;

    @GetMapping
    public ResponseEntity<List<CertificationResponse>> getAll() {
        List<CertificationResponse> result = certificationRepository.findAll()
                .stream()
                .map(c -> CertificationResponse.builder()
                        .id(c.getId())
                        .userId(c.getUserId())
                        .userSkillId(c.getUserSkillId())
                        .testTitle(c.getTest().getTitle())
                        .score(c.getScore())
                        .date(c.getDate())
                        .certificateUrl(c.getCertificateUrl())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}