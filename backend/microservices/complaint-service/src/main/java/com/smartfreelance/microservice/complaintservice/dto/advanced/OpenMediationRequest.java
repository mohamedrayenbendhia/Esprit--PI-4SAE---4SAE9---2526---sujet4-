package com.smartfreelance.microservice.complaintservice.dto.advanced;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class OpenMediationRequest {
    @Min(24)
    private int evidenceWindowHours = 72;
    @Min(24)
    private int decisionWindowHours = 48;
}
