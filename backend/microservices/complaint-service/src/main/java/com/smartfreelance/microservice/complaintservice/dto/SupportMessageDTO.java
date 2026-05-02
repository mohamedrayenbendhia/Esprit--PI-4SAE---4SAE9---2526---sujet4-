package com.smartfreelance.microservice.complaintservice.dto;

import com.smartfreelance.microservice.complaintservice.entity.SupportMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportMessageDTO {

    private String id;
    private String complaintId;
    private String senderId;
    private SupportMessage.SenderType senderType;
    private SupportMessage.MessageType messageType;
    private SupportMessage.ConversationType conversationType;
    private String content;
    private List<String> attachments;
    private Boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}