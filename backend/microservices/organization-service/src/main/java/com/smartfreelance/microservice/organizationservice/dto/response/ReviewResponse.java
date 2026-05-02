package com.smartfreelance.microservice.organizationservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponse {
    private String id;
    private String organizationId;
    private String reviewerId;
    private String projectId;
    private int rating;
    private String comment;
    private String reply;
    private LocalDateTime replyAt;
    private boolean reported;
    private LocalDateTime createdAt;
}
