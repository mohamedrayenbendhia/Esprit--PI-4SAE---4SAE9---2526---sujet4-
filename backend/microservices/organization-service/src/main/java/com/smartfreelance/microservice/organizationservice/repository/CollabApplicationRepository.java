package com.smartfreelance.microservice.organizationservice.repository;

import com.smartfreelance.microservice.organizationservice.entity.CollabApplication;
import com.smartfreelance.microservice.organizationservice.enums.CollabApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollabApplicationRepository extends JpaRepository<CollabApplication, String> {

    /** Toutes les candidatures pour une offre (vue manager) */
    Page<CollabApplication> findByOfferId(String offerId, Pageable pageable);

    /** Candidatures d'un freelance (toutes offres confondues) */
    List<CollabApplication> findByApplicantId(String applicantId);

    /** Candidatures d'un freelance pour une organisation précise */
    List<CollabApplication> findByApplicantIdAndOrganizationId(String applicantId, String organizationId);

    /** Vérifie qu'une candidature en attente n'existe pas déjà */
    boolean existsByOfferIdAndApplicantIdAndStatus(String offerId, String applicantId, CollabApplicationStatus status);

    /** Nombre d'acceptées pour vérifier le quota */
    long countByOfferIdAndStatus(String offerId, CollabApplicationStatus status);

    /** Comptages globaux par organisation (pour le dashboard) */
    long countByOrganizationId(String organizationId);

    long countByOrganizationIdAndStatus(String organizationId, CollabApplicationStatus status);
}
