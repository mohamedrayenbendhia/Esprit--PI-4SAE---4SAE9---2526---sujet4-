package com.smartfreelance.microservice.organizationservice.repository;

import com.smartfreelance.microservice.organizationservice.entity.Invitation;
import com.smartfreelance.microservice.organizationservice.enums.InvitationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, String> {

    Optional<Invitation> findByToken(String token);

    Page<Invitation> findByOrganizationId(String organizationId, Pageable pageable);

    List<Invitation> findByInviteeIdAndStatus(String inviteeId, InvitationStatus status);

    @Query("""
            SELECT i FROM Invitation i
            WHERE i.status = :status
              AND (
                    i.inviteeId = :inviteeId
                    OR (:inviteeEmail IS NOT NULL AND LOWER(i.inviteeEmail) = LOWER(:inviteeEmail))
                  )
            """)
    List<Invitation> findPendingForInvitee(@Param("inviteeId") String inviteeId,
                                           @Param("inviteeEmail") String inviteeEmail,
                                           @Param("status") InvitationStatus status);

    boolean existsByOrganizationIdAndInviteeIdAndStatus(String organizationId, String inviteeId, InvitationStatus status);

    boolean existsByOrganizationIdAndInviteeEmailIgnoreCaseAndStatus(String organizationId, String inviteeEmail, InvitationStatus status);

    List<Invitation> findByOrganizationIdAndStatus(String organizationId, InvitationStatus status);

    long countByOrganizationId(String organizationId);

    long countByOrganizationIdAndStatus(String organizationId, InvitationStatus status);
}
