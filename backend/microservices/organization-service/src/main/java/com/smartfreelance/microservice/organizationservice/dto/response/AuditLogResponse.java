package com.smartfreelance.microservice.organizationservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogResponse {
    private String id;
    private String organizationId;
    private String performedByUserId;
    private String action;
    private String details;
    private LocalDateTime createdAt;
}
