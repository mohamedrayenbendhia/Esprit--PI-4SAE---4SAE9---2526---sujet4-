package com.smartfreelance.microservice.organizationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfreelance.microservice.organizationservice.dto.request.CreateReviewRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.ReplyReviewRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.ReviewResponse;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.GlobalExceptionHandler;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewController Unit Tests")
class ReviewControllerTest {

    @Mock private ReviewService reviewService;
    @InjectMocks private ReviewController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String USER_ID = "user-1";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Authentication mockAuth() {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(USER_ID);
        return auth;
    }

    private ReviewResponse buildReviewResponse(String id) {
        return ReviewResponse.builder()
                .id(id)
                .organizationId("org-1")
                .reviewerId(USER_ID)
                .rating(5)
                .comment("Excellent!")
                .reported(false)
                .build();
    }

    // ── POST /api/organizations/{orgId}/reviews ───────────────────────────────

    @Test
    @DisplayName("create_validRequest_returns201")
    void create_validRequest_returns201() throws Exception {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setRating(5);
        req.setComment("Great org");
        req.setProjectId("proj-1");

        ReviewResponse response = buildReviewResponse("rev-1");
        when(reviewService.create(eq("org-1"), any(CreateReviewRequest.class), eq(USER_ID)))
                .thenReturn(response);

        mockMvc.perform(post("/api/organizations/org-1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("rev-1"))
                .andExpect(jsonPath("$.reviewerId").value(USER_ID));
    }

    @Test
    @DisplayName("create_duplicateReview_returns422")
    void create_duplicateReview_returns422() throws Exception {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setRating(4);
        req.setComment("Nice");
        req.setProjectId("proj-1");

        when(reviewService.create(eq("org-1"), any(), eq(USER_ID)))
                .thenThrow(new BusinessRuleException("You have already reviewed this organization."));

        mockMvc.perform(post("/api/organizations/org-1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("You have already reviewed this organization."));
    }

    @Test
    @DisplayName("create_noInteraction_returns422")
    void create_noInteraction_returns422() throws Exception {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setRating(3);
        req.setComment("Meh");
        req.setProjectId("proj-1");

        when(reviewService.create(eq("org-1"), any(), eq(USER_ID)))
                .thenThrow(new BusinessRuleException("You have not interacted with this organization."));

        mockMvc.perform(post("/api/organizations/org-1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── GET /api/organizations/{orgId}/reviews ────────────────────────────────

    @Test
    @DisplayName("list_existingOrg_returnsPage")
    void list_existingOrg_returnsPage() throws Exception {
        ReviewResponse r = buildReviewResponse("rev-1");
        when(reviewService.getByOrg(eq("org-1"), any()))
                .thenReturn(new PageImpl<>(List.of(r), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/organizations/org-1/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value("rev-1"));
    }

    @Test
    @DisplayName("list_emptyOrg_returnsEmptyPage")
    void list_emptyOrg_returnsEmptyPage() throws Exception {
        when(reviewService.getByOrg(eq("org-1"), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/organizations/org-1/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("list_secondPage_returnsCorrectPage")
    void list_secondPage_returnsCorrectPage() throws Exception {
        ReviewResponse r = buildReviewResponse("rev-2");
        when(reviewService.getByOrg(eq("org-1"), any()))
                .thenReturn(new PageImpl<>(List.of(r), PageRequest.of(1, 10), 11));

        mockMvc.perform(get("/api/organizations/org-1/reviews")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("rev-2"));
    }

    // ── POST /api/organizations/{orgId}/reviews/{reviewId}/reply ─────────────

    @Test
    @DisplayName("reply_ownerReplies_returns200")
    void reply_ownerReplies_returns200() throws Exception {
        ReplyReviewRequest req = new ReplyReviewRequest();
        req.setReply("Thank you for your feedback!");

        ReviewResponse response = ReviewResponse.builder()
                .id("rev-1").reply("Thank you for your feedback!").build();
        when(reviewService.reply(eq("rev-1"), any(ReplyReviewRequest.class), eq(USER_ID)))
                .thenReturn(response);

        mockMvc.perform(post("/api/organizations/org-1/reviews/rev-1/reply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Thank you for your feedback!"));
    }

    @Test
    @DisplayName("reply_notOwner_returns422")
    void reply_notOwner_returns422() throws Exception {
        ReplyReviewRequest req = new ReplyReviewRequest();
        req.setReply("reply");

        when(reviewService.reply(eq("rev-1"), any(), eq(USER_ID)))
                .thenThrow(new BusinessRuleException("Only the organization owner can reply to reviews."));

        mockMvc.perform(post("/api/organizations/org-1/reviews/rev-1/reply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("reply_reviewNotFound_returns404")
    void reply_reviewNotFound_returns404() throws Exception {
        ReplyReviewRequest req = new ReplyReviewRequest();
        req.setReply("reply");

        when(reviewService.reply(eq("missing"), any(), eq(USER_ID)))
                .thenThrow(new ResourceNotFoundException("Review not found"));

        mockMvc.perform(post("/api/organizations/org-1/reviews/missing/reply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/organizations/{orgId}/reviews/{reviewId} ─────────────────

    @Test
    @DisplayName("delete_byReviewer_returns204")
    void delete_byReviewer_returns204() throws Exception {
        doNothing().when(reviewService).delete("rev-1", USER_ID);

        mockMvc.perform(delete("/api/organizations/org-1/reviews/rev-1")
                        .principal(mockAuth()))
                .andExpect(status().isNoContent());

        verify(reviewService).delete("rev-1", USER_ID);
    }

    @Test
    @DisplayName("delete_notOwnerOrReviewer_returns422")
    void delete_notOwnerOrReviewer_returns422() throws Exception {
        doThrow(new BusinessRuleException("You can only delete your own reviews."))
                .when(reviewService).delete("rev-1", USER_ID);

        mockMvc.perform(delete("/api/organizations/org-1/reviews/rev-1")
                        .principal(mockAuth()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("delete_reviewNotFound_returns404")
    void delete_reviewNotFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Review not found"))
                .when(reviewService).delete("missing", USER_ID);

        mockMvc.perform(delete("/api/organizations/org-1/reviews/missing")
                        .principal(mockAuth()))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/organizations/{orgId}/reviews/{reviewId}/report ────────────

    @Test
    @DisplayName("report_success_returns200")
    void report_success_returns200() throws Exception {
        ReviewResponse response = ReviewResponse.builder()
                .id("rev-1").reported(true).build();
        when(reviewService.report("rev-1", USER_ID)).thenReturn(response);

        mockMvc.perform(post("/api/organizations/org-1/reviews/rev-1/report")
                        .principal(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reported").value(true));
    }

    @Test
    @DisplayName("report_reviewNotFound_returns404")
    void report_reviewNotFound_returns404() throws Exception {
        when(reviewService.report("missing", USER_ID))
                .thenThrow(new ResourceNotFoundException("Review not found"));

        mockMvc.perform(post("/api/organizations/org-1/reviews/missing/report")
                        .principal(mockAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("report_alreadyReported_returns422")
    void report_alreadyReported_returns422() throws Exception {
        when(reviewService.report("rev-1", USER_ID))
                .thenThrow(new BusinessRuleException("This review has already been reported."));

        mockMvc.perform(post("/api/organizations/org-1/reviews/rev-1/report")
                        .principal(mockAuth()))
                .andExpect(status().isUnprocessableEntity());
    }
}
