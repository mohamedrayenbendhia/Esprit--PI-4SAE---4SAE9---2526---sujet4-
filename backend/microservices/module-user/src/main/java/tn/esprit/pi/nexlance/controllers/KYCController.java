package tn.esprit.pi.nexlance.controllers;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.pi.nexlance.dto.KYCVerificationDto;
import tn.esprit.pi.nexlance.dto.ReviewKYCRequest;
import tn.esprit.pi.nexlance.entities.KYCVerification;
import tn.esprit.pi.nexlance.mappers.KYCMapper;
import tn.esprit.pi.nexlance.services.FileUploadService;
import tn.esprit.pi.nexlance.services.KYCVerificationService;
import tn.esprit.pi.nexlance.services.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:4202"})
public class KYCController {

    private final KYCVerificationService kycVerificationService;
    private final FileUploadService fileUploadService;
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<List<KYCVerificationDto>> getMyDocuments(Authentication authentication) {
        try {
            UUID userId = getUserIdFromAuthentication(authentication);
            List<KYCVerification> documents = kycVerificationService.getVerificationsByUserId(userId);
            List<KYCVerificationDto> dtos = documents.stream()
                    .map(KYCMapper::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/me/status")
    public ResponseEntity<Map<String, Object>> getMyStatus(Authentication authentication) {
        try {
            UUID userId = getUserIdFromAuthentication(authentication);
            List<KYCVerification> documents = kycVerificationService.getVerificationsByUserId(userId);

            String status = "NOT_VERIFIED";
            boolean verified = false;

            if (!documents.isEmpty()) {
                boolean hasApproved = documents.stream()
                        .anyMatch(doc -> doc.getStatus() == KYCVerification.VerificationStatus.APPROVED);
                boolean hasPending = documents.stream()
                        .anyMatch(doc -> doc.getStatus() == KYCVerification.VerificationStatus.PENDING);

                if (hasApproved) {
                    status = "APPROVED";
                    verified = true;
                } else if (hasPending) {
                    status = "PENDING";
                }
            }

            List<KYCVerificationDto> dtos = documents.stream()
                    .map(KYCMapper::toDto)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("verified", verified);
            response.put("status", status);
            response.put("documents", dtos);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<KYCVerificationDto> submitDocument(
            @RequestParam("documentType") String documentType,
            @RequestParam("document") MultipartFile file,
            @RequestParam(value = "expiryDate", required = false) String expiryDate,
            Authentication authentication) {
        try {
            UUID userId = getUserIdFromAuthentication(authentication);

            String documentUrl = fileUploadService.uploadKYCDocument(file, userId, documentType);

            KYCVerification verification = new KYCVerification();
            verification.setUserId(userId);
            verification.setDocumentType(KYCVerification.DocumentType.valueOf(documentType.toUpperCase()));
            verification.setDocumentUrl(documentUrl);

            KYCVerification submitted = kycVerificationService.submitDocument(verification);
            KYCVerificationDto dto = KYCMapper.toDto(submitted);

            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable UUID documentId,
            Authentication authentication) {
        try {
            UUID userId = getUserIdFromAuthentication(authentication);
            KYCVerification document = kycVerificationService.getVerificationById(documentId);

            if (!document.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if (document.getDocumentUrl() != null) {
                fileUploadService.deleteFile(document.getDocumentUrl());
            }

            kycVerificationService.deleteVerification(documentId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/admin/pending")
    public ResponseEntity<List<KYCVerificationDto>> getPendingDocuments() {
        try {
            Page<KYCVerification> page = kycVerificationService
                    .getVerificationsByStatus(KYCVerification.VerificationStatus.PENDING, Pageable.unpaged());
            List<KYCVerificationDto> dtos = page.getContent().stream()
                    .map(KYCMapper::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<KYCVerificationDto>> getAllDocuments(
            @RequestParam(required = false) KYCVerification.VerificationStatus status) {
        try {
            List<KYCVerification> documents;
            if (status != null) {
                Page<KYCVerification> page = kycVerificationService.getVerificationsByStatus(status, Pageable.unpaged());
                documents = page.getContent();
            } else {
                Page<KYCVerification> page = kycVerificationService.getAllVerifications(Pageable.unpaged());
                documents = page.getContent();
            }
            List<KYCVerificationDto> dtos = documents.stream()
                    .map(KYCMapper::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/admin/user/{userId}")
    public ResponseEntity<List<KYCVerificationDto>> getUserDocuments(@PathVariable UUID userId) {
        try {
            List<KYCVerification> documents = kycVerificationService.getVerificationsByUserId(userId);
            List<KYCVerificationDto> dtos = documents.stream()
                    .map(KYCMapper::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/admin/{id}/review")
    public ResponseEntity<?> reviewDocument(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewKYCRequest request,
            Authentication authentication) {
        try {
            UUID reviewerId = getUserIdFromAuthentication(authentication);

            KYCVerification verified;
            if (request.getStatus() == KYCVerification.VerificationStatus.APPROVED) {
                verified = kycVerificationService.approveVerification(id, reviewerId);
                if (request.getExpiryDate() != null) {
                    verified.setExpiryDate(request.getExpiryDate());
                }
            } else if (request.getStatus() == KYCVerification.VerificationStatus.REJECTED) {
                verified = kycVerificationService.rejectVerification(id, reviewerId, request.getRejectionReason());
            } else {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid status. Must be APPROVED or REJECTED");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            KYCVerificationDto dto = KYCMapper.toDto(verified);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Une erreur est survenue");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/admin/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            long totalPending = kycVerificationService
                    .getVerificationsByStatus(KYCVerification.VerificationStatus.PENDING, Pageable.unpaged())
                    .getTotalElements();
            long totalApproved = kycVerificationService
                    .getVerificationsByStatus(KYCVerification.VerificationStatus.APPROVED, Pageable.unpaged())
                    .getTotalElements();
            long totalRejected = kycVerificationService
                    .getVerificationsByStatus(KYCVerification.VerificationStatus.REJECTED, Pageable.unpaged())
                    .getTotalElements();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalPending", totalPending);
            stats.put("totalApproved", totalApproved);
            stats.put("totalRejected", totalRejected);
            stats.put("averageReviewTime", 0);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private UUID getUserIdFromAuthentication(Authentication authentication) {
        if (authentication != null) {
            Object details = authentication.getDetails();
            if (details instanceof Claims claims) {
                String userId = claims.get("userId", String.class);
                if (userId != null) return UUID.fromString(userId);
            }
            try {
                return UUID.fromString(authentication.getName());
            } catch (IllegalArgumentException e) {
                return userService.getUserByEmail(authentication.getName()).getId();
            }
        }
        throw new RuntimeException("Unable to extract user ID from authentication");
    }
}