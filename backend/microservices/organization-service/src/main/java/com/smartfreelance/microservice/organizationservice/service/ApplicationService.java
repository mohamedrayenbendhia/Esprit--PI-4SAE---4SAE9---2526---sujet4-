package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.CreateApplicationRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.RespondApplicationRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.ApplicationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ApplicationService {
    ApplicationResponse apply(String orgId, CreateApplicationRequest request, String applicantId);
    ApplicationResponse respond(String applicationId, RespondApplicationRequest request, String responderId);
    Page<ApplicationResponse> getOrgApplications(String orgId, Pageable pageable);
    void withdraw(String applicationId, String applicantId);
    List<ApplicationResponse> getMyApplications(String applicantId);
}
