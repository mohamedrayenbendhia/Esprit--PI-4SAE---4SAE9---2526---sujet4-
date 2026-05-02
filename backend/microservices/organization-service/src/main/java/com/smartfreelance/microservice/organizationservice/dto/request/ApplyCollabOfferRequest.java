package com.smartfreelance.microservice.organizationservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApplyCollabOfferRequest {

    @NotBlank(message = "Le message de motivation est requis")
    @Size(max = 2000)
    private String message;

    @Size(max = 512)
    private String portfolioUrl;
}
