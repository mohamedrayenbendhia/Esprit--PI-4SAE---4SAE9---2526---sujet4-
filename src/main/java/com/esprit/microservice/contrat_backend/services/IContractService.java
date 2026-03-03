package com.esprit.microservice.contrat_backend.services;

import com.esprit.microservice.contrat_backend.entities.Contract;
import com.esprit.microservice.contrat_backend.entities.ContractStatus;
import com.esprit.microservice.contrat_backend.entities.ContractType;
import com.esprit.microservice.contrat_backend.entities.CustomClause;

import java.util.List;
import java.util.Map;

public interface IContractService {

    // ========== CRUD BASIQUE ==========
    Contract create(Contract contract);
    Contract getById(Long id);
    List<Contract> getAll();
    Contract update(Long id, Contract contract);
    void delete(Long id);

    // ========== RECHERCHE PAR UTILISATEUR ==========
    List<Contract> getByClientId(String clientId);
    List<Contract> getByFreelancerId(String freelancerId);

    // ========== RECHERCHE PAR STATUT ==========
    List<Contract> getByClientIdAndStatus(String clientId, ContractStatus status);
    List<Contract> getByFreelancerIdAndStatus(String freelancerId, ContractStatus status);

    // ========== RECHERCHE PAR TYPE ==========
    List<Contract> getByClientIdAndType(String clientId, ContractType type);
    List<Contract> getByFreelancerIdAndType(String freelancerId, ContractType type);

    // ========== RECHERCHE PAR MOT-CLÉ ==========
    List<Contract> searchByKeyword(String clientId, String keyword);
    List<Contract> searchByKeywordForFreelancer(String freelancerId, String keyword);

    // ========== WORKFLOW SIGNATURES ==========
    Contract signByClient(Long id, String signatureHash);
    Contract sendToFreelancer(Long id);
    Contract signByFreelancer(Long id, String signatureHash);

    // ========== WORKFLOW FREELANCER ==========
    Contract requestModification(Long id, String reason);
    Contract acceptContract(Long id);
    Contract declineContract(Long id, String reason);

    // ========== GESTION CYCLE DE VIE ==========
    Contract activate(Long id);
    Contract complete(Long id);
    Contract cancel(Long id);

    // ========== GESTION CLAUSES PERSONNALISÉES ==========
    CustomClause addCustomClause(Long contractId, CustomClause clause);
    List<CustomClause> getCustomClauses(Long contractId);
    void deleteCustomClause(Long clauseId);

    // ========== UTILITAIRES ==========
    String generateContractNumber();



    // ========== STATISTIQUES ==========
    Map<String, Object> getClientStatistics(String clientId);
    Map<String, Object> getFreelancerStatistics(String freelancerId);
    Map<String, Object> getFreelancerEarnings(String freelancerId);
}