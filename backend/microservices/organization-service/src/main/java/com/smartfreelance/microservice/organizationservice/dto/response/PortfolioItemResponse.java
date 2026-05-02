package com.smartfreelance.microservice.organizationservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PortfolioItemResponse {
    private String id;
    private String organizationId;
    private String title;
    private String description;
    private String imageUrl;
    private String projectUrl;
    private String clientName;
    private List<String> tags;
    private LocalDate completedAt;
    private LocalDateTime createdAt;
}
