package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.entity.SupportMessage;
import com.smartfreelance.microservice.complaintservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.complaintservice.repository.ComplaintRepository;
import com.smartfreelance.microservice.complaintservice.repository.SupportMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SupportMessageServiceImpl implements SupportMessageService {

    private final SupportMessageRepository messageRepository;
    private final ComplaintRepository complaintRepository;

    @Override
    public SupportMessage createMessage(SupportMessage message) {
        log.info("Creating new support message for complaint: {}", message.getComplaintId());

        if (message.getComplaintId() == null || message.getComplaintId().isEmpty()) {
            throw new IllegalArgumentException("Complaint ID cannot be null or empty");
        }

        if (message.getSenderId() == null || message.getSenderId().isEmpty()) {
            throw new IllegalArgumentException("Sender ID cannot be null or empty");
        }

        if (message.getContent() == null || message.getContent().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be null or empty");
        }

        if (message.getMessageType() == null) {
            message.setMessageType(SupportMessage.MessageType.TEXT);
        }

        if (message.getIsRead() == null) {
            message.setIsRead(false);
        }

        SupportMessage savedMessage = messageRepository.save(message);
        log.info("Support message created successfully with id: {}", savedMessage.getId());

        // US6 — enregistrer firstResponseAt lors du premier message du support
        if (SupportMessage.SenderType.SUPPORT.equals(message.getSenderType())) {
            complaintRepository.findById(message.getComplaintId()).ifPresent(complaint -> {
                if (complaint.getFirstResponseAt() == null) {
                    complaint.setFirstResponseAt(LocalDateTime.now());
                    complaintRepository.save(complaint);
                    log.info("[firstResponseAt] Enregistré pour la réclamation {}", complaint.getId());
                }
            });
        }

        return savedMessage;
    }

    @Override
    @Transactional(readOnly = true)
    public SupportMessage getMessageById(String id) {
        log.debug("Fetching message with id: {}", id);
        return messageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportMessage> getAllMessages() {
        log.debug("Fetching all messages");
        return messageRepository.findAll();
    }

    @Override
    public SupportMessage updateMessage(String id, SupportMessage messageDetails) {
        log.info("Updating message with id: {}", id);

        SupportMessage existingMessage = getMessageById(id);

        if (messageDetails.getContent() != null) {
            existingMessage.setContent(messageDetails.getContent());
        }
        if (messageDetails.getAttachments() != null) {
            existingMessage.setAttachments(messageDetails.getAttachments());
        }

        SupportMessage updatedMessage = messageRepository.save(existingMessage);
        log.info("Message updated successfully: {}", updatedMessage.getId());

        return updatedMessage;
    }

    @Override
    public void deleteMessage(String id) {
        log.info("Deleting message with id: {}", id);
        SupportMessage message = getMessageById(id);
        messageRepository.delete(message);
        log.info("Message deleted successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportMessage> getMessagesByComplaint(String complaintId) {
        log.debug("Fetching messages for complaint: {}", complaintId);
        return messageRepository.findByComplaintIdOrderByCreatedAtAsc(complaintId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportMessage> getMessagesBySender(String senderId) {
        log.debug("Fetching messages from sender: {}", senderId);
        return messageRepository.findBySenderId(senderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportMessage> getUnreadMessages(String complaintId) {
        log.debug("Fetching unread messages for complaint: {}", complaintId);
        return messageRepository.findUnreadByComplaint(complaintId);
    }

    @Override
    @Transactional(readOnly = true)
    public SupportMessage getLatestMessage(String complaintId) {
        log.debug("Fetching latest message for complaint: {}", complaintId);
        SupportMessage latestMessage = messageRepository.findLatestByComplaint(complaintId);

        if (latestMessage == null) {
            throw new ResourceNotFoundException("No messages found for complaint: " + complaintId);
        }

        return latestMessage;
    }

    @Override
    public SupportMessage markAsRead(String messageId) {
        log.info("Marking message {} as read", messageId);

        SupportMessage message = getMessageById(messageId);
        message.setIsRead(true);
        message.setReadAt(LocalDateTime.now());

        SupportMessage updatedMessage = messageRepository.save(message);
        log.info("Message marked as read successfully");

        return updatedMessage;
    }

    @Override
    public List<SupportMessage> markAllAsRead(String complaintId) {
        log.info("Marking all messages as read for complaint: {}", complaintId);

        List<SupportMessage> unreadMessages = getUnreadMessages(complaintId);
        LocalDateTime now = LocalDateTime.now();

        unreadMessages.forEach(message -> {
            message.setIsRead(true);
            message.setReadAt(now);
        });

        List<SupportMessage> updatedMessages = messageRepository.saveAll(unreadMessages);
        log.info("Marked {} messages as read", updatedMessages.size());

        return updatedMessages;
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadMessages(String complaintId) {
        return messageRepository.countByComplaintIdAndIsReadFalse(complaintId);
    }
}