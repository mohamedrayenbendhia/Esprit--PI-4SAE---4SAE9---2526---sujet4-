package com.smartfreelance.microservice.organizationservice.dto.request;

import com.smartfreelance.microservice.organizationservice.enums.MemberRole;
import lombok.Data;

@Data
public class InviteMemberRequest {

    private String inviteeId;
    private String inviteeEmail;
    private MemberRole role;
    private String message;
}
