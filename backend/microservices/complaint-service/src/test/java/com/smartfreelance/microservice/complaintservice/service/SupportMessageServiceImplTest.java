package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.entity.SupportMessage;
import com.smartfreelance.microservice.complaintservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.complaintservice.repository.ComplaintRepository;
import com.smartfreelance.microservice.complaintservice.repository.SupportMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SupportMessageServiceImplTest {

    @Mock private SupportMessageRepository messageRepository;
    @Mock private ComplaintRepository complaintRepository;

    @InjectMocks private SupportMessageServiceImpl messageService;

    private SupportMessage sampleMessage;

    @BeforeEach
    void setUp() {
        sampleMessage = new SupportMessage();
        sampleMessage.setId("msg1");
        sampleMessage.setComplaintId("comp123");
        sampleMessage.setSenderId("user1");
        sampleMessage.setContent("Hello Support");
    }

    @Test
    void createMessage_Success() {
        when(messageRepository.save(any())).thenReturn(sampleMessage);

        SupportMessage created = messageService.createMessage(sampleMessage);

        assertNotNull(created);
        assertEquals(SupportMessage.MessageType.TEXT, created.getMessageType());
        assertFalse(created.getIsRead());
        verify(messageRepository).save(any());
    }

    @Test
    void createMessage_ThrowsException_WhenInvalid() {
        sampleMessage.setContent("");
        assertThrows(IllegalArgumentException.class, () -> messageService.createMessage(sampleMessage));
    }

    @Test
    void createMessage_ShouldUpdateComplaint_WhenFirstSupportResponse() {
        sampleMessage.setSenderType(SupportMessage.SenderType.SUPPORT);
        Complaint complaint = new Complaint();
        complaint.setId("comp123");

        when(messageRepository.save(any())).thenReturn(sampleMessage);
        when(complaintRepository.findById("comp123")).thenReturn(Optional.of(complaint));

        messageService.createMessage(sampleMessage);

        assertNotNull(complaint.getFirstResponseAt());
        verify(complaintRepository).save(complaint);
    }

    @Test
    void getMessageById_Success() {
        when(messageRepository.findById("msg1")).thenReturn(Optional.of(sampleMessage));
        assertEquals(sampleMessage, messageService.getMessageById("msg1"));
    }

    @Test
    void getMessageById_NotFound() {
        when(messageRepository.findById("none")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> messageService.getMessageById("none"));
    }

    @Test
    void updateMessage_Success() {
        SupportMessage details = new SupportMessage();
        details.setContent("Updated Content");

        when(messageRepository.findById("msg1")).thenReturn(Optional.of(sampleMessage));
        when(messageRepository.save(any())).thenReturn(sampleMessage);

        SupportMessage updated = messageService.updateMessage("msg1", details);

        assertEquals("Updated Content", updated.getContent());
    }

    @Test
    void deleteMessage_Success() {
        when(messageRepository.findById("msg1")).thenReturn(Optional.of(sampleMessage));
        messageService.deleteMessage("msg1");
        verify(messageRepository).delete(sampleMessage);
    }

    @Test
    void markAsRead_Success() {
        when(messageRepository.findById("msg1")).thenReturn(Optional.of(sampleMessage));
        when(messageRepository.save(any())).thenReturn(sampleMessage);

        SupportMessage result = messageService.markAsRead("msg1");

        assertTrue(result.getIsRead());
        assertNotNull(result.getReadAt());
    }

    @Test
    void markAllAsRead_Success() {
        List<SupportMessage> unread = List.of(sampleMessage);
        when(messageRepository.findUnreadByComplaint("comp123")).thenReturn(unread);
        when(messageRepository.saveAll(any())).thenReturn(unread);

        List<SupportMessage> results = messageService.markAllAsRead("comp123");

        assertTrue(results.get(0).getIsRead());
        verify(messageRepository).saveAll(unread);
    }

    @Test
    void getLatestMessage_ThrowsException_IfEmpty() {
        when(messageRepository.findLatestByComplaint("comp123")).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () -> messageService.getLatestMessage("comp123"));
    }
}