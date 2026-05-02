package com.smartfreelance.microservice.organizationservice.dto.response;

import com.smartfreelance.microservice.organizationservice.enums.MemberRole;
import com.smartfreelance.microservice.organizationservice.enums.MemberStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MemberResponse {
    private String id;
    private String organizationId;
    private String userId;
    private MemberRole role;
    private MemberStatus status;
    private boolean displayOnProfile;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
}
