package com.smartfreelance.microservice.organizationservice.dto.response;

import com.smartfreelance.microservice.organizationservice.enums.CollabOfferStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CollabOfferResponse {

    private String id;
    private String organizationId;
    private String createdBy;
    private String title;
    private String description;
    private List<String> requiredSkills;
    private String durationLabel;
    private Double budgetEstimate;
    private Integer maxApplicants;
    private LocalDate deadlineDate;
    private CollabOfferStatus status;
    private long applicationCount; // nombre total de candidatures
    private long acceptedCount;    // nombre de candidatures acceptées
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
