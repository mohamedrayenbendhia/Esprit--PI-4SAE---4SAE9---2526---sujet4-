package com.smartfreelance.microservice.complaintservice.mapper;

import com.smartfreelance.microservice.complaintservice.dto.SupportMessageDTO;
import com.smartfreelance.microservice.complaintservice.dto.SupportMessageRequestDTO;
import com.smartfreelance.microservice.complaintservice.entity.SupportMessage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SupportMessageMapper {

    public SupportMessageDTO toDTO(SupportMessage message) {
        if (message == null) return null;

        return SupportMessageDTO.builder()
                .id(message.getId())
                .complaintId(message.getComplaintId())
                .senderId(message.getSenderId())
                .senderType(message.getSenderType())
                .messageType(message.getMessageType())
                .conversationType(message.getConversationType())
                .content(message.getContent())
                .attachments(message.getAttachments())
                .isRead(message.getIsRead())
                .readAt(message.getReadAt())
                .createdAt(message.getCreatedAt())
                .build();
    }

    public List<SupportMessageDTO> toDTOList(List<SupportMessage> messages) {
        if (messages == null) return null;
        return messages.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public SupportMessage toEntity(SupportMessageRequestDTO dto) {
        if (dto == null) return null;

        SupportMessage message = new SupportMessage();
        message.setComplaintId(dto.getComplaintId());
        message.setSenderId(dto.getSenderId());
        message.setSenderType(dto.getSenderType());
        message.setMessageType(dto.getMessageType());
        message.setConversationType(
                dto.getConversationType() != null
                        ? dto.getConversationType()
                        : SupportMessage.ConversationType.COMPLAINANT
        );
        message.setContent(dto.getContent());
        message.setAttachments(dto.getAttachments());
        return message;
    }
}