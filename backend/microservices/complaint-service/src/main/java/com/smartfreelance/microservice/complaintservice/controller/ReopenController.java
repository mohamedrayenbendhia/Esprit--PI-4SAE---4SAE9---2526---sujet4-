package com.smartfreelance.microservice.complaintservice.controller;

import com.smartfreelance.microservice.complaintservice.dto.ComplaintDTO;
import com.smartfreelance.microservice.complaintservice.dto.advanced.ReopenComplaintRequest;
import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.mapper.ComplaintMapper;
import com.smartfreelance.microservice.complaintservice.service.ReopenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ReopenController {

    private final ReopenService    reopenService;
    private final ComplaintMapper  complaintMapper;

    @PostMapping("/{complaintId}/reopen")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCE')")
    public ResponseEntity<ComplaintDTO> reopen(
            @PathVariable String complaintId,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody ReopenComplaintRequest req) {
        Complaint reopened = reopenService.reopen(complaintId, userId, req.getReason());
        return ResponseEntity.ok(complaintMapper.toDTO(reopened));
    }
}
