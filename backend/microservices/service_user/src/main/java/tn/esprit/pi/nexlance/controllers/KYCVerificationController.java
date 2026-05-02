package tn.esprit.pi.nexlance.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi.nexlance.entities.KYCVerification;
import tn.esprit.pi.nexlance.services.KYCVerificationService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/kyc-verifications")
@RequiredArgsConstructor
public class KYCVerificationController {

    private final KYCVerificationService kycVerificationService;

    @PostMapping
    public ResponseEntity<KYCVerification> submitDocument(@RequestBody KYCVerification verification) {
        KYCVerification submittedVerification = kycVerificationService.submitDocument(verification);
        return ResponseEntity.status(HttpStatus.CREATED).body(submittedVerification);
    }

    @GetMapping("/{id}")
    public ResponseEntity<KYCVerification> getVerificationById(@PathVariable UUID id) {
        KYCVerification verification = kycVerificationService.getVerificationById(id);
        return ResponseEntity.ok(verification);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<KYCVerification>> getVerificationsByUserId(@PathVariable UUID userId) {
        List<KYCVerification> verifications = kycVerificationService.getVerificationsByUserId(userId);
        return ResponseEntity.ok(verifications);
    }

    @GetMapping("/user/{userId}/status/{status}")
    public ResponseEntity<List<KYCVerification>> getVerificationsByUserIdAndStatus(
            @PathVariable UUID userId, 
            @PathVariable KYCVerification.VerificationStatus status) {
        List<KYCVerification> verifications = kycVerificationService.getVerificationsByUserIdAndStatus(userId, status);
        return ResponseEntity.ok(verifications);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<KYCVerification>> getVerificationsByStatus(
            @PathVariable KYCVerification.VerificationStatus status, 
            Pageable pageable) {
        Page<KYCVerification> verifications = kycVerificationService.getVerificationsByStatus(status, pageable);
        return ResponseEntity.ok(verifications);
    }

    @GetMapping
    public ResponseEntity<Page<KYCVerification>> getAllVerifications(Pageable pageable) {
        Page<KYCVerification> verifications = kycVerificationService.getAllVerifications(pageable);
        return ResponseEntity.ok(verifications);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<KYCVerification> approveVerification(
            @PathVariable UUID id, 
            @RequestParam UUID reviewerId) {
        KYCVerification verification = kycVerificationService.approveVerification(id, reviewerId);
        return ResponseEntity.ok(verification);
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<KYCVerification> rejectVerification(
            @PathVariable UUID id, 
            @RequestParam UUID reviewerId,
            @RequestBody String reason) {
        KYCVerification verification = kycVerificationService.rejectVerification(id, reviewerId, reason);
        return ResponseEntity.ok(verification);
    }

    @DeleteMapping("/expired")
    public ResponseEntity<Void> deleteExpiredDocuments() {
        kycVerificationService.deleteExpiredDocuments();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVerification(@PathVariable UUID id) {
        kycVerificationService.deleteVerification(id);
        return ResponseEntity.noContent().build();
    }
}
