package com.esprit.microservice.contrat_backend.repositories;

import com.esprit.microservice.contrat_backend.entities.PaymentSchedule;
import com.esprit.microservice.contrat_backend.entities.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule, Long> {

    // ========== RECHERCHE PAR CONTRAT ==========
    List<PaymentSchedule> findByContractId(Long contractId);
    List<PaymentSchedule> findByContractIdOrderBySequenceNumber(Long contractId);
    List<PaymentSchedule> findByContractIdAndStatus(Long contractId, PaymentStatus status);

    // ========== RECHERCHE PAR STATUT ==========
    List<PaymentSchedule> findByStatus(PaymentStatus status);

    // ========== RECHERCHE PAR DATE ==========
    List<PaymentSchedule> findByDueDateBefore(LocalDate date);
    List<PaymentSchedule> findByDueDateAfter(LocalDate date);
    List<PaymentSchedule> findByDueDateBetween(LocalDate startDate, LocalDate endDate);

    // ========== PAIEMENTS EN RETARD ==========
    List<PaymentSchedule> findByStatusAndDueDateBefore(PaymentStatus status, LocalDate date);

    // ========== STATISTIQUES ==========
    Long countByContractId(Long contractId);
    Long countByContractIdAndStatus(Long contractId, PaymentStatus status);
    Long countByStatus(PaymentStatus status);
}