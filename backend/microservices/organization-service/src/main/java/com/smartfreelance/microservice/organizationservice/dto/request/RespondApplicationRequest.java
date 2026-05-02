package com.smartfreelance.microservice.organizationservice.dto.request;

import com.smartfreelance.microservice.organizationservice.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RespondApplicationRequest {

    @NotNull
    private ApplicationStatus status;

    private String rejectionReason;
}
