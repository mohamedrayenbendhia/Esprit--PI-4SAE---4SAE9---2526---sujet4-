package com.smartfreelance.microservice.complaintservice.controller;

import com.smartfreelance.microservice.complaintservice.dto.InvolveReportedRequest;
import com.smartfreelance.microservice.complaintservice.dto.SupportMessageDTO;
import com.smartfreelance.microservice.complaintservice.dto.SupportMessageRequestDTO;
import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.entity.SupportMessage;
import com.smartfreelance.microservice.complaintservice.entity.SupportMessage.ConversationType;
import com.smartfreelance.microservice.complaintservice.mapper.SupportMessageMapper;
import com.smartfreelance.microservice.complaintservice.notification.ComplaintNotificationEvent;
import com.smartfreelance.microservice.complaintservice.notification.ComplaintNotificationService;
import com.smartfreelance.microservice.complaintservice.repository.SupportMessageRepository;
import com.smartfreelance.microservice.complaintservice.service.ComplaintService;
import com.smartfreelance.microservice.complaintservice.service.SupportMessageService;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/support-messages")
@RequiredArgsConstructor
@Slf4j
public class SupportMessageController {

    private final SupportMessageService messageService;
    private final SupportMessageMapper messageMapper;
    private final ComplaintService complaintService;
    private final ComplaintNotificationService notificationService;
    private final SupportMessageRepository messageRepository;
    private final com.smartfreelance.microservice.complaintservice.service.SlaService slaService;

    @Value("${user-service.url:http://localhost:8084}")
    private String userServiceUrl;

    private WebClient userServiceClient;

    @PostConstruct
    public void init() {
        this.userServiceClient = WebClient.builder().baseUrl(userServiceUrl).build();
    }

    // =========================================================================
    // CRUD
    // =========================================================================

    /**
     * Envoyer un message.
     *
     * Règles métier :
     * - FREELANCE/CLIENT    → peut envoyer uniquement sur SES réclamations
     * - SUPPORT_AGENT       → peut envoyer uniquement sur ses réclamations ASSIGNÉES
     * - ADMIN               → peut envoyer sur n'importe quelle réclamation
     *
     * conversationType :
     * - COMPLAINANT (défaut) = fil plaignant ↔ support
     * - REPORTED             = fil partie mise en cause ↔ support
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('FREELANCE', 'CLIENT', 'SUPPORT_AGENT', 'ADMIN')")
    public ResponseEntity<?> createMessage(
            @Valid @RequestBody SupportMessageRequestDTO request,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {

        boolean isPrivileged = userRole.equals("SUPPORT_AGENT") || userRole.equals("ADMIN");

        // Récupérer la réclamation
        Complaint complaint;
        try {
            complaint = complaintService.getComplaintById(request.getComplaintId());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Réclamation introuvable"));
        }

        // US10 — bloquer les messages sur une réclamation clôturée
        if (complaint.getStatus() == Complaint.Status.CLOSED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                        "error",  "Cette réclamation est clôturée.",
                        "detail", "Il n'est plus possible d'envoyer des messages sur une réclamation clôturée."
                    ));
        }

        // US10 — les utilisateurs ne peuvent plus écrire après résolution
        if (complaint.getStatus() == Complaint.Status.RESOLVED && !isPrivileged) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                        "error",  "Cette réclamation est résolue.",
                        "detail", "Vous ne pouvez plus envoyer de messages. " +
                                  "Si vous avez des questions, attendez la clôture officielle."
                    ));
        }

        // FREELANCE/CLIENT : distinguer plaignant et partie mise en cause
        if (!isPrivileged) {
            boolean isReporter = complaint.getReporterId().equals(userId);
            boolean isReported  = userId.equals(complaint.getReportedUserId());

            if (!isReporter && !isReported) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Vous ne pouvez envoyer des messages que sur vos propres réclamations"));
            }

            if (isReporter) {
                // Le plaignant est toujours dans le fil COMPLAINANT
                request.setConversationType(ConversationType.COMPLAINANT);
            } else {
                // La partie mise en cause est toujours dans le fil REPORTED
                request.setConversationType(ConversationType.REPORTED);
            }
        }

        // SUPPORT_AGENT : doit être assigné
        if (userRole.equals("SUPPORT_AGENT") && !userId.equals(complaint.getAssignedToId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error",
                            "Vous ne pouvez répondre que sur les réclamations qui vous sont assignées."));
        }

        // Vérifier que le fil REPORTED a bien été initialisé avant qu'un utilisateur y écrive
        if (ConversationType.REPORTED.equals(request.getConversationType())) {
            boolean filExists = messageRepository
                    .existsByComplaintIdAndConversationType(complaint.getId(), ConversationType.REPORTED);
            if (!filExists) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error",
                                "La partie mise en cause n'a pas encore été impliquée. "
                                        + "Utilisez d'abord l'action 'Impliquer la partie'."));
            }
        }

        // Bloquer les notes internes pour les utilisateurs non-support
        if (!isPrivileged && SupportMessage.MessageType.NOTE_INTERNE.equals(request.getMessageType())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Seuls les agents support peuvent créer des notes internes"));
        }

        // Construire et sauvegarder le message
        SupportMessage message = messageMapper.toEntity(request);
        message.setSenderId(userId);
        message.setSenderType(isPrivileged ? SupportMessage.SenderType.SUPPORT : SupportMessage.SenderType.USER);

        SupportMessage created = messageService.createMessage(message);
        log.info("[Message] Créé par {} dans le fil {} de la réclamation {}",
                userId, request.getConversationType(), complaint.getId());

        // Enregistrer la première réponse SLA si c'est le support qui répond
        if (SupportMessage.SenderType.SUPPORT.equals(created.getSenderType())) {
            slaService.recordFirstResponse(complaint.getId());
        }

        // Notification push + email conditionnel au destinataire du fil
        notifyNewMessage(complaint, created, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(messageMapper.toDTO(created));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT', 'FREELANCE', 'CLIENT')")
    public ResponseEntity<SupportMessageDTO> getMessageById(@PathVariable String id) {
        return ResponseEntity.ok(messageMapper.toDTO(messageService.getMessageById(id)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SupportMessageDTO>> getAllMessages() {
        return ResponseEntity.ok(messageMapper.toDTOList(messageService.getAllMessages()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SupportMessageDTO> updateMessage(
            @PathVariable String id,
            @Valid @RequestBody SupportMessageRequestDTO request) {
        return ResponseEntity.ok(messageMapper.toDTO(
                messageService.updateMessage(id, messageMapper.toEntity(request))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteMessage(@PathVariable String id) {
        messageService.deleteMessage(id);
        return ResponseEntity.ok(Map.of("message", "Message supprimé", "id", id));
    }

    // =========================================================================
    // RECHERCHES FILTRÉES PAR CONVERSATION
    // =========================================================================

    /**
     * Messages d'une réclamation — filtrés par fil de conversation et rôle.
     *
     * ADMIN/SUPPORT_AGENT :
     *   - Voient les deux fils (COMPLAINANT + REPORTED)
     *   - Voient aussi les NOTE_INTERNE
     *   - Paramètre conversationType optionnel pour filtrer sur un fil
     *
     * FREELANCE/CLIENT :
     *   - Voient uniquement le fil auquel ils appartiennent
     *   - Ne voient pas les NOTE_INTERNE
     *   - Le paramètre conversationType est ignoré (forcé par le backend)
     */
    @GetMapping("/complaint/{complaintId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT', 'FREELANCE', 'CLIENT')")
    public ResponseEntity<?> getMessagesByComplaint(
            @PathVariable String complaintId,
            @RequestParam(required = false) ConversationType conversationType,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {

        boolean isPrivileged = userRole.equals("SUPPORT_AGENT") || userRole.equals("ADMIN");

        List<SupportMessage> messages;

        if (isPrivileged) {
            // Support/admin : filtre optionnel sur le fil
            if (conversationType != null) {
                messages = messageRepository
                        .findByComplaintIdAndConversationTypeOrderByCreatedAtAsc(complaintId, conversationType);
            } else {
                messages = messageService.getMessagesByComplaint(complaintId);
            }
            // Les agents voient leurs propres notes internes uniquement (pas celles des autres agents).
            // L'admin voit toutes les notes internes.
            if (userRole.equals("SUPPORT_AGENT")) {
                final String agentUserId = userId;
                messages = messages.stream()
                        .filter(m ->
                            m.getMessageType() != SupportMessage.MessageType.NOTE_INTERNE
                            || agentUserId.equals(m.getSenderId())
                        )
                        .toList();
            }
        } else {
            // Utilisateur : déterminer son fil
            Complaint complaint;
            try {
                complaint = complaintService.getComplaintById(complaintId);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Réclamation introuvable"));
            }

            boolean isReporter  = userId.equals(complaint.getReporterId());
            boolean isReported  = userId.equals(complaint.getReportedUserId());

            if (!isReporter && !isReported) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès refusé"));
            }

            ConversationType userConvType = isReporter ? ConversationType.COMPLAINANT : ConversationType.REPORTED;
            messages = messageRepository
                    .findByComplaintIdAndConversationTypeOrderByCreatedAtAsc(complaintId, userConvType);

            // Filtrer les notes internes
            messages = messages.stream()
                    .filter(m -> m.getMessageType() != SupportMessage.MessageType.NOTE_INTERNE)
                    .toList();
        }

        return ResponseEntity.ok(messageMapper.toDTOList(messages));
    }

    @GetMapping("/sender/{senderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ResponseEntity<List<SupportMessageDTO>> getMessagesBySender(@PathVariable String senderId) {
        return ResponseEntity.ok(messageMapper.toDTOList(messageService.getMessagesBySender(senderId)));
    }

    @GetMapping("/complaint/{complaintId}/unread")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT', 'FREELANCE', 'CLIENT')")
    public ResponseEntity<List<SupportMessageDTO>> getUnreadMessages(@PathVariable String complaintId) {
        return ResponseEntity.ok(messageMapper.toDTOList(messageService.getUnreadMessages(complaintId)));
    }

    @GetMapping("/complaint/{complaintId}/latest")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT', 'FREELANCE', 'CLIENT')")
    public ResponseEntity<SupportMessageDTO> getLatestMessage(@PathVariable String complaintId) {
        return ResponseEntity.ok(messageMapper.toDTO(messageService.getLatestMessage(complaintId)));
    }

    // =========================================================================
    // ACTIONS
    // =========================================================================

    @PutMapping("/{id}/mark-read")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT', 'FREELANCE', 'CLIENT')")
    public ResponseEntity<SupportMessageDTO> markAsRead(@PathVariable String id) {
        return ResponseEntity.ok(messageMapper.toDTO(messageService.markAsRead(id)));
    }

    @PutMapping("/complaint/{complaintId}/mark-all-read")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ResponseEntity<List<SupportMessageDTO>> markAllAsRead(@PathVariable String complaintId) {
        return ResponseEntity.ok(messageMapper.toDTOList(messageService.markAllAsRead(complaintId)));
    }

    @GetMapping("/complaint/{complaintId}/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT', 'FREELANCE', 'CLIENT')")
    public ResponseEntity<Map<String, Long>> countUnreadMessages(@PathVariable String complaintId) {
        return ResponseEntity.ok(Map.of("unreadCount", messageService.countUnreadMessages(complaintId)));
    }

    // =========================================================================
    // IMPLIQUER LA PARTIE MISE EN CAUSE
    // =========================================================================

    /**
     * Action explicite : le support/admin implique la partie mise en cause.
     *
     * Effets :
     *  1. Crée le fil de conversation REPORTED avec un message d'invitation automatique
     *  2. Envoie une notification push + email à la partie mise en cause
     *
     * Préconditions :
     *  - La réclamation doit avoir un reportedUserId
     *  - Le fil REPORTED ne doit pas encore exister
     */
    @PostMapping("/complaint/{complaintId}/involve-reported")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ResponseEntity<?> involveReportedUser(
            @PathVariable String complaintId,
            @Valid @RequestBody InvolveReportedRequest request,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {

        // Vérifier la réclamation
        Complaint complaint;
        try {
            complaint = complaintService.getComplaintById(complaintId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Réclamation introuvable"));
        }

        if (complaint.getReportedUserId() == null || complaint.getReportedUserId().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Cette réclamation n'a pas de partie mise en cause"));
        }

        // Vérifier que le fil n'existe pas déjà
        if (messageRepository.existsByComplaintIdAndConversationType(complaintId, ConversationType.REPORTED)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "La partie mise en cause est déjà impliquée dans cette réclamation"));
        }

        // Créer le message d'invitation (premier message du fil REPORTED)
        SupportMessage invitationMsg = SupportMessage.builder()
                .complaintId(complaintId)
                .senderId(userId)
                .senderType(SupportMessage.SenderType.SUPPORT)
                .messageType(SupportMessage.MessageType.AUTO_RESPONSE)
                .conversationType(ConversationType.REPORTED)
                .content(request.getInvitationMessage())
                .isRead(false)
                .build();

        messageService.createMessage(invitationMsg);
        log.info("[Involve] Agent {} a impliqué la partie {} dans la réclamation {}",
                userId, complaint.getReportedUserId(), complaintId);

        // Notification push + email à la partie mise en cause
        ComplaintNotificationEvent event = ComplaintNotificationEvent.builder()
                .eventType(ComplaintNotificationEvent.EventType.REPORTED_INVOLVED)
                .complaintId(complaint.getId())
                .ticketNumber(complaint.getTicketNumber())
                .complaintSubject(complaint.getSubject())
                .reporterId(complaint.getReporterId())
                .reportedUserId(complaint.getReportedUserId())
                .invitationMessage(request.getInvitationMessage())
                .build();
        notificationService.handle(event);

        return ResponseEntity.ok(Map.of(
                "message", "La partie mise en cause a été impliquée avec succès",
                "reportedUserId", complaint.getReportedUserId()
        ));
    }

    // =========================================================================
    // HELPER
    // =========================================================================

    /**
     * B-N2 — Calcule le nom d'affichage de l'expéditeur.
     * SUPPORT → "Support NexLance" ; USER → prénom + nom depuis user-service.
     */
    private String resolveSenderName(String senderId, SupportMessage.SenderType senderType) {
        if (SupportMessage.SenderType.SUPPORT.equals(senderType)) {
            return "Support NexLance";
        }
        try {
            Map<String, Object> user = userServiceClient.get()
                    .uri("/api/users/profile/{id}", senderId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            if (user != null) {
                String firstName = (String) user.getOrDefault("firstName", "");
                String lastName  = (String) user.getOrDefault("lastName",  "");
                String name = (firstName + " " + lastName).trim();
                return name.isBlank() ? "Un utilisateur" : name;
            }
        } catch (Exception e) {
            log.warn("[Notify] Impossible de résoudre le nom de {} : {}", senderId, e.getMessage());
        }
        return "Un utilisateur";
    }

    /**
     * B-N2 — Construit et envoie l'événement NEW_MESSAGE avec le bon destinataire.
     *
     * COMPLAINANT : support→user = reporterId | user→support = assignedToId
     * REPORTED    : support→reported = reportedUserId | reported→support = assignedToId
     * Si assignedToId = null et expéditeur USER → flag unassigned = true
     */
    private void notifyNewMessage(Complaint complaint, SupportMessage message, String senderId) {
        boolean isSupportSender = SupportMessage.SenderType.SUPPORT.equals(message.getSenderType());

        String recipientId;
        boolean unassigned = false;

        if (ConversationType.REPORTED.equals(message.getConversationType())) {
            recipientId = isSupportSender
                    ? complaint.getReportedUserId()
                    : complaint.getAssignedToId();
        } else {
            if (isSupportSender) {
                recipientId = complaint.getReporterId();
            } else if (complaint.getAssignedToId() != null) {
                recipientId = complaint.getAssignedToId();
            } else {
                recipientId = null;
                unassigned  = true;
                log.warn("[Notify] Réclamation {} non assignée — message de {} sans destinataire agent",
                        complaint.getId(), senderId);
            }
        }

        if (recipientId != null && recipientId.equals(senderId)) return;

        String senderName = resolveSenderName(senderId, message.getSenderType());

        // GAP #8 — si personne n'est assigné, prévenir un admin pour triage de la queue
        String adminId = unassigned ? resolveFirstAdminId() : null;

        ComplaintNotificationEvent event = ComplaintNotificationEvent.builder()
                .eventType(ComplaintNotificationEvent.EventType.NEW_MESSAGE)
                .complaintId(complaint.getId())
                .ticketNumber(complaint.getTicketNumber())
                .complaintSubject(complaint.getSubject())
                .reporterId(complaint.getReporterId())
                .reportedUserId(complaint.getReportedUserId())
                .assignedToId(complaint.getAssignedToId())
                .recipientId(recipientId)
                .unassigned(unassigned)
                .secondaryUserId(adminId)
                .senderName(senderName)
                .messageExcerpt(message.getContent())
                .conversationType(message.getConversationType().name())
                .build();

        notificationService.handle(event);
    }

    /**
     * GAP #8 — Résout l'identifiant d'un administrateur pour router les messages
     * postés sur une réclamation non assignée. Retourne null si la recherche échoue
     * (la notification sera simplement loggée côté handler).
     */
    @SuppressWarnings("unchecked")
    private String resolveFirstAdminId() {
        try {
            List<Map<String, Object>> admins = userServiceClient.get()
                    .uri("/api/users/by-role/ADMIN")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .block();
            if (admins != null && !admins.isEmpty()) {
                Object id = admins.get(0).get("id");
                return id != null ? id.toString() : null;
            }
        } catch (Exception e) {
            log.warn("[Notify] Impossible de résoudre l'admin pour GAP #8 : {}", e.getMessage());
        }
        return null;
    }
}