package com.smartfreelance.microservice.organizationservice.dto.response;

import com.smartfreelance.microservice.organizationservice.enums.InvitationStatus;
import com.smartfreelance.microservice.organizationservice.enums.MemberRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InvitationResponse {
    private String id;
    private String organizationId;
    private String inviterId;
    private String inviteeId;
    private String inviteeEmail;
    private MemberRole role;
    private InvitationStatus status;
    private String message;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
}
