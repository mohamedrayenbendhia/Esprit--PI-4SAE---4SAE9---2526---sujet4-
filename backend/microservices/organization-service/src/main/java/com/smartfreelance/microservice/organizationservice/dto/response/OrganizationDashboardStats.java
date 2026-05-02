package com.smartfreelance.microservice.organizationservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrganizationDashboardStats {
    private long totalOrganizations;
    private long activeOrganizations;
    private long pendingVerification;
    private long suspended;
    private long dissolved;
}
