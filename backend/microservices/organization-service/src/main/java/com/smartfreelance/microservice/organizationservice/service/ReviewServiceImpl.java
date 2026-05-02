package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.CreateReviewRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.ReplyReviewRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.ReviewResponse;
import com.smartfreelance.microservice.organizationservice.entity.OrganizationReview;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implémentation du service d'avis avec :
 *  - Détection de review bombing (≥ 4 avis ≤ 2★ en 24 h)
 *  - Recalcul du TrustScore après chaque modification
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    /** Seuil bombing : si >= BOMBING_THRESHOLD avis ≤ 2★ en 24h → signaler automatiquement. */
    private static final int  BOMBING_THRESHOLD  = 4;
    private static final int  BOMBING_MAX_RATING = 2;

    private final OrganizationReviewRepository reviewRepo;
    private final OrganizationRepository        orgRepo;
    private final TrustScoreService             trustScoreService;

    @Override
    public ReviewResponse create(String orgId, CreateReviewRequest request, String reviewerId) {
        // ── Création de l'avis ─────────────────────────────────────────────────
        OrganizationReview review = OrganizationReview.builder()
                .organizationId(orgId)
                .reviewerId(reviewerId)
                .projectId(request.getProjectId())
                .rating(request.getRating())
                .comment(request.getComment())
                .build();
        review = reviewRepo.save(review);

        // ── Règle 3 : détection de review bombing ─────────────────────────────
        if (isUnderBombingAttack(orgId)) {
            review.setReported(true);
            review = reviewRepo.save(review);
            log.warn("[FraudDetection] Review bombing détecté sur l'organisation {} — avis {} signalé automatiquement",
                    orgId, review.getId());
        }

        // ── Recalcul du score ──────────────────────────────────────────────────
        trustScoreService.recompute(orgId);
        return toResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canReview(String orgId, String reviewerId) {
        Organization org = orgRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + orgId));
        return !org.getOwnerId().equals(reviewerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getByOrg(String orgId, Pageable pageable) {
        return reviewRepo.findByOrganizationId(orgId, pageable).map(this::toResponse);
    }

    @Override
    public ReviewResponse reply(String reviewId, ReplyReviewRequest request, String ownerId) {
        OrganizationReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
        Organization org = orgRepo.findById(review.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        if (!org.getOwnerId().equals(ownerId)) {
            throw new BusinessRuleException("Only the organization owner can reply to reviews.");
        }
        review.setReply(request.getReply());
        review.setReplyAt(LocalDateTime.now());
        ReviewResponse response = toResponse(reviewRepo.save(review));
        // Une réponse améliore le taux de reply → recalcul du score
        trustScoreService.recompute(org.getId());
        return response;
    }

    @Override
    public void delete(String reviewId, String userId) {
        OrganizationReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
        // Autorisé : l'auteur de l'avis OU le propriétaire de l'organisation
        boolean isReviewer = review.getReviewerId().equals(userId);
        boolean isOrgOwner = orgRepo.findById(review.getOrganizationId())
                .map(org -> org.getOwnerId().equals(userId))
                .orElse(false);
        if (!isReviewer && !isOrgOwner) {
            throw new BusinessRuleException("You can only delete your own reviews, or reviews left on your organization.");
        }
        String orgId = review.getOrganizationId();
        reviewRepo.delete(review);
        trustScoreService.recompute(orgId); // un avis supprimé change la moyenne
    }

    @Override
    public ReviewResponse report(String reviewId, String reporterId) {
        OrganizationReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
        review.setReported(true);
        return toResponse(reviewRepo.save(review));
    }

    private ReviewResponse toResponse(OrganizationReview r) {
        return ReviewResponse.builder()
                .id(r.getId()).organizationId(r.getOrganizationId()).reviewerId(r.getReviewerId())
                .projectId(r.getProjectId()).rating(r.getRating()).comment(r.getComment())
                .reply(r.getReply()).replyAt(r.getReplyAt()).reported(r.isReported())
                .createdAt(r.getCreatedAt()).build();
    }

    // ── Fraud detection helpers ───────────────────────────────────────────────

    /**
     * Détecte un potentiel review bombing :
     * ≥ BOMBING_THRESHOLD avis avec note ≤ BOMBING_MAX_RATING dans les dernières 24 heures.
     */
    private boolean isUnderBombingAttack(String orgId) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        long recentBadReviews = reviewRepo
                .countByOrganizationIdAndRatingLessThanEqualAndCreatedAtAfter(orgId, BOMBING_MAX_RATING, since);
        return recentBadReviews >= BOMBING_THRESHOLD;
    }
}
