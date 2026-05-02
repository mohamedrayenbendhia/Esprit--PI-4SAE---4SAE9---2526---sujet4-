package com.smartfreelance.microservice.organizationservice.dto.request;

import com.smartfreelance.microservice.organizationservice.enums.OrganizationSize;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationType;
import lombok.Data;

import java.util.List;

@Data
public class MatchingRequest {
    private List<String> requiredSkills;
    private OrganizationType preferredType;
    private OrganizationSize preferredSize;
    private String location;
}
