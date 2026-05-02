package com.smartfreelance.microservice.organizationservice.repository;

import com.smartfreelance.microservice.organizationservice.entity.OrganizationReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationReviewRepository extends JpaRepository<OrganizationReview, String> {

    Page<OrganizationReview> findByOrganizationId(String organizationId, Pageable pageable);

    boolean existsByOrganizationIdAndReviewerId(String organizationId, String reviewerId);

    @Query("SELECT AVG(r.rating) FROM OrganizationReview r WHERE r.organizationId = :orgId")
    Optional<Double> findAverageRatingByOrganizationId(@Param("orgId") String organizationId);

    long countByOrganizationId(String organizationId);

    long countByOrganizationIdAndRating(String organizationId, int rating);

    /** Distribution complète des notes en une seule requête (évite 5 appels séparés) */
    @Query("SELECT r.rating, COUNT(r) FROM OrganizationReview r WHERE r.organizationId = :orgId GROUP BY r.rating")
    List<Object[]> findRatingDistribution(@Param("orgId") String organizationId);

    long countByOrganizationIdAndReported(String organizationId, boolean reported);

    long countByOrganizationIdAndReplyIsNotNull(String organizationId);

    /** Détection de review bombing : compte les avis ≤ maxRating depuis une date donnée. */
    long countByOrganizationIdAndRatingLessThanEqualAndCreatedAtAfter(
            String organizationId, int maxRating, LocalDateTime since);
}
