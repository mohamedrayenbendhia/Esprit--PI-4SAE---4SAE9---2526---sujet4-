package com.smartfreelance.microservice.complaintservice.dto.advanced;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReopenComplaintRequest {
    @NotBlank
    private String reason;
}
