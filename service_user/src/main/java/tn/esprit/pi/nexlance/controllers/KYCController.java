package tn.esprit.pi.nexlance.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.pi.nexlance.dto.KYCVerificationDto;
import tn.esprit.pi.nexlance.dto.ReviewKYCRequest;
import tn.esprit.pi.nexlance.entities.KYCVerification;
import tn.esprit.pi.nexlance.mappers.KYCMapper;
import tn.esprit.pi.nexlance.services.FileUploadService;
import tn.esprit.pi.nexlance.services.KYCVerificationService;
import tn.esprit.pi.nexlance.services.UserService;

import java.nio.file.Path;
import java.nio.file.Paths;
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

    /**
     * Get all KYC documents for the authenticated user
     */
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

    /**
     * Get KYC status for the authenticated user
     */
    @GetMapping("/me/status")
    public ResponseEntity<Map<String, Object>> getMyStatus(Authentication authentication) {
        try {
            UUID userId = getUserIdFromAuthentication(authentication);
            List<KYCVerification> documents = kycVerificationService.getVerificationsByUserId(userId);
            
            // Determine overall status
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

    /**
     * Submit a new KYC document
     */
    @PostMapping("/submit")
    public ResponseEntity<KYCVerificationDto> submitDocument(
            @RequestParam("documentType") String documentType,
            @RequestParam("document") MultipartFile file,
            @RequestParam(value = "expiryDate", required = false) String expiryDate,
            Authentication authentication) {
        try {
            UUID userId = getUserIdFromAuthentication(authentication);
            
            // Upload file
            String documentUrl = fileUploadService.uploadKYCDocument(file, userId, documentType);
            
            // Create verification record
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

    /**
     * Delete a KYC document
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable UUID documentId,
            Authentication authentication) {
        try {
            UUID userId = getUserIdFromAuthentication(authentication);
            KYCVerification document = kycVerificationService.getVerificationById(documentId);
            
            // Verify the document belongs to the user
            if (!document.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Delete file if exists
            if (document.getDocumentUrl() != null) {
                fileUploadService.deleteFile(document.getDocumentUrl());
            }
            
            kycVerificationService.deleteVerification(documentId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Get pending KYC documents (Admin only)
     */
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

    /**
     * Get all KYC documents with optional status filter (Admin only)
     */
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

    /**
     * Get KYC documents for a specific user (Admin only)
     */
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

    /**
     * Review a KYC document (Admin only)
     */
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
            } else if (request.getStatus() == KYCVerification.VerificationStatus.EXPIRED) {
                // Deactivate/expire the document
                verified = kycVerificationService.deactivateVerification(id, reviewerId, request.getRejectionReason());
            } else {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid status. Must be APPROVED, REJECTED, or EXPIRED");
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
            errorResponse.put("error", "An error occurred while processing the request");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get admin statistics
     */
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
            stats.put("averageReviewTime", 0); // TODO: Implement calculation
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Serve KYC document file
     */
    @GetMapping("/{id}/document")
    public ResponseEntity<Resource> getDocument(@PathVariable UUID id) {
        try {
            KYCVerification verification = kycVerificationService.getVerificationById(id);
            String documentUrl = verification.getDocumentUrl();
            
            if (documentUrl == null || documentUrl.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // Remove leading slash and resolve relative to app working directory
            String cleanPath = documentUrl.startsWith("/") ? documentUrl.substring(1) : documentUrl;
            Path filePath = Paths.get(cleanPath);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                String contentType = "application/octet-stream";
                if (documentUrl.endsWith(".pdf")) {
                    contentType = "application/pdf";
                } else if (documentUrl.endsWith(".jpg") || documentUrl.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (documentUrl.endsWith(".png")) {
                    contentType = "image/png";
                }
                
                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
            } else {
                // File doesn't exist on disk - return the URL as a redirect hint
                Map<String, String> info = new HashMap<>();
                info.put("documentUrl", documentUrl);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Helper method to extract user ID from JWT token
     */
    private UUID getUserIdFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String sub = jwt.getSubject();
            // Try to parse as UUID, if fails, lookup by email
            try {
                return UUID.fromString(sub);
            } catch (IllegalArgumentException e) {
                // If sub is email, look up user
                return userService.getUserByEmail(sub).getId();
            }
        }
        throw new RuntimeException("Unable to extract user ID from authentication");
    }
}
