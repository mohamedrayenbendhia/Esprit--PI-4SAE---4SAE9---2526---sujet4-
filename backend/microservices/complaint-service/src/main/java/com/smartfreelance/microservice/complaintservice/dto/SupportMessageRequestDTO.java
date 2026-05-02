package com.smartfreelance.microservice.complaintservice.dto;

import com.smartfreelance.microservice.complaintservice.entity.SupportMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportMessageRequestDTO {

    @NotBlank(message = "Complaint ID is required")
    @Size(max = 36)
    private String complaintId;

    // Injectés par le controller depuis X-User-Id / X-User-Role — jamais envoyés par le frontend
    private String senderId;
    private SupportMessage.SenderType senderType;

    private SupportMessage.MessageType messageType;

    /**
     * Espace de conversation cible.
     * Envoyé par le frontend selon l'onglet actif.
     * Défaut : COMPLAINANT.
     */
    private SupportMessage.ConversationType conversationType = SupportMessage.ConversationType.COMPLAINANT;

    @NotBlank(message = "Message content is required")
    @Size(min = 1, max = 5000)
    private String content;

    private List<String> attachments;
}