package com.smartfreelance.microservice.organizationservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RespondInvitationRequest {

    @NotNull
    private boolean accepted;
}
