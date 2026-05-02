package com.smartfreelance.microservice.organizationservice.dto.response;

import com.smartfreelance.microservice.organizationservice.enums.TrustBadge;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BadgeResponse {
    private String organizationId;
    private List<TrustBadge> badges;
}
