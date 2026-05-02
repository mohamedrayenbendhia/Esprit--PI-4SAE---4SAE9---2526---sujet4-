package com.smartfreelance.microservice.organizationservice.repository;

import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationSize;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationStatus;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationType;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, String> {

    Optional<Organization> findByName(String name);

    List<Organization> findByOwnerId(String ownerId);

    Page<Organization> findByStatus(OrganizationStatus status, Pageable pageable);

    @Query("SELECT o FROM Organization o WHERE o.visibility = 'PUBLIC' AND o.status = 'ACTIVE' " +
           "AND (:keyword IS NULL OR LOWER(o.name) LIKE LOWER(CONCAT('%',:keyword,'%')) " +
           "OR LOWER(o.description) LIKE LOWER(CONCAT('%',:keyword,'%'))) " +
           "AND (:type IS NULL OR o.type = :type) " +
           "AND (:size IS NULL OR o.size = :size)")
    Page<Organization> searchPublic(@Param("keyword") String keyword,
                                    @Param("type") OrganizationType type,
                                    @Param("size") OrganizationSize size,
                                    Pageable pageable);

    boolean existsByName(String name);

    boolean existsByNameIgnoreCase(String name);

    long countByStatus(OrganizationStatus status);

    List<Organization> findByStatusAndVisibility(OrganizationStatus status, OrganizationVisibility visibility);

    // ── Scheduler queries ─────────────────────────────────────────────────────

    /** Toutes les organisations non-dissoutes pour le job de dormance. */
    @Query("SELECT o FROM Organization o WHERE o.status NOT IN ('DISSOLVED', 'REJECTED')")
    List<Organization> findAllNonDissolved();
}
