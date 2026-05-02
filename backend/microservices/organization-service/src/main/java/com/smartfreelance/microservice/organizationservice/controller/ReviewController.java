package com.smartfreelance.microservice.organizationservice.controller;

import com.smartfreelance.microservice.organizationservice.dto.request.CreateReviewRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.ReplyReviewRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.ReviewResponse;
import com.smartfreelance.microservice.organizationservice.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/organizations/{orgId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewResponse> create(@PathVariable String orgId,
                                                  @Valid @RequestBody CreateReviewRequest request,
                                                  Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(orgId, request, userId));
    }

    @GetMapping
    public ResponseEntity<Page<ReviewResponse>> list(@PathVariable String orgId, Pageable pageable) {
        return ResponseEntity.ok(reviewService.getByOrg(orgId, pageable));
    }

    @GetMapping("/eligibility")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Boolean>> canReview(@PathVariable String orgId,
                                                           Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.ok(Map.of("canReview", reviewService.canReview(orgId, userId)));
    }

    @PostMapping("/{reviewId}/reply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewResponse> reply(@PathVariable String orgId,
                                                 @PathVariable String reviewId,
                                                 @Valid @RequestBody ReplyReviewRequest request,
                                                 Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.ok(reviewService.reply(reviewId, request, userId));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable String orgId,
                                        @PathVariable String reviewId,
                                        Authentication auth) {
        String userId = (String) auth.getDetails();
        reviewService.delete(reviewId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{reviewId}/report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewResponse> report(@PathVariable String orgId,
                                                  @PathVariable String reviewId,
                                                  Authentication auth) {
        String userId = (String) auth.getDetails();
        return ResponseEntity.ok(reviewService.report(reviewId, userId));
    }
}
