package com.microservice.module_portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class PortfolioUpdateRequest {

    @NotBlank(message = "Headline is required")
    private String headline;

    private String linkedinUrl;
    private String githubUrl;
    private String location;
    private List<ProjectRequest> projects;
    private boolean isPublic;
}