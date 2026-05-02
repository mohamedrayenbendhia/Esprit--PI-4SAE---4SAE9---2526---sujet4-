package com.smartfreelance.microservice.organizationservice.dto.request;

import com.smartfreelance.microservice.organizationservice.enums.OrganizationSize;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationVisibility;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateOrganizationRequest {

    @Size(min = 1, max = 100, message = "Name cannot be empty if provided")
    private String name;

    @Size(max = 5000)
    private String description;

    private String logoUrl;
    private String website;
    private List<String> specialties;
    private String location;
    private String siret;
    private OrganizationSize size;
    private OrganizationVisibility visibility;
}
