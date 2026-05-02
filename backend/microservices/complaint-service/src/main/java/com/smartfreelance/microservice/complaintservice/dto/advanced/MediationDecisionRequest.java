package com.smartfreelance.microservice.complaintservice.dto.advanced;

import com.smartfreelance.microservice.complaintservice.entity.MediationSession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MediationDecisionRequest {
    @NotNull
    private MediationSession.MediationOutcome outcome;
    @NotBlank
    private String reasoning;
    private String targetUserId;
    private UserSanction.SanctionType sanction;

    public static class UserSanction {
        public enum SanctionType { WARNING, TEMP_SUSPENSION, PERMANENT_SUSPENSION }
    }
}
