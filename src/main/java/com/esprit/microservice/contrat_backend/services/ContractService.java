package com.esprit.microservice.contrat_backend.services;

import com.esprit.microservice.contrat_backend.entities.*;
import com.esprit.microservice.contrat_backend.repositories.ContractRepository;
import com.esprit.microservice.contrat_backend.repositories.CustomClauseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ContractService implements IContractService {

    private final ContractRepository contractRepository;
    private final CustomClauseRepository customClauseRepository;

    // ========== UTILITAIRE HASH ==========

    private String hashSignature(String rawSignature) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawSignature.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    // ========== CRUD BASIQUE ==========

    @Override
    @Transactional
    public Contract create(Contract contract) {
        if (contract.getContractNumber() == null || contract.getContractNumber().isEmpty()) {
            contract.setContractNumber(generateContractNumber());
        }
        if (contract.getStatus() == null) {
            contract.setStatus(ContractStatus.DRAFT);
        }
        return contractRepository.save(contract);
    }

    @Override
    public Contract getById(Long id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + id));
    }

    @Override
    public List<Contract> getAll() {
        return contractRepository.findAll();
    }

    @Override
    @Transactional
    public Contract update(Long id, Contract contract) {
        Contract existing = getById(id);
        if (existing.getStatus() != ContractStatus.DRAFT) {
            throw new RuntimeException("Cannot update contract that is not in DRAFT status");
        }
        contract.setId(id);
        contract.setContractNumber(existing.getContractNumber());
        contract.setCreatedAt(existing.getCreatedAt());
        return contractRepository.save(contract);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Contract contract = getById(id);
        if (contract.getStatus() != ContractStatus.DRAFT &&
                contract.getStatus() != ContractStatus.CANCELLED) {
            throw new RuntimeException("Cannot delete contract that is not in DRAFT or CANCELLED status");
        }
        contractRepository.deleteById(id);
    }

    // ========== RECHERCHE PAR UTILISATEUR ==========

    @Override
    public List<Contract> getByClientId(String clientId) {
        return contractRepository.findByClientIdOrderByCreatedAtDesc(clientId);
    }

    @Override
    public List<Contract> getByFreelancerId(String freelancerId) {
        return contractRepository.findByFreelancerIdOrderByCreatedAtDesc(freelancerId);
    }

    // ========== RECHERCHE PAR STATUT ==========

    @Override
    public List<Contract> getByClientIdAndStatus(String clientId, ContractStatus status) {
        return contractRepository.findByClientIdAndStatus(clientId, status);
    }

    @Override
    public List<Contract> getByFreelancerIdAndStatus(String freelancerId, ContractStatus status) {
        return contractRepository.findByFreelancerIdAndStatus(freelancerId, status);
    }

    // ========== RECHERCHE PAR TYPE ==========

    @Override
    public List<Contract> getByClientIdAndType(String clientId, ContractType type) {
        return contractRepository.findByClientIdAndContractType(clientId, type);
    }

    @Override
    public List<Contract> getByFreelancerIdAndType(String freelancerId, ContractType type) {
        return contractRepository.findByFreelancerIdAndContractType(freelancerId, type);
    }

    // ========== RECHERCHE PAR MOT-CLÉ ==========

    @Override
    public List<Contract> searchByKeyword(String clientId, String keyword) {
        return contractRepository.searchByClientIdAndKeyword(clientId, keyword);
    }

    @Override
    public List<Contract> searchByKeywordForFreelancer(String freelancerId, String keyword) {
        return contractRepository.searchByFreelancerIdAndKeyword(freelancerId, keyword);
    }

    // ========== WORKFLOW SIGNATURES ==========

    @Override
    @Transactional
    public Contract signByClient(Long id, String signatureData) {
        Contract contract = getById(id);
        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new RuntimeException("Contract must be in DRAFT status to be signed by client");
        }
        contract.setClientSignedAt(LocalDateTime.now());
        contract.setClientSignatureHash(hashSignature(signatureData));
        contract.setClientSignatureImage(signatureData);
        contract.setModificationComment(null);
        contract.setModificationRequestedAt(null);
        contract.setStatus(ContractStatus.PENDING_FREELANCER_SIGNATURE);
        return contractRepository.save(contract);
    }

    @Override
    @Transactional
    public Contract sendToFreelancer(Long id) {
        Contract contract = getById(id);
        if (contract.getStatus() != ContractStatus.PENDING_FREELANCER_SIGNATURE) {
            throw new RuntimeException("Contract must be signed by client before sending to freelancer");
        }
        return contract;
    }

    @Override
    @Transactional
    public Contract signByFreelancer(Long id, String signatureData) {
        Contract contract = getById(id);
        if (contract.getStatus() != ContractStatus.PENDING_FREELANCER_SIGNATURE) {
            throw new RuntimeException("Contract must be pending freelancer signature");
        }
        contract.setFreelancerSignedAt(LocalDateTime.now());
        contract.setFreelancerSignatureHash(hashSignature(signatureData));
        contract.setFreelancerSignatureImage(signatureData);
        contract.setStatus(ContractStatus.ACTIVE);
        return contractRepository.save(contract);
    }

    // ========== WORKFLOW FREELANCER ==========

    @Override
    @Transactional
    public Contract requestModification(Long id, String reason) {
        Contract contract = getById(id);
        if (contract.getStatus() != ContractStatus.PENDING_FREELANCER_SIGNATURE) {
            throw new RuntimeException("Cannot request modification at this stage");
        }
        contract.setStatus(ContractStatus.DRAFT);
        contract.setModificationComment(reason);
        contract.setModificationRequestedAt(LocalDateTime.now());
        contract.setClientSignedAt(null);
        contract.setClientSignatureHash(null);
        contract.setClientSignatureImage(null);
        return contractRepository.save(contract);
    }

    @Override
    @Transactional
    public Contract acceptContract(Long id) {
        throw new RuntimeException("Use signByFreelancer() instead. Freelancer must sign the contract.");
    }

    @Override
    @Transactional
    public Contract declineContract(Long id, String reason) {
        Contract contract = getById(id);
        if (contract.getStatus() != ContractStatus.PENDING_FREELANCER_SIGNATURE) {
            throw new RuntimeException("Contract must be pending freelancer signature");
        }
        contract.setStatus(ContractStatus.CANCELLED);
        return contractRepository.save(contract);
    }

    // ========== GESTION CYCLE DE VIE ==========

    @Override
    @Transactional
    public Contract activate(Long id) {
        Contract contract = getById(id);
        if (contract.getStatus() != ContractStatus.FULLY_SIGNED) {
            throw new RuntimeException("Contract must be fully signed to be activated");
        }
        contract.setStatus(ContractStatus.ACTIVE);
        return contractRepository.save(contract);
    }

    @Override
    @Transactional
    public Contract complete(Long id) {
        Contract contract = getById(id);
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new RuntimeException("Only active contracts can be completed");
        }
        contract.setStatus(ContractStatus.COMPLETED);
        return contractRepository.save(contract);
    }

    @Override
    @Transactional
    public Contract cancel(Long id) {
        Contract contract = getById(id);
        if (contract.getStatus() == ContractStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel a completed contract");
        }
        contract.setStatus(ContractStatus.CANCELLED);
        return contractRepository.save(contract);
    }

    // ========== GESTION CLAUSES PERSONNALISÉES ==========

    @Override
    @Transactional
    public CustomClause addCustomClause(Long contractId, CustomClause clause) {
        Contract contract = getById(contractId);
        clause.setContract(contract);
        List<CustomClause> existingClauses = customClauseRepository.findByContractId(contractId);
        int nextNumber = existingClauses.size() + 1;
        clause.setClauseNumber(nextNumber);
        clause.setPosition(nextNumber);
        return customClauseRepository.save(clause);
    }

    @Override
    public List<CustomClause> getCustomClauses(Long contractId) {
        return customClauseRepository.findByContractIdOrderByPosition(contractId);
    }

    @Override
    @Transactional
    public void deleteCustomClause(Long clauseId) {
        customClauseRepository.deleteById(clauseId);
    }

    // ========== UTILITAIRES ==========

    @Override
    public String generateContractNumber() {
        String year = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy"));
        long count = contractRepository.count() + 1;
        return String.format("CTR-%s-%05d", year, count);
    }

    // ========== STATISTIQUES ==========

    @Override
    public Map<String, Object> getClientStatistics(String clientId) {
        Map<String, Object> stats = new HashMap<>();
        List<Contract> allContracts = getByClientId(clientId);
        stats.put("totalContracts", allContracts.size());
        stats.put("draftContracts", allContracts.stream().filter(c -> c.getStatus() == ContractStatus.DRAFT).count());
        stats.put("activeContracts", allContracts.stream().filter(c -> c.getStatus() == ContractStatus.ACTIVE).count());
        stats.put("completedContracts", allContracts.stream().filter(c -> c.getStatus() == ContractStatus.COMPLETED).count());
        stats.put("pendingSignature", allContracts.stream().filter(c -> c.getStatus() == ContractStatus.PENDING_FREELANCER_SIGNATURE).count());
        return stats;
    }

    @Override
    public Map<String, Object> getFreelancerStatistics(String freelancerId) {
        Map<String, Object> stats = new HashMap<>();
        List<Contract> allContracts = getByFreelancerId(freelancerId);
        stats.put("totalContracts", allContracts.size());
        stats.put("pendingSignature", allContracts.stream().filter(c -> c.getStatus() == ContractStatus.PENDING_FREELANCER_SIGNATURE).count());
        stats.put("activeContracts", allContracts.stream().filter(c -> c.getStatus() == ContractStatus.ACTIVE).count());
        stats.put("completedContracts", allContracts.stream().filter(c -> c.getStatus() == ContractStatus.COMPLETED).count());
        return stats;
    }

    @Override
    public Map<String, Object> getFreelancerEarnings(String freelancerId) {
        Map<String, Object> earnings = new HashMap<>();
        List<Contract> contracts = getByFreelancerId(freelancerId);
        double totalEarnings = contracts.stream()
                .filter(c -> c.getStatus() == ContractStatus.COMPLETED)
                .mapToDouble(c -> c.getTotalAmount().doubleValue())
                .sum();
        double pendingEarnings = contracts.stream()
                .filter(c -> c.getStatus() == ContractStatus.ACTIVE)
                .mapToDouble(c -> c.getTotalAmount().doubleValue())
                .sum();
        earnings.put("totalEarnings", totalEarnings);
        earnings.put("pendingEarnings", pendingEarnings);
        earnings.put("completedContracts", contracts.stream()
                .filter(c -> c.getStatus() == ContractStatus.COMPLETED).count());
        return earnings;
    }

    // ========== VÉRIFICATION SIGNATURE (QR CODE) ==========

    @Override
    public boolean existsByClientSignatureHash(String hash) {
        return contractRepository.existsByClientSignatureHash(hash);
    }
}