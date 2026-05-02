package com.smartfreelance.microservice.notificationservice.service;

import com.smartfreelance.microservice.notificationservice.dto.NotificationRequestDTO;
import com.smartfreelance.microservice.notificationservice.entity.Notification;
import com.smartfreelance.microservice.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService service;

    private NotificationRequestDTO request;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    @BeforeEach
    void setUp() {
        request = new NotificationRequestDTO();
        request.setRecipientId("user-1");
        request.setType("INFO");
        request.setTitle("Hello");
        request.setMessage("World");
        request.setReferenceId("ref-1");
        request.setReferenceType("COMPLAINT");
    }

    @Test
    void create_shouldSaveAndPushWebsocketAndReturnDTO() {
        Notification saved = Notification.builder()
                .id("n-1")
                .recipientId(request.getRecipientId())
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .referenceId(request.getReferenceId())
                .referenceType(request.getReferenceType())
                .createdAt(LocalDateTime.now())
                .build();

        when(repository.save(any(Notification.class))).thenReturn(saved);

        var dto = service.create(request);

        verify(repository).save(notificationCaptor.capture());
        Notification captured = notificationCaptor.getValue();
        assertThat(captured.getRecipientId()).isEqualTo("user-1");
        verify(messagingTemplate).convertAndSend(eq("/topic/notifications/user-1"), any(Object.class));

        assertThat(dto.getId()).isEqualTo("n-1");
        assertThat(dto.getRecipientId()).isEqualTo("user-1");
    }

    @Test
    void getByUser_shouldReturnPageMapped() {
        Notification n1 = Notification.builder().id("n1").recipientId("user-1").createdAt(LocalDateTime.now()).build();
        when(repository.findByRecipientIdOrderByCreatedAtDesc(eq("user-1"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(n1)));

        var page = service.getByUser("user-1", 0, 10);
        assertThat(page).isNotNull();
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getUnreadByUser_shouldReturnList() {
        Notification n1 = Notification.builder().id("n1").recipientId("user-1").createdAt(LocalDateTime.now()).build();
        when(repository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc("user-1"))
                .thenReturn(List.of(n1));

        var list = service.getUnreadByUser("user-1");
        assertThat(list).hasSize(1);
    }

    @Test
    void countUnread_shouldReturnCount() {
        when(repository.countByRecipientIdAndReadFalse("user-1")).thenReturn(5L);
        assertThat(service.countUnread("user-1")).isEqualTo(5L);
    }

    @Test
    void markAsRead_shouldThrowOnMissing() {
        when(repository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.markAsRead("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Notification not found");
    }

    @Test
    void markAsRead_shouldMarkAndSave_whenUnread() {
        Notification n = Notification.builder().id("n2").recipientId("user-1").read(false).build();
        when(repository.findById("n2")).thenReturn(Optional.of(n));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        var dto = service.markAsRead("n2");

        verify(repository).save(any(Notification.class));
        assertThat(dto.isRead()).isTrue();
        assertThat(dto.getReadAt()).isNotNull();
    }

    @Test
    void markAllAsRead_shouldDelegateToRepository() {
        when(repository.markAllAsReadByRecipientId("user-1")).thenReturn(3);
        int marked = service.markAllAsRead("user-1");
        assertThat(marked).isEqualTo(3);
        verify(repository).markAllAsReadByRecipientId("user-1");
    }

    @Test
    void create_shouldHandleMessagingException_gracefully() {
        Notification saved = Notification.builder()
                .id("n-2")
                .recipientId(request.getRecipientId())
                .createdAt(LocalDateTime.now())
                .build();

        when(repository.save(any(Notification.class))).thenReturn(saved);
        doThrow(new RuntimeException("WS down")).when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        var dto = service.create(request);

        // ensure save was called and method returns DTO despite WS failure
        verify(repository).save(any(Notification.class));
        verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
        assertThat(dto.getId()).isEqualTo("n-2");
    }
}

