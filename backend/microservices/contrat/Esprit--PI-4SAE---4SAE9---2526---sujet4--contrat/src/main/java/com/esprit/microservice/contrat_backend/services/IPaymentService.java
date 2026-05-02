package com.esprit.microservice.contrat_backend.services;

import com.esprit.microservice.contrat_backend.entities.PaymentSchedule;
import com.esprit.microservice.contrat_backend.entities.PaymentStatus;

import java.util.List;

public interface IPaymentService {

    // ========== CRUD ==========
    PaymentSchedule create(Long contractId, PaymentSchedule payment);
    PaymentSchedule getById(Long id);
    List<PaymentSchedule> getAll();
    PaymentSchedule update(Long id, PaymentSchedule payment);
    void delete(Long id);

    // ========== RECHERCHE ==========
    List<PaymentSchedule> getByContract(Long contractId);
    List<PaymentSchedule> getByContractAndStatus(Long contractId, PaymentStatus status);
    List<PaymentSchedule> getByStatus(PaymentStatus status);

    // ========== GESTION STATUT ==========
    PaymentSchedule updateStatus(Long id, PaymentStatus status);
    PaymentSchedule markAsPaid(Long id, String invoiceNumber);
    PaymentSchedule markAsOverdue(Long id);

    // ========== STATISTIQUES ==========
    Long countPendingPayments(Long contractId);
    Long countPaidPayments(Long contractId);
}