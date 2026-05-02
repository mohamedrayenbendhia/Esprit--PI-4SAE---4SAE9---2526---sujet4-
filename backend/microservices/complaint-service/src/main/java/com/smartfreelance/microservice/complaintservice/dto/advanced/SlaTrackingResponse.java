package com.smartfreelance.microservice.complaintservice.dto.advanced;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SlaTrackingResponse {
    private String id;
    private String complaintId;
    private LocalDateTime firstResponseDeadline;
    private LocalDateTime resolutionDeadline;
    private boolean firstResponseBreached;
    private boolean resolutionBreached;
    private LocalDateTime firstResponseAt;
    private LocalDateTime resolvedAt;
    private double resolutionProgressPercent;
}
