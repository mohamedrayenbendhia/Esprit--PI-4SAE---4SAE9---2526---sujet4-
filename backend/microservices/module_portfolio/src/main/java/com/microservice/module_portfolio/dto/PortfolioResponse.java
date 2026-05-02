package com.microservice.module_portfolio.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PortfolioResponse {
    private Long id;
    private String userId; // ✅ UUID en String
    private String headline;
    private String linkedinUrl;
    private String githubUrl;
    private String location;
    private List<ProjectResponse> projects;
    @JsonProperty("isPublic")
    private boolean isPublic;
    private int viewsCount;
}