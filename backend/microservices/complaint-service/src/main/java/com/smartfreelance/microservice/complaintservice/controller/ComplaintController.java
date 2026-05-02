package com.smartfreelance.microservice.complaintservice.controller;

import com.smartfreelance.microservice.complaintservice.dto.ComplaintDTO;
import com.smartfreelance.microservice.complaintservice.dto.ComplaintEventDTO;
import com.smartfreelance.microservice.complaintservice.dto.ComplaintRequestDTO;
import com.smartfreelance.microservice.complaintservice.dto.ComplaintUpdateDTO;
import com.smartfreelance.microservice.complaintservice.dto.ResolveComplaintRequest;
import com.smartfreelance.microservice.complaintservice.service.ComplaintEventService;
import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.entity.Complaint.Priority;
import com.smartfreelance.microservice.complaintservice.entity.Complaint.Status;
import com.smartfreelance.microservice.complaintservice.entity.SupportMessage;
import com.smartfreelance.microservice.complaintservice.mapper.ComplaintMapper;
import com.smartfreelance.microservice.complaintservice.notification.ComplaintNotificationEvent;
import com.smartfreelance.microservice.complaintservice.notification.ComplaintNotificationService;
import com.smartfreelance.microservice.complaintservice.repository.SupportMessageRepository;
import com.smartfreelance.microservice.complaintservice.service.ComplaintService;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
@Slf4j
public class ComplaintController {

    private final ComplaintService complaintService;
    private final ComplaintMapper  complaintMapper;
    private final ComplaintNotificationService notificationService;
    private final SupportMessageRepository messageRepository;
    private final ComplaintEventService eventService;

    @Value("${user-service.url:http://localhost:8084}")
    private String userServiceUrl;

    @Value("${complaint.service-url:http://localhost:8092}")
    private String complaintServiceUrl;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder().baseUrl(userServiceUrl).build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FREELANCE', 'CLIENT')")
    public ResponseEntity<?> createComplaint(
            @Valid @RequestBody ComplaintRequestDTO request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("Utilisateur {} crée un litige : {}", userId, request.getSubject());
        String resolvedReportedUserId = null;

        if (request.getReportedUserEmail() != null && !request.getReportedUserEmail().isBlank()) {
            try {
                Map<String, Object> userResponse = webClient.get()
                        .uri("/api/users/email/{email}", request.getReportedUserEmail())
                        .headers(h -> { if (authHeader != null) h.set("Authorization", authHeader); })
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .block();
                if (userResponse == null || userResponse.get("id") == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Aucun utilisateur trouvé pour l'adresse e-mail fournie"));
                }
                resolvedReportedUserId = (String) userResponse.get("id");
            } catch (Exception e) {
                log.warn("[Create] Email {} introuvable : {}", request.getReportedUserEmail(), e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Aucun utilisateur trouvé pour l'adresse e-mail fournie"));
            }
        }
        if (resolvedReportedUserId != null && resolvedReportedUserId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Vous ne pouvez pas vous signaler vous-même"));
        }
        Complaint complaint = complaintMapper.toEntity(request);
        complaint.setReporterId(userId);
        complaint.setReportedUserId(resolvedReportedUserId);
        complaint.setAssignedToId(null);
        Complaint saved = complaintService.createComplaint(complaint);
        notificationService.handle(ComplaintNotificationEvent.builder()
                .eventType(ComplaintNotificationEvent.EventType.COMPLAINT_CREATED)
                .complaintId(saved.getId())
                .ticketNumber(saved.getTicketNumber())
                .complaintSubject(saved.getSubject())
                .reporterId(saved.getReporterId())
                .build());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(complaintMapper.toDTO(saved));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT', 'FREELANCE', 'CLIENT')")
    public ResponseEntity<ComplaintDTO> getComplaintById(
            @PathVariable String id,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {

        Complaint complaint = complaintService.getComplaintById(id);
        boolean isPrivileged = userRole.equals("ADMIN") || userRole.equals("SUPPORT_AGENT");
        if (userRole.equals("SUPPORT_AGENT")
                && complaint.getAssignedToId() != null
                && !userId.equals(complaint.getAssignedToId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        boolean isReporter = complaint.getReporterId().equals(userId);
        boolean isReported  = userId.equals(complaint.getReportedUserId());
        if (!isPrivileged && !isReporter && !isReported) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(complaintMapper.toDTO(complaint));
    }

    @GetMapping("/ticket/{ticketNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ResponseEntity<ComplaintDTO> getComplaintByTicketNumber(
            @PathVariable String ticketNumber) {
        return ResponseEntity.ok(complaintMapper.toDTO(
                complaintService.getComplaintByTicketNumber(ticketNumber)));
    }

    /** ADMIN uniquement — tous les litiges sans filtre */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ComplaintDTO>> getAllComplaints() {
        return ResponseEntity.ok(complaintMapper.toDTOList(complaintService.getAllComplaints()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComplaintDTO> updateComplaint(
            @PathVariable String id,
            @Valid @RequestBody ComplaintUpdateDTO updateDTO) {

        Complaint existing = complaintService.getComplaintById(id);
        complaintMapper.updateEntityFromDTO(existing, updateDTO);
        return ResponseEntity.ok(complaintMapper.toDTO(
                complaintService.updateComplaint(id, existing)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteComplaint(@PathVariable String id) {
        complaintService.deleteComplaint(id);
        return ResponseEntity.ok(Map.of("message", "Litige supprimé", "id", id));
    }

    @GetMapping("/admin/queue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ComplaintDTO>> getAdminQueue() {
        List<Complaint> unassigned = complaintService.getUnassignedComplaints(Status.OPEN);
        List<Complaint> escalated  = complaintService.getComplaintsByStatus(Status.ESCALATED);

        List<Complaint> queue = new ArrayList<>(unassigned);
        escalated.forEach(c -> {
            if (queue.stream().noneMatch(q -> q.getId().equals(c.getId()))) {
                queue.add(c);
            }
        });

        log.debug("File admin : {} litiges non assignés + {} escaladés", unassigned.size(), escalated.size());
        return ResponseEntity.ok(complaintMapper.toDTOList(queue));
    }

    @GetMapping("/agent/queue")
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT', 'ADMIN')")
    public ResponseEntity<List<ComplaintDTO>> getAgentQueue(
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {

        if (userRole.equals("ADMIN")) {
            return ResponseEntity.ok(complaintMapper.toDTOList(complaintService.getAllComplaints()));
        }

        List<Complaint> mine = complaintService.getComplaintsByAssignedAgent(userId);
        return ResponseEntity.ok(complaintMapper.toDTOList(mine));
    }

    @GetMapping("/agent/my-assigned")
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT', 'ADMIN')")
    public ResponseEntity<List<ComplaintDTO>> getMyAssignedComplaints(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(complaintMapper.toDTOList(
                complaintService.getComplaintsByAssignedAgent(userId)));
    }

    @GetMapping("/agent/available")
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT', 'ADMIN')")
    public ResponseEntity<List<ComplaintDTO>> getAvailableComplaints() {
        List<Complaint> available = complaintService.getUnassignedComplaints(Status.OPEN);
        return ResponseEntity.ok(complaintMapper.toDTOList(available));
    }

    @PutMapping("/{id}/take")
    @PreAuthorize("hasRole('SUPPORT_AGENT')")
    public ResponseEntity<?> takeComplaint(
            @PathVariable String id,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {

        Complaint complaint = complaintService.getComplaintById(id);

        if (complaint.getAssignedToId() != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error",
                        "Cette réclamation est déjà assignée. " +
                        "Seul l'administrateur peut la réassigner."));
        }

        Complaint updated = complaintService.assignComplaint(id, userId);
        updated = complaintService.updateStatus(id, Status.IN_PROGRESS);

        // GAP #5 — auto-assignation : on notifie uniquement le reporter
        // (l'agent vient de cliquer « prendre », pas besoin de lui renvoyer l'info)
        notificationService.handle(ComplaintNotificationEvent.builder()
                .eventType(ComplaintNotificationEvent.EventType.COMPLAINT_ASSIGNED)
                .complaintId(updated.getId())
                .ticketNumber(updated.getTicketNumber())
                .complaintSubject(updated.getSubject())
                .reporterId(updated.getReporterId())
                .assignedToId(userId)
                .selfAssignment(true)
                .build());

        log.info("[Take] Agent {} a pris la réclamation {}", userId, id);
        return ResponseEntity.ok(complaintMapper.toDTO(updated));
    }

    @GetMapping("/my-complaints")
    @PreAuthorize("hasAnyRole('FREELANCE', 'CLIENT', 'ADMIN', 'SUPPORT_AGENT')")
    public ResponseEntity<List<ComplaintDTO>> getMyComplaints(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(complaintMapper.toDTOList(
                complaintService.getComplaintsByReporter(userId)));
    }

    /**
     * US2 — Réclamations où l'utilisateur connecté est la partie mise en cause.
     * Retourne uniquement celles où le fil REPORTED a été ouvert par le support.
     */
    @GetMapping("/involved")
    @PreAuthorize("hasAnyRole('FREELANCE', 'CLIENT')")
    public ResponseEntity<List<ComplaintDTO>> getComplaintsWhereInvolved(
            @RequestHeader("X-User-Id") String userId) {
        List<Complaint> involved = complaintService.getComplaintsByReportedUser(userId);
        List<Complaint> accessible = involved.stream()
                .filter(c -> messageRepository.existsByComplaintIdAndConversationType(
                        c.getId(), SupportMessage.ConversationType.REPORTED))
                .toList();
        return ResponseEntity.ok(complaintMapper.toDTOList(accessible));
    }

    @GetMapping("/reporter/{reporterId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT', 'FREELANCE', 'CLIENT')")
    public ResponseEntity<List<ComplaintDTO>> getComplaintsByReporter(
            @PathVariable String reporterId,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {

        boolean isPrivileged = userRole.equals("ADMIN") || userRole.equals("SUPPORT_AGENT");
        if (!isPrivileged && !reporterId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(complaintMapper.toDTOList(
                complaintService.getComplaintsByReporter(reporterId)));
    }

    @GetMapping("/reported-user/{reportedUserId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ResponseEntity<List<ComplaintDTO>> getComplaintsByReportedUser(
            @PathVariable String reportedUserId) {
        return ResponseEntity.ok(complaintMapper.toDTOList(
                complaintService.getComplaintsByReportedUser(reportedUserId)));
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ResponseEntity<List<ComplaintDTO>> getComplaintsByProject(
            @PathVariable String projectId) {
        return ResponseEntity.ok(complaintMapper.toDTOList(
                complaintService.getComplaintsByProject(projectId)));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ResponseEntity<List<ComplaintDTO>> getComplaintsByStatus(@PathVariable Status status) {
        return ResponseEntity.ok(complaintMapper.toDTOList(
                complaintService.getComplaintsByStatus(status)));
    }

    @GetMapping("/priority/{priority}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ResponseEntity<List<ComplaintDTO>> getComplaintsByPriority(@PathVariable Priority priority) {
        return ResponseEntity.ok(complaintMapper.toDTOList(
                complaintService.getComplaintsByPriority(priority)));
    }

    @GetMapping("/assigned/{agentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ResponseEntity<List<ComplaintDTO>> getComplaintsByAssignedAgent(
            @PathVariable String agentId,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {

        if (userRole.equals("SUPPORT_AGENT") && !agentId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(complaintMapper.toDTOList(
                complaintService.getComplaintsByAssignedAgent(agentId)));
    }

    @GetMapping("/unassigned")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ComplaintDTO>> getUnassignedComplaints(
            @RequestParam(defaultValue = "OPEN") Status status) {
        return ResponseEntity.ok(complaintMapper.toDTOList(
                complaintService.getUnassignedComplaints(status)));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ResponseEntity<List<ComplaintDTO>> getOverdueComplaints(
            @RequestParam(defaultValue = "7") int daysThreshold) {
        return ResponseEntity.ok(complaintMapper.toDTOList(
                complaintService.getOverdueComplaints(daysThreshold)));
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ResponseEntity<List<ComplaintDTO>> getComplaintsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(complaintMapper.toDTOList(
                complaintService.getComplaintsByDateRange(startDate, endDate)));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ResponseEntity<?> assignComplaint(
            @PathVariable String id,
            @RequestParam String agentId,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Complaint complaint = complaintService.getComplaintById(id);
        if (userRole.equals("SUPPORT_AGENT")) {
            if (!userId.equals(complaint.getAssignedToId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Vous ne pouvez réassigner que les litiges qui vous sont assignés"));
            }
            if (complaint.getPriority() != Priority.CRITICAL) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                            "error",           "Réassignation non autorisée.",
                            "detail",          "Seules les réclamations CRITIQUES peuvent être réassignées par un agent.",
                            "currentPriority", complaint.getPriority().name(),
                            "suggestion",      "Demandez à un administrateur de réassigner cette réclamation, " +
                                               "ou escaladez-la en passant la priorité à CRITICAL si justifié."
                        ));
            }
            try {
                Map<String, Object> roleResponse = webClient.get()
                        .uri("/api/users/{id}/role", agentId)
                        .headers(h -> { if (authHeader != null) h.set("Authorization", authHeader); })
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .block();
                String targetRole = roleResponse != null ? (String) roleResponse.get("role") : null;
                if (!"SUPPORT_AGENT".equals(targetRole) && !"ADMIN".equals(targetRole)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error",
                                    "La cible doit être un agent de support ou un administrateur"));
                }
                if ("ADMIN".equals(targetRole)) {
                    complaintService.updateStatus(id, Status.ESCALATED);
                    log.info("Litige {} escaladé vers l'admin {} par l'agent {}", id, agentId, userId);
                }
            } catch (Exception e) {
                log.error("Erreur vérification rôle cible : {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("error", "Impossible de vérifier le rôle de la cible"));
            }
        }
        log.info("Litige {} assigné à {} par {} (rôle: {})", id, agentId, userId, userRole);
        boolean wasEscalation = (complaint.getStatus() != Status.ESCALATED)
                && (complaintService.getComplaintById(id).getStatus() == Status.ESCALATED);
        Complaint updated = complaintService.assignComplaint(id, agentId);
        if (userRole.equals("ADMIN") && updated.getStatus() == Status.OPEN) {
            updated = complaintService.updateStatus(id, Status.IN_PROGRESS);
        }
        if (wasEscalation) {
            notificationService.handle(ComplaintNotificationEvent.builder()
                    .eventType(ComplaintNotificationEvent.EventType.COMPLAINT_ESCALATED)
                    .complaintId(updated.getId())
                    .ticketNumber(updated.getTicketNumber())
                    .complaintSubject(updated.getSubject())
                    .reporterId(updated.getReporterId())
                    .assignedToId(agentId)
                    .newStatus(Status.ESCALATED.name())
                    .build());
        }
        notificationService.handle(ComplaintNotificationEvent.builder()
                .eventType(ComplaintNotificationEvent.EventType.COMPLAINT_ASSIGNED)
                .complaintId(updated.getId())
                .ticketNumber(updated.getTicketNumber())
                .complaintSubject(updated.getSubject())
                .reporterId(updated.getReporterId())
                .assignedToId(agentId)
                .build());
        return ResponseEntity.ok(complaintMapper.toDTO(updated));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ResponseEntity<?> updateComplaintStatus(
            @PathVariable String id,
            @RequestParam Status status,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {
        if (status == Status.CLOSED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "error",  "Utilisez l'action 'Clôturer' dédiée.",
                        "detail", "La clôture exige que la réclamation soit d'abord résolue. " +
                                  "Utilisez PUT /" + id + "/close."
                    ));
        }
        if (status == Status.RESOLVED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "error",  "Utilisez l'action 'Résoudre' dédiée.",
                        "detail", "La résolution nécessite un texte explicatif. " +
                                  "Utilisez PUT /" + id + "/resolve."
                    ));
        }
        Complaint complaint = complaintService.getComplaintById(id);
        if (userRole.equals("SUPPORT_AGENT") && !userId.equals(complaint.getAssignedToId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (status == Status.OPEN && complaint.getStatus() != Status.OPEN) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error",
                        "Impossible de repasser une réclamation à l'état OPEN une fois traitée."));
        }

        Status oldStatus = complaint.getStatus();
        Complaint updated = complaintService.updateStatus(id, status);
        ComplaintNotificationEvent.EventType eventType =
            (status == Status.ESCALATED)
                ? ComplaintNotificationEvent.EventType.COMPLAINT_ESCALATED
                : ComplaintNotificationEvent.EventType.STATUS_CHANGED;
        notificationService.handle(ComplaintNotificationEvent.builder()
                .eventType(eventType)
                .complaintId(updated.getId())
                .ticketNumber(updated.getTicketNumber())
                .complaintSubject(updated.getSubject())
                .reporterId(updated.getReporterId())
                .assignedToId(updated.getAssignedToId())
                .oldStatus(oldStatus.name())
                .newStatus(status.name())
                .build());
        return ResponseEntity.ok(complaintMapper.toDTO(updated));
    }

    @PutMapping("/{id}/priority")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ResponseEntity<ComplaintDTO> updateComplaintPriority(
            @PathVariable String id,
            @RequestParam Priority priority,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {
        Complaint complaint = complaintService.getComplaintById(id);
        if (userRole.equals("SUPPORT_AGENT") && !userId.equals(complaint.getAssignedToId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Priority oldPriority = complaint.getPriority();
        Complaint updated = complaintService.updatePriority(id, priority);
        if (oldPriority != priority) {
            notificationService.handle(ComplaintNotificationEvent.builder()
                    .eventType(ComplaintNotificationEvent.EventType.PRIORITY_CHANGED)
                    .complaintId(updated.getId())
                    .ticketNumber(updated.getTicketNumber())
                    .complaintSubject(updated.getSubject())
                    .reporterId(updated.getReporterId())
                    .assignedToId(updated.getAssignedToId())
                    .oldPriority(oldPriority != null ? oldPriority.name() : null)
                    .newPriority(priority.name())
                    .build());
        }
        return ResponseEntity.ok(complaintMapper.toDTO(updated));
    }

    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ResponseEntity<?> resolveComplaint(
            @PathVariable String id,
            @Valid @RequestBody ResolveComplaintRequest request,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {
        Complaint complaint = complaintService.getComplaintById(id);
        if (userRole.equals("SUPPORT_AGENT") && !userId.equals(complaint.getAssignedToId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Vous ne pouvez résoudre que vos litiges assignés"));
        }

        Complaint updated = complaintService.resolveComplaint(id, request.getResolution(), request.getResolutionType());
        notificationService.handle(ComplaintNotificationEvent.builder()
                .eventType(ComplaintNotificationEvent.EventType.COMPLAINT_RESOLVED)
                .complaintId(updated.getId())
                .ticketNumber(updated.getTicketNumber())
                .complaintSubject(updated.getSubject())
                .reporterId(updated.getReporterId())
                .assignedToId(updated.getAssignedToId())
                .newStatus(Status.RESOLVED.name())
                .build());
        return ResponseEntity.ok(complaintMapper.toDTO(updated));
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComplaintDTO> closeComplaint(@PathVariable String id) {
        Complaint updated = complaintService.closeComplaint(id);
        notificationService.handle(ComplaintNotificationEvent.builder()
                .eventType(ComplaintNotificationEvent.EventType.COMPLAINT_CLOSED)
                .complaintId(updated.getId())
                .ticketNumber(updated.getTicketNumber())
                .complaintSubject(updated.getSubject())
                .reporterId(updated.getReporterId())
                .newStatus(Status.CLOSED.name())
                .build());
        return ResponseEntity.ok(complaintMapper.toDTO(updated));
    }

    @PutMapping("/{id}/rate")
    @PreAuthorize("hasAnyRole('FREELANCE', 'CLIENT')")
    public ResponseEntity<?> rateComplaint(
            @PathVariable String id,
            @RequestParam Integer rating,
            @RequestHeader("X-User-Id") String userId) {
        Complaint complaint = complaintService.getComplaintById(id);
        if (!complaint.getReporterId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Vous ne pouvez noter que vos propres litiges"));
        }
        if (complaint.getStatus() != Status.CLOSED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Le litige doit être clôturé pour être noté"));
        }
        return ResponseEntity.ok(complaintMapper.toDTO(
                complaintService.rateComplaint(id, rating)));
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('FREELANCE', 'CLIENT', 'ADMIN', 'SUPPORT_AGENT')")
    public ResponseEntity<?> uploadAttachments(
            @RequestParam("files") List<MultipartFile> files,
            @RequestHeader("X-User-Id") String userId) {
        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Aucun fichier fourni"));
        }
        if (files.size() > 5) {
            return ResponseEntity.badRequest().body(Map.of("error", "Maximum 5 fichiers autorisés"));
        }
        List<String> uploadedUrls = new ArrayList<>();
        String uploadDir = "uploads/complaints/" + userId + "/";
        try {
            java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
            java.nio.file.Files.createDirectories(uploadPath);

            for (MultipartFile file : files) {
                if (file.getSize() > 10L * 1024 * 1024) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Fichier trop volumineux : " +
                                file.getOriginalFilename() + " (max 10 Mo)"));
                }
                String originalName = file.getOriginalFilename();
                String extension = (originalName != null && originalName.contains("."))
                        ? originalName.substring(originalName.lastIndexOf(".")) : "";
                String fileName = java.util.UUID.randomUUID() + extension;
                file.transferTo(uploadPath.resolve(fileName));
                uploadedUrls.add(complaintServiceUrl + "/uploads/complaints/" + userId + "/" + fileName);
            }

            return ResponseEntity.ok(Map.of("urls", uploadedUrls));
        } catch (Exception e) {
            log.error("[Upload] Erreur upload pour {} : {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de l'upload : " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/events")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT', 'FREELANCE', 'CLIENT')")
    public ResponseEntity<?> getComplaintEvents(
            @PathVariable String id,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {
        Complaint complaint = complaintService.getComplaintById(id);
        boolean isPrivileged = userRole.equals("ADMIN") || userRole.equals("SUPPORT_AGENT");
        if (!isPrivileged) {
            boolean isReporter = complaint.getReporterId().equals(userId);
            boolean isReported  = userId.equals(complaint.getReportedUserId());
            if (!isReporter && !isReported) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        return ResponseEntity.ok(eventService.getTimeline(id));
    }

    @GetMapping("/statistics/by-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getStatsByStatus() {
        Map<String, Long> stats = new HashMap<>();
        for (Status s : Status.values()) stats.put(s.name(), complaintService.countByStatus(s));
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/statistics/by-priority")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getStatsByPriority() {
        Map<String, Long> stats = new HashMap<>();
        for (Priority p : Priority.values()) stats.put(p.name(), complaintService.countByPriority(p));
        return ResponseEntity.ok(stats);
    }
}