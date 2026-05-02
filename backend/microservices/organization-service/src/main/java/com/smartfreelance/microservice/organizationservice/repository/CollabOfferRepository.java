package com.smartfreelance.microservice.organizationservice.repository;

import com.smartfreelance.microservice.organizationservice.entity.CollabOffer;
import com.smartfreelance.microservice.organizationservice.enums.CollabOfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface CollabOfferRepository extends JpaRepository<CollabOffer, String> {

    /** Toutes les offres d'une organisation (pour les managers) */
    Page<CollabOffer> findByOrganizationId(String organizationId, Pageable pageable);

    /** Offres ouvertes d'une organisation (vue publique) */
    Page<CollabOffer> findByOrganizationIdAndStatus(String organizationId, CollabOfferStatus status, Pageable pageable);

    /** Offres ouvertes globales (exploration depuis le profil) */
    Page<CollabOffer> findByStatus(CollabOfferStatus status, Pageable pageable);

    /** Nombre d'offres par statut pour une organisation */
    long countByOrganizationIdAndStatus(String organizationId, CollabOfferStatus status);

    /** Nombre total d'offres pour une organisation */
    long countByOrganizationId(String organizationId);

    // ── Scheduler queries ─────────────────────────────────────────────────────

    /** Offres OPEN dont la deadline est dépassée (pour fermeture automatique). */
    @Query("SELECT o FROM CollabOffer o WHERE o.status = 'OPEN' AND o.deadlineDate IS NOT NULL AND o.deadlineDate < :today")
    List<CollabOffer> findExpiredOpenOffers(@Param("today") LocalDate today);

    /** Offres OPEN publiées depuis plus de {days} jours sans candidature reçue. */
    @Query("""
            SELECT o FROM CollabOffer o
            WHERE o.status = 'OPEN'
              AND o.createdAt < :before
              AND NOT EXISTS (
                  SELECT 1 FROM CollabApplication a WHERE a.offerId = o.id
              )
            """)
    List<CollabOffer> findStaleOffersWithNoApplications(@Param("before") LocalDateTime before);

    /** Toutes les offres OPEN appartenant à une organisation suspendue. */
    @Query("""
            SELECT o FROM CollabOffer o
            WHERE o.status = 'OPEN'
              AND o.organizationId IN (
                  SELECT org.id FROM Organization org WHERE org.status = 'SUSPENDED'
              )
            """)
    List<CollabOffer> findOpenOffersOfSuspendedOrgs();
}
