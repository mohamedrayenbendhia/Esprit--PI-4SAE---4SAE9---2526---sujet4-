package com.esprit.microservice.contrat_backend.services;

import com.esprit.microservice.contrat_backend.entities.Contract;
import com.esprit.microservice.contrat_backend.entities.PaymentSchedule;
import com.esprit.microservice.contrat_backend.entities.PaymentStatus;
import com.esprit.microservice.contrat_backend.repositories.ContractRepository;
import com.esprit.microservice.contrat_backend.repositories.PaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService implements IPaymentService {

    private final PaymentScheduleRepository paymentScheduleRepository;
    private final ContractRepository contractRepository;

    // ========== CRUD ==========

    @Override
    @Transactional
    public PaymentSchedule create(Long contractId, PaymentSchedule payment) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + contractId));

        payment.setContract(contract);

        List<PaymentSchedule> existingPayments = paymentScheduleRepository.findByContractIdOrderBySequenceNumber(contractId);
        int nextSequence = existingPayments.size() + 1;
        payment.setSequenceNumber(nextSequence);

        if (payment.getStatus() == null) {
            payment.setStatus(PaymentStatus.PENDING);
        }

        return paymentScheduleRepository.save(payment);
    }

    @Override
    public PaymentSchedule getById(Long id) {
        return paymentScheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));
    }

    @Override
    public List<PaymentSchedule> getAll() {
        return paymentScheduleRepository.findAll();
    }

    @Override
    @Transactional
    public PaymentSchedule update(Long id, PaymentSchedule payment) {
        PaymentSchedule existing = getById(id);
        payment.setId(id);
        payment.setContract(existing.getContract());
        payment.setSequenceNumber(existing.getSequenceNumber());
        payment.setCreatedAt(existing.getCreatedAt());
        return paymentScheduleRepository.save(payment);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        paymentScheduleRepository.deleteById(id);
    }

    // ========== RECHERCHE ==========

    @Override
    public List<PaymentSchedule> getByContract(Long contractId) {
        return paymentScheduleRepository.findByContractIdOrderBySequenceNumber(contractId);
    }

    @Override
    public List<PaymentSchedule> getByContractAndStatus(Long contractId, PaymentStatus status) {
        return paymentScheduleRepository.findByContractIdAndStatus(contractId, status);
    }

    @Override
    public List<PaymentSchedule> getByStatus(PaymentStatus status) {
        return paymentScheduleRepository.findByStatus(status);
    }

    // ========== GESTION STATUT ==========

    @Override
    @Transactional
    public PaymentSchedule updateStatus(Long id, PaymentStatus status) {
        PaymentSchedule payment = getById(id);
        payment.setStatus(status);
        if (status == PaymentStatus.PAID && payment.getPaidAt() == null) {
            payment.setPaidAt(LocalDateTime.now());
        }
        return paymentScheduleRepository.save(payment);
    }

    @Override
    @Transactional
    public PaymentSchedule markAsPaid(Long id, String invoiceNumber) {
        PaymentSchedule payment = getById(id);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
            payment.setInvoiceNumber(invoiceNumber);
        }
        return paymentScheduleRepository.save(payment);
    }

    @Override
    @Transactional
    public PaymentSchedule markAsOverdue(Long id) {
        PaymentSchedule payment = getById(id);
        payment.setStatus(PaymentStatus.OVERDUE);
        return paymentScheduleRepository.save(payment);
    }

    // ========== STATISTIQUES ==========

    @Override
    public Long countPendingPayments(Long contractId) {
        return paymentScheduleRepository.countByContractIdAndStatus(contractId, PaymentStatus.PENDING);
    }

    @Override
    public Long countPaidPayments(Long contractId) {
        return paymentScheduleRepository.countByContractIdAndStatus(contractId, PaymentStatus.PAID);
    }
}