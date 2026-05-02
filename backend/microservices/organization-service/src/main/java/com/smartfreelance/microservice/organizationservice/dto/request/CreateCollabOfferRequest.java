package com.smartfreelance.microservice.organizationservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateCollabOfferRequest {

    @NotBlank(message = "Le titre est requis")
    @Size(max = 150)
    private String title;

    @NotBlank(message = "La description est requise")
    private String description;

    private List<String> requiredSkills;

    @Size(max = 100)
    private String durationLabel;

    @Min(0)
    private Double budgetEstimate;

    @Min(1)
    private Integer maxApplicants;

    private LocalDate deadlineDate;
}
