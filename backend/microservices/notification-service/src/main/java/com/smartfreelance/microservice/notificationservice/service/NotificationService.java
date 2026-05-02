package com.smartfreelance.microservice.notificationservice.service;

import com.smartfreelance.microservice.notificationservice.dto.NotificationRequestDTO;
import com.smartfreelance.microservice.notificationservice.dto.NotificationResponseDTO;
import com.smartfreelance.microservice.notificationservice.entity.Notification;
import com.smartfreelance.microservice.notificationservice.repository.NotificationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository repository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationResponseDTO create(NotificationRequestDTO dto) {
        Notification notification = Notification.builder()
                .recipientId(dto.getRecipientId())
                .type(dto.getType())
                .title(dto.getTitle())
                .message(dto.getMessage())
                .referenceId(dto.getReferenceId())
                .referenceType(dto.getReferenceType())
                .build();

        Notification saved = repository.save(notification);
        log.info("[Notification] Created for {} — type={} title={}", dto.getRecipientId(), dto.getType(), dto.getTitle());

        NotificationResponseDTO dto2 = toDTO(saved);
        try {
            messagingTemplate.convertAndSend("/topic/notifications/" + saved.getRecipientId(), dto2);
            log.debug("[WS] Push sent to /topic/notifications/{}", saved.getRecipientId());
        } catch (Exception ex) {
            log.warn("[WS] STOMP push failed for {} : {}", saved.getRecipientId(), ex.getMessage());
        }

        return dto2;
    }

    public Page<NotificationResponseDTO> getByUser(String userId, int page, int size) {
        return repository.findByRecipientIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(this::toDTO);
    }

    public List<NotificationResponseDTO> getUnreadByUser(String userId) {
        return repository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public long countUnread(String userId) {
        return repository.countByRecipientIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponseDTO markAsRead(String id) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + id));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            repository.save(notification);
        }
        return toDTO(notification);
    }

    @Transactional
    public int markAllAsRead(String userId) {
        int count = repository.markAllAsReadByRecipientId(userId);
        log.info("[Notification] {} notifications marked as read for {}", count, userId);
        return count;
    }

    private NotificationResponseDTO toDTO(Notification n) {
        return NotificationResponseDTO.builder()
                .id(n.getId())
                .recipientId(n.getRecipientId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .readAt(n.getReadAt())
                .build();
    }
}
