package com.smartfreelance.microservice.organizationservice.dto.request;

import com.smartfreelance.microservice.organizationservice.enums.CollabApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RespondCollabApplicationRequest {

    @NotNull(message = "Le statut de réponse est requis")
    private CollabApplicationStatus status; // ACCEPTED ou REJECTED

    private String rejectionReason;
}
