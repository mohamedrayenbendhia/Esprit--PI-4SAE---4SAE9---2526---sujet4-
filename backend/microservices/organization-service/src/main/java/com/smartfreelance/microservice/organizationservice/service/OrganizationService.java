package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.CreateOrganizationRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.UpdateOrganizationRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.OrganizationResponse;
import com.smartfreelance.microservice.organizationservice.dto.response.OrganizationSummaryResponse;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationSize;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationType;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrganizationService {
    OrganizationResponse create(CreateOrganizationRequest request, String ownerId);
    boolean isNameAvailable(String name);
    OrganizationResponse getById(String id);
    OrganizationResponse update(String id, UpdateOrganizationRequest request, String userId);
    void delete(String id, String userId);
    List<OrganizationSummaryResponse> getMyOrganizations(String userId);
    Page<OrganizationSummaryResponse> searchPublic(String keyword, OrganizationType type, OrganizationSize size, Pageable pageable);
    void transferOwnership(String orgId, String currentOwnerId, String newOwnerId);
    void dissolve(String orgId, String ownerId);
    OrganizationResponse setVisibility(String id, OrganizationVisibility visibility, String userId);
}
