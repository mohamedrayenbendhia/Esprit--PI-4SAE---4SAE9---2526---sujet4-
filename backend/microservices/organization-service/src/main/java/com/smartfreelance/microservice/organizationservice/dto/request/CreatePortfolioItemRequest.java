package com.smartfreelance.microservice.organizationservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreatePortfolioItemRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    private String description;
    private String imageUrl;
    private String projectUrl;
    private String clientName;
    private List<String> tags;
    private LocalDate completedAt;
}
