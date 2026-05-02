package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.AdminSuspendRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.AdminVerifyRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.OrganizationDashboardStats;
import com.smartfreelance.microservice.organizationservice.dto.response.OrganizationResponse;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminOrganizationService {
    Page<OrganizationResponse> listAll(OrganizationStatus status, Pageable pageable);
    OrganizationResponse verify(String orgId, AdminVerifyRequest request, String adminId);
    OrganizationResponse suspend(String orgId, AdminSuspendRequest request, String adminId);
    OrganizationResponse reactivate(String orgId, String adminId);
    OrganizationDashboardStats getDashboardStats();
}
