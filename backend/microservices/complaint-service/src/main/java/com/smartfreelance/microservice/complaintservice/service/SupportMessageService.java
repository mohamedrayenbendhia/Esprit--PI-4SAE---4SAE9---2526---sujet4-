package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.entity.SupportMessage;

import java.util.List;

public interface SupportMessageService {
    SupportMessage createMessage(SupportMessage message);
    SupportMessage getMessageById(String id);
    List<SupportMessage> getAllMessages();
    SupportMessage updateMessage(String id, SupportMessage message);
    void deleteMessage(String id);
    List<SupportMessage> getMessagesByComplaint(String complaintId);
    List<SupportMessage> getMessagesBySender(String senderId);
    List<SupportMessage> getUnreadMessages(String complaintId);
    SupportMessage getLatestMessage(String complaintId);
    SupportMessage markAsRead(String messageId);
    List<SupportMessage> markAllAsRead(String complaintId);
    long countUnreadMessages(String complaintId);
}
