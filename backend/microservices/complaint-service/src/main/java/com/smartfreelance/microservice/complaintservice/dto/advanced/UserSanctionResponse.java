package com.smartfreelance.microservice.complaintservice.dto.advanced;

import com.smartfreelance.microservice.complaintservice.entity.UserSanction;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class UserSanctionResponse {
    private String id;
    private String userId;
    private UserSanction.SanctionType type;
    private String reason;
    private String triggerComplaintId;
    private boolean active;
    private LocalDateTime expiresAt;
    private LocalDateTime appliedAt;
    private boolean appliedBySystem;
}
