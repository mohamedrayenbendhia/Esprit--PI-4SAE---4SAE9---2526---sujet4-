package com.smartfreelance.microservice.organizationservice.controller;

import com.smartfreelance.microservice.organizationservice.service.GdprExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/gdpr")
@RequiredArgsConstructor
public class GdprController {

    private final GdprExportService gdprService;

    @GetMapping("/export")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> export(Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.ok(gdprService.exportUserData(userId));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(Authentication auth) {
        String userId = (String) auth.getDetails();
        gdprService.deleteUserData(userId);
        return ResponseEntity.noContent().build();
    }
}
