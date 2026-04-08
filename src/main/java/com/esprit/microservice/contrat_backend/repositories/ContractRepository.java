package com.esprit.microservice.contrat_backend.repositories;

import com.esprit.microservice.contrat_backend.entities.Contract;
import com.esprit.microservice.contrat_backend.entities.ContractStatus;
import com.esprit.microservice.contrat_backend.entities.ContractType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    // ========== RECHERCHE PAR NUMÉRO ==========
    Optional<Contract> findByContractNumber(String contractNumber);

    // ========== RECHERCHE PAR CLIENT ==========
    List<Contract> findByClientId(String clientId);
    List<Contract> findByClientIdOrderByCreatedAtDesc(String clientId);
    List<Contract> findByClientIdAndStatus(String clientId, ContractStatus status);
    List<Contract> findByClientIdAndContractType(String clientId, ContractType contractType);

    // ========== RECHERCHE PAR FREELANCER ==========
    List<Contract> findByFreelancerId(String freelancerId);
    List<Contract> findByFreelancerIdOrderByCreatedAtDesc(String freelancerId);
    List<Contract> findByFreelancerIdAndStatus(String freelancerId, ContractStatus status);
    List<Contract> findByFreelancerIdAndContractType(String freelancerId, ContractType contractType);

    // ========== RECHERCHE PAR STATUT & TYPE ==========
    List<Contract> findByStatus(ContractStatus status);
    List<Contract> findByContractType(ContractType contractType);

    // ========== RECHERCHE PAR MOT-CLÉ CLIENT ==========
    @Query("SELECT c FROM Contract c WHERE c.clientId = :clientId AND " +
            "(LOWER(c.missionTitle) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.missionDescription) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.freelancerName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.contractNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Contract> searchByClientIdAndKeyword(@Param("clientId") String clientId,
                                              @Param("keyword") String keyword);

    // ========== RECHERCHE PAR MOT-CLÉ FREELANCER ==========
    @Query("SELECT c FROM Contract c WHERE c.freelancerId = :freelancerId AND " +
            "(LOWER(c.missionTitle) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.missionDescription) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.clientName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.contractNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Contract> searchByFreelancerIdAndKeyword(@Param("freelancerId") String freelancerId,
                                                  @Param("keyword") String keyword);

    // ========== STATISTIQUES ==========
    Long countByClientId(String clientId);
    Long countByFreelancerId(String freelancerId);
    Long countByStatus(ContractStatus status);
    Long countByClientIdAndStatus(String clientId, ContractStatus status);
    Long countByFreelancerIdAndStatus(String freelancerId, ContractStatus status);

    // ========== VÉRIFICATION SIGNATURE (QR CODE) ==========
    boolean existsByClientSignatureHash(String clientSignatureHash);
}