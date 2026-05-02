package com.smartfreelance.microservice.complaintservice.dto.advanced;

import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CreateSlaRuleRequest {
    @NotNull
    private Complaint.Priority priority;
    @Min(1)
    private int maxFirstResponseHours;
    @Min(1)
    private int maxResolutionHours;
    @Min(1)
    private int warningThresholdHours;
    private boolean autoEscalate;
}
