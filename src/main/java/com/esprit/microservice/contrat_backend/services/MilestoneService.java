package com.esprit.microservice.contrat_backend.services;

import com.esprit.microservice.contrat_backend.entities.Contract;
import com.esprit.microservice.contrat_backend.entities.Milestone;
import com.esprit.microservice.contrat_backend.entities.MilestoneStatus;
import com.esprit.microservice.contrat_backend.repositories.ContractRepository;
import com.esprit.microservice.contrat_backend.repositories.MilestoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MilestoneService implements IMilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final ContractRepository contractRepository;

    // ========== CRUD ==========

    @Override
    @Transactional
    public Milestone create(Long contractId, Milestone milestone) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + contractId));

        milestone.setContract(contract);

        List<Milestone> existingMilestones = milestoneRepository.findByContractIdOrderBySequenceNumber(contractId);
        int nextSequence = existingMilestones.size() + 1;
        milestone.setSequenceNumber(nextSequence);

        if (milestone.getStatus() == null) {
            milestone.setStatus(MilestoneStatus.PENDING);
        }

        return milestoneRepository.save(milestone);
    }

    @Override
    public Milestone getById(Long id) {
        return milestoneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Milestone not found with id: " + id));
    }

    @Override
    public List<Milestone> getAll() {
        return milestoneRepository.findAll();
    }

    @Override
    @Transactional
    public Milestone update(Long id, Milestone milestone) {
        Milestone existing = getById(id);
        milestone.setId(id);
        milestone.setContract(existing.getContract());
        milestone.setSequenceNumber(existing.getSequenceNumber());
        milestone.setCreatedAt(existing.getCreatedAt());
        return milestoneRepository.save(milestone);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        milestoneRepository.deleteById(id);
    }

    // ========== RECHERCHE ==========

    @Override
    public List<Milestone> getByContract(Long contractId) {
        return milestoneRepository.findByContractIdOrderBySequenceNumber(contractId);
    }

    @Override
    public List<Milestone> getByContractAndStatus(Long contractId, MilestoneStatus status) {
        return milestoneRepository.findByContractIdAndStatus(contractId, status);
    }

    @Override
    public List<Milestone> getByStatus(MilestoneStatus status) {
        return milestoneRepository.findByStatus(status);
    }

    // ========== GESTION STATUT ==========

    @Override
    @Transactional
    public Milestone updateStatus(Long id, MilestoneStatus status) {
        Milestone milestone = getById(id);
        milestone.setStatus(status);
        return milestoneRepository.save(milestone);
    }

    @Override
    @Transactional
    public Milestone validate(Long id, String validatorId, String comment) {
        Milestone milestone = getById(id);

        if (milestone.getStatus() != MilestoneStatus.AWAITING_VALIDATION) {
            throw new RuntimeException("Milestone must be awaiting validation to be validated");
        }

        milestone.setStatus(MilestoneStatus.VALIDATED);
        milestone.setValidatedAt(LocalDateTime.now());
        milestone.setValidatedBy(validatorId);

        if (comment != null && !comment.isEmpty()) {
            milestone.setValidationComment(comment);
        }

        return milestoneRepository.save(milestone);
    }

    @Override
    @Transactional
    public Milestone reject(Long id, String reason) {
        Milestone milestone = getById(id);

        if (milestone.getStatus() != MilestoneStatus.AWAITING_VALIDATION) {
            throw new RuntimeException("Milestone must be awaiting validation to be rejected");
        }

        milestone.setStatus(MilestoneStatus.REJECTED);
        milestone.setValidationComment(reason);

        return milestoneRepository.save(milestone);
    }

    // ========== STATISTIQUES ==========

    @Override
    public Long countPendingMilestones(Long contractId) {
        return milestoneRepository.countByContractIdAndStatus(contractId, MilestoneStatus.PENDING);
    }

    @Override
    public Long countValidatedMilestones(Long contractId) {
        return milestoneRepository.countByContractIdAndStatus(contractId, MilestoneStatus.VALIDATED);
    }
}