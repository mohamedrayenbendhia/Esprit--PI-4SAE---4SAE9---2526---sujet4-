package com.esprit.microservice.contrat_backend.controller;

import com.esprit.microservice.contrat_backend.entities.*;
import com.esprit.microservice.contrat_backend.services.IContractService;
import com.esprit.microservice.contrat_backend.services.IPaymentService;
import com.esprit.microservice.contrat_backend.services.IMilestoneService;
import com.esprit.microservice.contrat_backend.services.PdfGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ContentDisposition;

import java.util.List;

@RestController
@RequestMapping("/api/freelancer/contracts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FreelancerContractController {

    private final IContractService contractService;
    private final IPaymentService paymentService;
    private final IMilestoneService milestoneService;
    private final PdfGenerationService pdfGenerationService;

    // ========== CONSULTATION DES CONTRATS ==========

    @GetMapping("/my-contracts")
    public ResponseEntity<List<Contract>> getMyContracts(@RequestParam String freelancerId) {
        return ResponseEntity.ok(contractService.getByFreelancerId(freelancerId));
    }

    @GetMapping("/by-status")
    public ResponseEntity<List<Contract>> getContractsByStatus(
            @RequestParam String freelancerId,
            @RequestParam ContractStatus status) {
        return ResponseEntity.ok(contractService.getByFreelancerIdAndStatus(freelancerId, status));
    }

    @GetMapping("/by-type")
    public ResponseEntity<List<Contract>> getContractsByType(
            @RequestParam String freelancerId,
            @RequestParam ContractType type) {
        return ResponseEntity.ok(contractService.getByFreelancerIdAndType(freelancerId, type));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Contract>> searchContracts(
            @RequestParam String freelancerId,
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(required = false) String keyword) {

        if (status != null) {
            return ResponseEntity.ok(contractService.getByFreelancerIdAndStatus(freelancerId, status));
        }
        if (keyword != null && !keyword.isEmpty()) {
            return ResponseEntity.ok(contractService.searchByKeywordForFreelancer(freelancerId, keyword));
        }
        return ResponseEntity.ok(contractService.getByFreelancerId(freelancerId));
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics(@RequestParam String freelancerId) {
        return ResponseEntity.ok(contractService.getFreelancerStatistics(freelancerId));
    }

    @GetMapping("/earnings")
    public ResponseEntity<?> getEarnings(@RequestParam String freelancerId) {
        return ResponseEntity.ok(contractService.getFreelancerEarnings(freelancerId));
    }

    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<PaymentSchedule> getPaymentDetails(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getById(paymentId));
    }

    @GetMapping("/milestones/{milestoneId}")
    public ResponseEntity<Milestone> getMilestoneDetails(@PathVariable Long milestoneId) {
        return ResponseEntity.ok(milestoneService.getById(milestoneId));
    }

    @PutMapping("/milestones/{milestoneId}/start")
    public ResponseEntity<Milestone> startMilestone(@PathVariable Long milestoneId) {
        return ResponseEntity.ok(milestoneService.updateStatus(milestoneId, MilestoneStatus.IN_PROGRESS));
    }

    @PutMapping("/milestones/{milestoneId}/submit-for-validation")
    public ResponseEntity<Milestone> submitForValidation(@PathVariable Long milestoneId) {
        return ResponseEntity.ok(milestoneService.updateStatus(milestoneId, MilestoneStatus.AWAITING_VALIDATION));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contract> getContractById(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.getById(id));
    }

    // ========== GÉNÉRATION PDF ==========
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> generatePdf(@PathVariable Long id) {
        try {
            Contract contract = contractService.getById(id);
            byte[] pdfContent = pdfGenerationService.generateContractPdf(contract);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("contract-" + contract.getContractNumber() + ".pdf")
                    .build());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfContent);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== WORKFLOW FREELANCER ==========

    @PostMapping("/{id}/sign")
    public ResponseEntity<Contract> signContract(
            @PathVariable Long id,
            @RequestParam String signatureHash) {
        return ResponseEntity.ok(contractService.signByFreelancer(id, signatureHash));
    }

    @PostMapping("/{id}/request-modification")
    public ResponseEntity<Contract> requestModification(
            @PathVariable Long id,
            @RequestParam String reason) {
        return ResponseEntity.ok(contractService.requestModification(id, reason));
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<Contract> declineContract(
            @PathVariable Long id,
            @RequestParam String reason) {
        return ResponseEntity.ok(contractService.declineContract(id, reason));
    }

    // ========== PAIEMENTS ==========

    @GetMapping("/{contractId}/payments")
    public ResponseEntity<List<PaymentSchedule>> getPayments(@PathVariable Long contractId) {
        return ResponseEntity.ok(paymentService.getByContract(contractId));
    }

    @GetMapping("/{contractId}/payments/pending")
    public ResponseEntity<List<PaymentSchedule>> getPendingPayments(@PathVariable Long contractId) {
        return ResponseEntity.ok(paymentService.getByContractAndStatus(contractId, PaymentStatus.PENDING));
    }

    @GetMapping("/{contractId}/payments/paid")
    public ResponseEntity<List<PaymentSchedule>> getPaidPayments(@PathVariable Long contractId) {
        return ResponseEntity.ok(paymentService.getByContractAndStatus(contractId, PaymentStatus.PAID));
    }

    // ========== JALONS ==========

    @GetMapping("/{contractId}/milestones")
    public ResponseEntity<List<Milestone>> getMilestones(@PathVariable Long contractId) {
        return ResponseEntity.ok(milestoneService.getByContract(contractId));
    }

    @GetMapping("/{contractId}/milestones/pending")
    public ResponseEntity<List<Milestone>> getPendingMilestones(@PathVariable Long contractId) {
        return ResponseEntity.ok(milestoneService.getByContractAndStatus(contractId, MilestoneStatus.PENDING));
    }

    @GetMapping("/{contractId}/milestones/in-progress")
    public ResponseEntity<List<Milestone>> getInProgressMilestones(@PathVariable Long contractId) {
        return ResponseEntity.ok(milestoneService.getByContractAndStatus(contractId, MilestoneStatus.IN_PROGRESS));
    }

    // ========== CLAUSES ==========

    @GetMapping("/{contractId}/clauses")
    public ResponseEntity<List<CustomClause>> getCustomClauses(@PathVariable Long contractId) {
        return ResponseEntity.ok(contractService.getCustomClauses(contractId));
    }
}