package com.smartfreelance.microservice.organizationservice.dto.response;

import com.smartfreelance.microservice.organizationservice.enums.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrganizationResponse {
    private String id;
    private String name;
    private String description;
    private String logoUrl;
    private String website;
    private OrganizationType type;
    private List<String> specialties;
    private String location;
    private Double latitude;
    private Double longitude;
    private String siret;
    private OrganizationSize size;
    private OrganizationStatus status;
    private OrganizationVisibility visibility;
    private String ownerId;
    private Double averageRating;
    private Integer completedProjectsCount;
    private Integer reviewCount;
    private Integer trustLevel;
    /** Visible uniquement des endpoints admin — null sur les routes publiques */
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
