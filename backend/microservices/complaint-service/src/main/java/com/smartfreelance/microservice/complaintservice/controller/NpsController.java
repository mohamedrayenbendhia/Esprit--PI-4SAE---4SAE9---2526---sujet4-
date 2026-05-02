package com.smartfreelance.microservice.complaintservice.controller;

import com.smartfreelance.microservice.complaintservice.dto.advanced.NpsResponseRequest;
import com.smartfreelance.microservice.complaintservice.dto.advanced.NpsStatsResponse;
import com.smartfreelance.microservice.complaintservice.entity.NpsSurvey;
import com.smartfreelance.microservice.complaintservice.service.NpsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nps")
@RequiredArgsConstructor
public class NpsController {

    private final NpsService npsService;

    @PostMapping("/{complaintId}/respond")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCE')")
    public ResponseEntity<NpsSurvey> respond(
            @PathVariable String complaintId,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody NpsResponseRequest req) {
        return ResponseEntity.ok(npsService.respond(complaintId, req, userId));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NpsStatsResponse> getStats() {
        return ResponseEntity.ok(npsService.getStats());
    }

    @GetMapping("/{complaintId}")
    @PreAuthorize("hasAnyRole('ADMIN','CLIENT','FREELANCE','SUPPORT_AGENT')")
    public ResponseEntity<NpsSurvey> getSurvey(
            @PathVariable String complaintId,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(npsService.createSurvey(complaintId, userId));
    }
}
