package com.esprit.microservice.contrat_backend.repositories;

import com.esprit.microservice.contrat_backend.entities.CustomClause;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomClauseRepository extends JpaRepository<CustomClause, Long> {

    // ========== RECHERCHE PAR CONTRAT ==========
    List<CustomClause> findByContractId(Long contractId);
    List<CustomClause> findByContractIdOrderByPosition(Long contractId);
    List<CustomClause> findByContractIdOrderByClauseNumber(Long contractId);

    // ========== RECHERCHE PAR CRÉATEUR ==========
    List<CustomClause> findByAddedBy(String userId);

    // ========== STATISTIQUES ==========
    Long countByContractId(Long contractId);
}