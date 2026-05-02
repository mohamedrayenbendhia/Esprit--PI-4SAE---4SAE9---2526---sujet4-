package com.smartfreelance.microservice.complaintservice.controller;

import com.smartfreelance.microservice.complaintservice.dto.advanced.CreateTemplateRequest;
import com.smartfreelance.microservice.complaintservice.dto.advanced.ResponseTemplateResponse;
import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.service.ResponseTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/response-templates")
@RequiredArgsConstructor
public class ResponseTemplateController {

    private final ResponseTemplateService templateService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT_AGENT')")
    public ResponseEntity<List<ResponseTemplateResponse>> getAll() {
        return ResponseEntity.ok(templateService.getAll());
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT_AGENT')")
    public ResponseEntity<List<ResponseTemplateResponse>> getByCategory(
            @PathVariable Complaint.ComplaintCategory category) {
        return ResponseEntity.ok(templateService.getByCategory(category));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseTemplateResponse> create(
            @Valid @RequestBody CreateTemplateRequest req,
            @RequestHeader("X-User-Id") String adminId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.create(req, adminId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseTemplateResponse> update(
            @PathVariable String id,
            @Valid @RequestBody CreateTemplateRequest req) {
        return ResponseEntity.ok(templateService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        templateService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/use")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT_AGENT')")
    public ResponseEntity<ResponseTemplateResponse> recordUsage(@PathVariable String id) {
        return ResponseEntity.ok(templateService.recordUsage(id));
    }
}
