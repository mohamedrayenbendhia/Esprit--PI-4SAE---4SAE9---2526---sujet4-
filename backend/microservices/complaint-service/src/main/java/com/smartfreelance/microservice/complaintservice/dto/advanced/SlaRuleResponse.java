package com.smartfreelance.microservice.complaintservice.dto.advanced;

import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class SlaRuleResponse {
    private String id;
    private Complaint.Priority priority;
    private int maxFirstResponseHours;
    private int maxResolutionHours;
    private int warningThresholdHours;
    private boolean autoEscalate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
