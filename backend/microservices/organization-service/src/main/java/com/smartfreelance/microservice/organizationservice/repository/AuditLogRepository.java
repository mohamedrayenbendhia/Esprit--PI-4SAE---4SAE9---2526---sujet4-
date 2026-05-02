package com.smartfreelance.microservice.organizationservice.repository;

import com.smartfreelance.microservice.organizationservice.entity.AuditLog;
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
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    Page<AuditLog> findByOrganizationIdOrderByCreatedAtDesc(String organizationId, Pageable pageable);

    List<AuditLog> findByPerformedByUserId(String userId);

    /** Date de la dernière action enregistrée pour une organisation (pour la détection de dormance). */
    @Query("SELECT MAX(a.createdAt) FROM AuditLog a WHERE a.organizationId = :orgId")
    Optional<LocalDateTime> findLastActivityAt(@Param("orgId") String orgId);
}
