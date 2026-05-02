package com.smartfreelance.microservice.organizationservice.dto.request;

import com.smartfreelance.microservice.organizationservice.enums.OrganizationSize;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationType;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrganizationRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 5000)
    private String description;

    private String logoUrl;
    private String website;

    @NotNull
    private OrganizationType type;

    private List<String> specialties;
    private String location;
    private String siret;
    private OrganizationSize size;
    private OrganizationVisibility visibility;
}
