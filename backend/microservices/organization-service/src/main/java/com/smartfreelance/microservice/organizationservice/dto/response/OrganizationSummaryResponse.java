package com.smartfreelance.microservice.organizationservice.dto.response;

import com.smartfreelance.microservice.organizationservice.enums.MemberRole;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationSize;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationStatus;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrganizationSummaryResponse {
    private String id;
    private String name;
    private String logoUrl;
    private OrganizationType type;
    private OrganizationStatus status;
    private String location;
    private Double averageRating;
    private Integer memberCount;
    private OrganizationSize size;
    private Integer reviewCount;

    /**
     * Rôle de l'utilisateur courant dans cette organisation.
     * Null si l'endpoint est public (recherche, profil) ou si l'utilisateur n'est pas membre.
     * Populé uniquement par GET /api/organizations/my.
     */
    private MemberRole myRole;
}
