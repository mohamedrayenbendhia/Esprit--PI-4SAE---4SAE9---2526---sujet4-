package com.smartfreelance.microservice.organizationservice.dto.response;

import com.smartfreelance.microservice.organizationservice.enums.CollabApplicationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CollabApplicationResponse {

    private String id;
    private String offerId;
    private String organizationId;
    private String applicantId;
    private String message;
    private String portfolioUrl;
    private CollabApplicationStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    // Infos de l'offre dénormalisées (utiles dans la vue "mes candidatures")
    private String offerTitle;
}
