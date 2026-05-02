package com.smartfreelance.microservice.organizationservice.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO envoyé au notification-service via Feign.
 * Correspond à NotificationRequestDTO du notification-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private String recipientId;
    private String type;
    private String title;
    private String message;
    private String referenceId;
    private String referenceType;
}
