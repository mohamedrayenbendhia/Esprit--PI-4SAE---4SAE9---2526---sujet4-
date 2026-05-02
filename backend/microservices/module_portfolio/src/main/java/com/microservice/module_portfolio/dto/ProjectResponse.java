package com.microservice.module_portfolio.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProjectResponse {
    private Long id;
    private String title;
    private String description;
    private String techStack;
    private LocalDate startDate;
    private LocalDate endDate;

    // ❌ Supprimé : private String projectUrl;
    // ✅ Remplacé par :
    private String githubUrl;
    private String demoUrl;
    private String images;
}