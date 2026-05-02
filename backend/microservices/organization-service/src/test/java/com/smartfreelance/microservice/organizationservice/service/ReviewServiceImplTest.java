package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.CreateReviewRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.ReplyReviewRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.ReviewResponse;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.entity.OrganizationReview;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewServiceImpl Unit Tests")
class ReviewServiceImplTest {

    /* ── Mocks ───────────────────────────────────────────────────────────── */

    @Mock private OrganizationReviewRepository reviewRepo;
    @Mock private OrganizationRepository        orgRepo;
    @Mock private TrustScoreService             trustScoreService;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    /* ── Helpers ─────────────────────────────────────────────────────────── */

    private Organization buildOrg(String orgId, String ownerId) {
        return Organization.builder()
                .id(orgId)
                .name("Acme Corp")
                .ownerId(ownerId)
                .build();
    }

    private OrganizationReview buildReview(String id, String orgId, String reviewerId, int rating) {
        OrganizationReview r = new OrganizationReview();
        r.setId(id);
        r.setOrganizationId(orgId);
        r.setReviewerId(reviewerId);
        r.setRating(rating);
        r.setComment("Great organization!");
        r.setReported(false);
        return r;
    }

    private CreateReviewRequest buildCreateRequest(int rating) {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setRating(rating);
        req.setComment("Great organization!");
        req.setProjectId("project-1");
        return req;
    }

    /* ── create() ─────────────────────────────────────────────────────────── */

    @Test
    @DisplayName("create_newReview_savesAndRecomputesTrustScore")
    void create_newReview_savesAndRecomputesTrustScore() {
        String orgId = "org-1";
        String reviewerId = "user-1";
        CreateReviewRequest req = buildCreateRequest(5);

        // No bombing attack (only 1 bad review in last 24h)
        when(reviewRepo.countByOrganizationIdAndRatingLessThanEqualAndCreatedAtAfter(
                eq(orgId), eq(2), any(LocalDateTime.class))).thenReturn(1L);

        OrganizationReview savedReview = buildReview("rev-1", orgId, reviewerId, 5);
        when(reviewRepo.save(any(OrganizationReview.class))).thenReturn(savedReview);

        ReviewResponse response = reviewService.create(orgId, req, reviewerId);

        assertThat(response).isNotNull();
        assertThat(response.getReviewerId()).isEqualTo(reviewerId);
        assertThat(response.getOrganizationId()).isEqualTo(orgId);

        // Verify trust score was recomputed
        verify(trustScoreService, times(1)).recompute(orgId);

        // Verify save was called
        ArgumentCaptor<OrganizationReview> captor = ArgumentCaptor.forClass(OrganizationReview.class);
        verify(reviewRepo, atLeastOnce()).save(captor.capture());
        OrganizationReview captured = captor.getAllValues().get(0);
        assertThat(captured.getRating()).isEqualTo(5);
        assertThat(captured.getReviewerId()).isEqualTo(reviewerId);
    }

    @Test
    @DisplayName("create_duplicateReview_allowsAnotherReview")
    void create_duplicateReview_allowsAnotherReview() {
        String orgId = "org-1";
        String reviewerId = "user-1";

        when(reviewRepo.countByOrganizationIdAndRatingLessThanEqualAndCreatedAtAfter(
                eq(orgId), eq(2), any(LocalDateTime.class))).thenReturn(0L);

        OrganizationReview saved = buildReview("rev-duplicate", orgId, reviewerId, 4);
        when(reviewRepo.save(any())).thenReturn(saved);

        ReviewResponse response = reviewService.create(orgId, buildCreateRequest(4), reviewerId);

        assertThat(response).isNotNull();
        assertThat(response.getReviewerId()).isEqualTo(reviewerId);
        verify(trustScoreService).recompute(orgId);
    }

    @Test
    @DisplayName("create_withoutPriorInteraction_allowsReview")
    void create_withoutPriorInteraction_allowsReview() {
        String orgId = "org-1";
        String reviewerId = "user-no-interaction";

        when(reviewRepo.countByOrganizationIdAndRatingLessThanEqualAndCreatedAtAfter(
                eq(orgId), eq(2), any(LocalDateTime.class))).thenReturn(0L);

        OrganizationReview saved = buildReview("rev-no-interaction", orgId, reviewerId, 3);
        when(reviewRepo.save(any())).thenReturn(saved);

        ReviewResponse response = reviewService.create(orgId, buildCreateRequest(3), reviewerId);

        assertThat(response).isNotNull();
        assertThat(response.getReviewerId()).isEqualTo(reviewerId);
        verify(trustScoreService).recompute(orgId);
    }

    @Test
    @DisplayName("create_interactionViaRfq_allowsReview")
    void create_interactionViaRfq_allowsReview() {
        String orgId = "org-1";
        String reviewerId = "user-rfq";

        // No bombing
        when(reviewRepo.countByOrganizationIdAndRatingLessThanEqualAndCreatedAtAfter(
                eq(orgId), eq(2), any(LocalDateTime.class))).thenReturn(0L);

        OrganizationReview saved = buildReview("rev-2", orgId, reviewerId, 4);
        when(reviewRepo.save(any())).thenReturn(saved);

        ReviewResponse response = reviewService.create(orgId, buildCreateRequest(4), reviewerId);

        assertThat(response).isNotNull();
        verify(trustScoreService).recompute(orgId);
    }

    @Test
    @DisplayName("create_interactionViaCollabApp_allowsReview")
    void create_interactionViaCollabApp_allowsReview() {
        String orgId = "org-1";
        String reviewerId = "user-collab";

        // No bombing
        when(reviewRepo.countByOrganizationIdAndRatingLessThanEqualAndCreatedAtAfter(
                eq(orgId), eq(2), any(LocalDateTime.class))).thenReturn(0L);

        OrganizationReview saved = buildReview("rev-3", orgId, reviewerId, 3);
        when(reviewRepo.save(any())).thenReturn(saved);

        ReviewResponse response = reviewService.create(orgId, buildCreateRequest(3), reviewerId);

        assertThat(response).isNotNull();
        verify(trustScoreService).recompute(orgId);
    }

    @Test
    @DisplayName("create_reviewBombing_autoReportsReview")
    void create_reviewBombing_autoReportsReview() {
        String orgId = "org-bomb";
        String reviewerId = "bomber-user";

        // Bombing threshold exceeded (>= 4 low-rating reviews in 24h)
        when(reviewRepo.countByOrganizationIdAndRatingLessThanEqualAndCreatedAtAfter(
                eq(orgId), eq(2), any(LocalDateTime.class))).thenReturn(4L);

        // First save returns review, second save with reported=true
        OrganizationReview firstSave = buildReview("rev-bomb", orgId, reviewerId, 1);
        firstSave.setReported(false);
        OrganizationReview secondSave = buildReview("rev-bomb", orgId, reviewerId, 1);
        secondSave.setReported(true);

        when(reviewRepo.save(any(OrganizationReview.class)))
                .thenReturn(firstSave)
                .thenReturn(secondSave);

        ReviewResponse response = reviewService.create(orgId, buildCreateRequest(1), reviewerId);

        // Should have been saved twice — second time with reported=true
        verify(reviewRepo, times(2)).save(any(OrganizationReview.class));

        // The final response should come from the second save (reported = true)
        assertThat(response.isReported()).isTrue();

        verify(trustScoreService).recompute(orgId);
    }

    /* ── getByOrg() ─────────────────────────────────────────────────────── */

    @Test
    @DisplayName("getByOrg_returnsPage")
    void getByOrg_returnsPage() {
        String orgId = "org-1";
        Pageable pageable = PageRequest.of(0, 10);

        OrganizationReview r = buildReview("rev-1", orgId, "user-1", 5);
        Page<OrganizationReview> page = new PageImpl<>(List.of(r));
        when(reviewRepo.findByOrganizationId(orgId, pageable)).thenReturn(page);

        Page<ReviewResponse> result = reviewService.getByOrg(orgId, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo("rev-1");
    }

    /* ── reply() ────────────────────────────────────────────────────────── */

    @Test
    @DisplayName("reply_ownerReplies_savesReply")
    void reply_ownerReplies_savesReply() {
        String reviewId = "rev-1";
        String orgId = "org-1";
        String ownerId = "owner-1";

        OrganizationReview review = buildReview(reviewId, orgId, "user-1", 4);
        when(reviewRepo.findById(reviewId)).thenReturn(Optional.of(review));

        Organization org = buildOrg(orgId, ownerId);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));

        OrganizationReview saved = buildReview(reviewId, orgId, "user-1", 4);
        saved.setReply("Thank you!");
        saved.setReplyAt(LocalDateTime.now());
        when(reviewRepo.save(any())).thenReturn(saved);

        ReplyReviewRequest replyRequest = new ReplyReviewRequest();
        replyRequest.setReply("Thank you!");

        ReviewResponse response = reviewService.reply(reviewId, replyRequest, ownerId);

        assertThat(response.getReply()).isEqualTo("Thank you!");
        verify(trustScoreService).recompute(orgId);

        ArgumentCaptor<OrganizationReview> captor = ArgumentCaptor.forClass(OrganizationReview.class);
        verify(reviewRepo).save(captor.capture());
        assertThat(captor.getValue().getReply()).isEqualTo("Thank you!");
        assertThat(captor.getValue().getReplyAt()).isNotNull();
    }

    @Test
    @DisplayName("reply_nonOwner_throwsBusinessRuleException")
    void reply_nonOwner_throwsBusinessRuleException() {
        String reviewId = "rev-1";
        String orgId = "org-1";
        String realOwnerId = "owner-1";
        String imposterUserId = "imposter-2";

        OrganizationReview review = buildReview(reviewId, orgId, "user-1", 4);
        when(reviewRepo.findById(reviewId)).thenReturn(Optional.of(review));

        Organization org = buildOrg(orgId, realOwnerId);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));

        ReplyReviewRequest replyRequest = new ReplyReviewRequest();
        replyRequest.setReply("Should fail");

        assertThatThrownBy(() -> reviewService.reply(reviewId, replyRequest, imposterUserId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only the organization owner");

        verify(reviewRepo, never()).save(any());
        verify(trustScoreService, never()).recompute(any());
    }

    @Test
    @DisplayName("reply_reviewNotFound_throwsResourceNotFoundException")
    void reply_reviewNotFound_throwsResourceNotFoundException() {
        when(reviewRepo.findById("missing")).thenReturn(Optional.empty());

        ReplyReviewRequest req = new ReplyReviewRequest();
        req.setReply("reply");

        assertThatThrownBy(() -> reviewService.reply("missing", req, "owner-1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /* ── delete() ───────────────────────────────────────────────────────── */

    @Test
    @DisplayName("delete_byReviewer_deletesAndRecomputes")
    void delete_byReviewer_deletesAndRecomputes() {
        String reviewId = "rev-1";
        String orgId = "org-1";
        String reviewerId = "user-1";

        OrganizationReview review = buildReview(reviewId, orgId, reviewerId, 5);
        when(reviewRepo.findById(reviewId)).thenReturn(Optional.of(review));

        // reviewer is also the user deleting — org lookup for owner check
        Organization org = buildOrg(orgId, "different-owner");
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));

        reviewService.delete(reviewId, reviewerId);

        verify(reviewRepo).delete(review);
        verify(trustScoreService).recompute(orgId);
    }

    @Test
    @DisplayName("delete_byOrgOwner_deletesAndRecomputes")
    void delete_byOrgOwner_deletesAndRecomputes() {
        String reviewId = "rev-1";
        String orgId = "org-1";
        String ownerId = "owner-1";
        String reviewerId = "another-user";

        OrganizationReview review = buildReview(reviewId, orgId, reviewerId, 2);
        when(reviewRepo.findById(reviewId)).thenReturn(Optional.of(review));

        Organization org = buildOrg(orgId, ownerId);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));

        reviewService.delete(reviewId, ownerId);

        verify(reviewRepo).delete(review);
        verify(trustScoreService).recompute(orgId);
    }

    @Test
    @DisplayName("delete_unauthorized_throwsBusinessRuleException")
    void delete_unauthorized_throwsBusinessRuleException() {
        String reviewId = "rev-1";
        String orgId = "org-1";
        String ownerId = "owner-1";
        String reviewerId = "reviewer-1";
        String unauthorizedUser = "stranger";

        OrganizationReview review = buildReview(reviewId, orgId, reviewerId, 3);
        when(reviewRepo.findById(reviewId)).thenReturn(Optional.of(review));

        Organization org = buildOrg(orgId, ownerId);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));

        assertThatThrownBy(() -> reviewService.delete(reviewId, unauthorizedUser))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("only delete your own reviews");

        verify(reviewRepo, never()).delete(any());
        verify(trustScoreService, never()).recompute(any());
    }

    @Test
    @DisplayName("delete_reviewNotFound_throwsResourceNotFoundException")
    void delete_reviewNotFound_throwsResourceNotFoundException() {
        when(reviewRepo.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.delete("ghost", "any-user"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
