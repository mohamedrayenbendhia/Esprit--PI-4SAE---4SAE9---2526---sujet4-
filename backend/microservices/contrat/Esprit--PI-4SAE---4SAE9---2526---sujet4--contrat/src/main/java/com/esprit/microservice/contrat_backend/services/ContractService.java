package com.esprit.microservice.contrat_backend.services;

import com.esprit.microservice.contrat_backend.entities.*;
import com.esprit.microservice.contrat_backend.repositories.ContractRepository;
import com.esprit.microservice.contrat_backend.repositories.CustomClauseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ContractService implements IContractService {

    private final ContractRepository contractRepository;
    private final CustomClauseRepository customClauseRepository;

    // ── CRUD ─────────────────────────────────────────────────────

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
                .orElseThrow(() -> new RuntimeException("Contract not found: " + id));
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
            throw new RuntimeException("Cannot delete contract not in DRAFT or CANCELLED status");
        }
        contractRepository.deleteById(id);
    }

    // ── RECHERCHE ────────────────────────────────────────────────

    @Override public List<Contract> getByClientId(String clientId) {
        return contractRepository.findByClientIdOrderByCreatedAtDesc(clientId);
    }
    @Override public List<Contract> getByFreelancerId(String freelancerId) {
        return contractRepository.findByFreelancerIdOrderByCreatedAtDesc(freelancerId);
    }
    @Override public List<Contract> getByClientIdAndStatus(String clientId, ContractStatus status) {
        return contractRepository.findByClientIdAndStatus(clientId, status);
    }
    @Override public List<Contract> getByFreelancerIdAndStatus(String freelancerId, ContractStatus status) {
        return contractRepository.findByFreelancerIdAndStatus(freelancerId, status);
    }
    @Override public List<Contract> getByClientIdAndType(String clientId, ContractType type) {
        return contractRepository.findByClientIdAndContractType(clientId, type);
    }
    @Override public List<Contract> getByFreelancerIdAndType(String freelancerId, ContractType type) {
        return contractRepository.findByFreelancerIdAndContractType(freelancerId, type);
    }
    @Override public List<Contract> searchByKeyword(String clientId, String keyword) {
        return contractRepository.searchByClientIdAndKeyword(clientId, keyword);
    }
    @Override public List<Contract> searchByKeywordForFreelancer(String freelancerId, String keyword) {
        return contractRepository.searchByFreelancerIdAndKeyword(freelancerId, keyword);
    }

    // ── SIGNATURES ───────────────────────────────────────────────

    @Override
    @Transactional
    public Contract signByClient(Long id, String signatureImage) {
        Contract contract = getById(id);
        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new RuntimeException("Contract must be in DRAFT status to be signed by client");
        }
        String code = generateSignatureCode();
        contract.setClientSignedAt(LocalDateTime.now());
        contract.setClientSignatureImage(signatureImage);
        contract.setClientSignatureCode(code);
        contract.setStatus(ContractStatus.SIGNED_BY_CLIENT);
        contract.setModificationComment(null);
        contract.setModificationRequestedAt(null);
        return contractRepository.save(contract);
    }

    @Override
    @Transactional
    public Contract signByClientWithCode(Long id, String signatureCode) {
        Contract contract = getById(id);
        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new RuntimeException("Contract must be in DRAFT status to be signed by client");
        }
        // Vérifier que le code fourni correspond au code enregistré pour ce client
        // Si le client n'a pas encore de code (première fois), on accepte et on enregistre
        if (contract.getClientSignatureCode() != null
                && !contract.getClientSignatureCode().equals(signatureCode)) {
            throw new RuntimeException("Invalid signature code");
        }
        contract.setClientSignedAt(LocalDateTime.now());
        contract.setClientSignatureImage(null); // pas d'image pour signature par code
        contract.setClientSignatureCode(signatureCode);
        contract.setStatus(ContractStatus.SIGNED_BY_CLIENT);
        contract.setModificationComment(null);
        contract.setModificationRequestedAt(null);
        return contractRepository.save(contract);
    }

    @Override
    @Transactional
    public Contract sendToFreelancer(Long id) {
        Contract contract = getById(id);
        if (contract.getStatus() != ContractStatus.SIGNED_BY_CLIENT) {
            throw new RuntimeException("Contract must be signed by client before sending to freelancer");
        }
        contract.setStatus(ContractStatus.PENDING_FREELANCER_SIGNATURE);
        return contractRepository.save(contract);
    }

    @Override
    @Transactional
    public Contract signByFreelancer(Long id, String signatureImage) {
        Contract contract = getById(id);
        if (contract.getStatus() != ContractStatus.PENDING_FREELANCER_SIGNATURE) {
            throw new RuntimeException("Contract must be pending freelancer signature");
        }
        String code = generateSignatureCode();
        contract.setFreelancerSignedAt(LocalDateTime.now());
        contract.setFreelancerSignatureImage(signatureImage);
        contract.setFreelancerSignatureCode(code);
        contract.setStatus(ContractStatus.FULLY_SIGNED);
        return contractRepository.save(contract);
    }

    @Override
    @Transactional
    public Contract signByFreelancerWithCode(Long id, String signatureCode) {
        Contract contract = getById(id);
        if (contract.getStatus() != ContractStatus.PENDING_FREELANCER_SIGNATURE) {
            throw new RuntimeException("Contract must be pending freelancer signature");
        }
        if (contract.getFreelancerSignatureCode() != null
                && !contract.getFreelancerSignatureCode().equals(signatureCode)) {
            throw new RuntimeException("Invalid signature code");
        }
        contract.setFreelancerSignedAt(LocalDateTime.now());
        contract.setFreelancerSignatureImage(null);
        contract.setFreelancerSignatureCode(signatureCode);
        contract.setStatus(ContractStatus.FULLY_SIGNED);
        return contractRepository.save(contract);
    }

    // ── WORKFLOW FREELANCER ───────────────────────────────────────

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
        contract.setClientSignatureImage(null);
        contract.setClientSignatureCode(null);
        return contractRepository.save(contract);
    }

    @Override
    @Transactional
    public Contract acceptContract(Long id) {
        throw new RuntimeException("Use signByFreelancer() instead.");
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

    // ── CYCLE DE VIE ─────────────────────────────────────────────

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

    // ── CLAUSES ──────────────────────────────────────────────────

    @Override
    @Transactional
    public CustomClause addCustomClause(Long contractId, CustomClause clause) {
        Contract contract = getById(contractId);
        clause.setContract(contract);
        List<CustomClause> existing = customClauseRepository.findByContractId(contractId);
        int next = existing.size() + 1;
        clause.setClauseNumber(next);
        clause.setPosition(next);
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

    // ── UTILITAIRES ──────────────────────────────────────────────

    @Override
    public String generateContractNumber() {
        String year = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy"));
        long count = contractRepository.count() + 1;
        return String.format("CTR-%s-%05d", year, count);
    }

    @Override
    public String generateSignatureCode() {
        // Code de 12 caractères alphanumérique : ex. SIG-A3F9-K2M7
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder("SIG-");
        for (int i = 0; i < 4; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        sb.append("-");
        for (int i = 0; i < 4; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        return sb.toString();
    }

    // ── STATISTIQUES ─────────────────────────────────────────────

    @Override
    public Map<String, Object> getClientStatistics(String clientId) {
        Map<String, Object> stats = new HashMap<>();
        List<Contract> all = getByClientId(clientId);
        stats.put("totalContracts", all.size());
        stats.put("draftContracts", all.stream().filter(c -> c.getStatus() == ContractStatus.DRAFT).count());
        stats.put("activeContracts", all.stream().filter(c -> c.getStatus() == ContractStatus.ACTIVE).count());
        stats.put("completedContracts", all.stream().filter(c -> c.getStatus() == ContractStatus.COMPLETED).count());
        stats.put("pendingSignature", all.stream().filter(c -> c.getStatus() == ContractStatus.PENDING_FREELANCER_SIGNATURE).count());
        return stats;
    }

    @Override
    public Map<String, Object> getFreelancerStatistics(String freelancerId) {
        Map<String, Object> stats = new HashMap<>();
        List<Contract> all = getByFreelancerId(freelancerId);
        stats.put("totalContracts", all.size());
        stats.put("pendingSignature", all.stream().filter(c -> c.getStatus() == ContractStatus.PENDING_FREELANCER_SIGNATURE).count());
        stats.put("activeContracts", all.stream().filter(c -> c.getStatus() == ContractStatus.ACTIVE).count());
        stats.put("completedContracts", all.stream().filter(c -> c.getStatus() == ContractStatus.COMPLETED).count());
        return stats;
    }

    @Override
    public Map<String, Object> getFreelancerEarnings(String freelancerId) {
        Map<String, Object> earnings = new HashMap<>();
        List<Contract> contracts = getByFreelancerId(freelancerId);
        double totalEarnings = contracts.stream()
                .filter(c -> c.getStatus() == ContractStatus.COMPLETED)
                .mapToDouble(c -> c.getTotalAmount().doubleValue()).sum();
        double pendingEarnings = contracts.stream()
                .filter(c -> c.getStatus() == ContractStatus.ACTIVE)
                .mapToDouble(c -> c.getTotalAmount().doubleValue()).sum();
        earnings.put("totalEarnings", totalEarnings);
        earnings.put("pendingEarnings", pendingEarnings);
        earnings.put("completedContracts", contracts.stream().filter(c -> c.getStatus() == ContractStatus.COMPLETED).count());
        return earnings;
    }
}