package com.esprit.microservice.contrat_backend.services;

import com.esprit.microservice.contrat_backend.entities.Contract;
import com.esprit.microservice.contrat_backend.entities.ContractStatus;
import com.esprit.microservice.contrat_backend.entities.ContractType;
import com.esprit.microservice.contrat_backend.entities.CustomClause;

import java.util.List;
import java.util.Map;

public interface IContractService {

    Contract create(Contract contract);
    Contract getById(Long id);
    List<Contract> getAll();
    Contract update(Long id, Contract contract);
    void delete(Long id);

    List<Contract> getByClientId(String clientId);
    List<Contract> getByFreelancerId(String freelancerId);
    List<Contract> getByClientIdAndStatus(String clientId, ContractStatus status);
    List<Contract> getByFreelancerIdAndStatus(String freelancerId, ContractStatus status);
    List<Contract> getByClientIdAndType(String clientId, ContractType type);
    List<Contract> getByFreelancerIdAndType(String freelancerId, ContractType type);
    List<Contract> searchByKeyword(String clientId, String keyword);
    List<Contract> searchByKeywordForFreelancer(String freelancerId, String keyword);

    // Signature par image (canvas)
    Contract signByClient(Long id, String signatureImage);
    // Signature par code externe
    Contract signByClientWithCode(Long id, String signatureCode);

    Contract sendToFreelancer(Long id);

    // Signature par image (canvas)
    Contract signByFreelancer(Long id, String signatureImage);
    // Signature par code externe
    Contract signByFreelancerWithCode(Long id, String signatureCode);

    Contract requestModification(Long id, String reason);
    Contract acceptContract(Long id);
    Contract declineContract(Long id, String reason);
    Contract activate(Long id);
    Contract complete(Long id);
    Contract cancel(Long id);

    CustomClause addCustomClause(Long contractId, CustomClause clause);
    List<CustomClause> getCustomClauses(Long contractId);
    void deleteCustomClause(Long clauseId);

    String generateContractNumber();
    String generateSignatureCode();

    Map<String, Object> getClientStatistics(String clientId);
    Map<String, Object> getFreelancerStatistics(String freelancerId);
    Map<String, Object> getFreelancerEarnings(String freelancerId);
}