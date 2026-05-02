package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.CreateReviewRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.ReplyReviewRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {
    ReviewResponse create(String orgId, CreateReviewRequest request, String reviewerId);
    boolean canReview(String orgId, String reviewerId);
    Page<ReviewResponse> getByOrg(String orgId, Pageable pageable);
    ReviewResponse reply(String reviewId, ReplyReviewRequest request, String ownerId);
    void delete(String reviewId, String userId);
    ReviewResponse report(String reviewId, String reporterId);
}
