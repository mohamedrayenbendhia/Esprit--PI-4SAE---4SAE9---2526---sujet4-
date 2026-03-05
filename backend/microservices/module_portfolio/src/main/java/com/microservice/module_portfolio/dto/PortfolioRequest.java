package com.microservice.module_portfolio.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PortfolioRequest {

    private String userId; // ✅ sera set par JWT, plus @NotNull

    @NotBlank(message = "Headline is required")
    private String headline;

    private String linkedinUrl;
    private String githubUrl;
    private String location;
    private List<ProjectRequest> projects;
    @Builder.Default
    private boolean isPublic = true;
}