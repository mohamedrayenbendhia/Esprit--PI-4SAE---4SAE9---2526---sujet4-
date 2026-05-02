package com.esprit.microservice.contrat_backend.services;

import com.esprit.microservice.contrat_backend.entities.Milestone;
import com.esprit.microservice.contrat_backend.entities.MilestoneStatus;

import java.util.List;

public interface IMilestoneService {

    // ========== CRUD ==========
    Milestone create(Long contractId, Milestone milestone);
    Milestone getById(Long id);
    List<Milestone> getAll();
    Milestone update(Long id, Milestone milestone);
    void delete(Long id);

    // ========== RECHERCHE ==========
    List<Milestone> getByContract(Long contractId);
    List<Milestone> getByContractAndStatus(Long contractId, MilestoneStatus status);
    List<Milestone> getByStatus(MilestoneStatus status);

    // ========== GESTION STATUT ==========
    Milestone updateStatus(Long id, MilestoneStatus status);
    Milestone validate(Long id, String validatorId, String comment);
    Milestone reject(Long id, String reason);

    // ========== STATISTIQUES ==========
    Long countPendingMilestones(Long contractId);
    Long countValidatedMilestones(Long contractId);
}