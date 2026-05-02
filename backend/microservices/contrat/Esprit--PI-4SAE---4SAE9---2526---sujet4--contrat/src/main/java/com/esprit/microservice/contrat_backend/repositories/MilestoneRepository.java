package com.esprit.microservice.contrat_backend.repositories;

import com.esprit.microservice.contrat_backend.entities.Milestone;
import com.esprit.microservice.contrat_backend.entities.MilestoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    // ========== RECHERCHE PAR CONTRAT ==========
    List<Milestone> findByContractId(Long contractId);
    List<Milestone> findByContractIdOrderBySequenceNumber(Long contractId);
    List<Milestone> findByContractIdAndStatus(Long contractId, MilestoneStatus status);

    // ========== RECHERCHE PAR STATUT ==========
    List<Milestone> findByStatus(MilestoneStatus status);

    // ========== RECHERCHE PAR DATE ==========
    List<Milestone> findByDueDateBefore(LocalDate date);
    List<Milestone> findByDueDateAfter(LocalDate date);
    List<Milestone> findByDueDateBetween(LocalDate startDate, LocalDate endDate);

    // ========== JALONS EN RETARD ==========
    List<Milestone> findByStatusAndDueDateBefore(MilestoneStatus status, LocalDate date);

    // ========== STATISTIQUES ==========
    Long countByContractId(Long contractId);
    Long countByContractIdAndStatus(Long contractId, MilestoneStatus status);
    Long countByStatus(MilestoneStatus status);
}