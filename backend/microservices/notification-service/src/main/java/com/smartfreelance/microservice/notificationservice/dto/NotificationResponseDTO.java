package com.smartfreelance.microservice.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {
    private String id;
    private String recipientId;
    private String type;
    private String title;
    private String message;
    private String referenceId;
    private String referenceType;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
