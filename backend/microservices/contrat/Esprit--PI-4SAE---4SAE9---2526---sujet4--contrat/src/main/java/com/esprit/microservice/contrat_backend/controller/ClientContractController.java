package com.esprit.microservice.contrat_backend.controller;

import com.esprit.microservice.contrat_backend.entities.*;
import com.esprit.microservice.contrat_backend.services.CurrencyConversionService;
import com.esprit.microservice.contrat_backend.services.IContractService;
import com.esprit.microservice.contrat_backend.services.IPaymentService;
import com.esprit.microservice.contrat_backend.services.IMilestoneService;
import com.esprit.microservice.contrat_backend.services.PdfGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/client/contracts")
@RequiredArgsConstructor
public class ClientContractController {

    private final IContractService          contractService;
    private final IPaymentService           paymentService;
    private final IMilestoneService         milestoneService;
    private final PdfGenerationService      pdfGenerationService;
    private final CurrencyConversionService currencyConversionService;


    // ── CONTRATS ─────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<Contract> createContract(@RequestBody Contract contract) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contractService.create(contract));
    }

    @GetMapping("/my-contracts")
    public ResponseEntity<List<Contract>> getMyContracts(@RequestParam String clientId) {
        return ResponseEntity.ok(contractService.getByClientId(clientId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Contract>> searchContracts(
            @RequestParam String clientId,
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(required = false) String keyword) {
        if (status != null)
            return ResponseEntity.ok(contractService.getByClientIdAndStatus(clientId, status));
        if (keyword != null && !keyword.isEmpty())
            return ResponseEntity.ok(contractService.searchByKeyword(clientId, keyword));
        return ResponseEntity.ok(contractService.getByClientId(clientId));
    }

    @GetMapping("/by-status")
    public ResponseEntity<List<Contract>> getContractsByStatus(
            @RequestParam String clientId, @RequestParam ContractStatus status) {
        return ResponseEntity.ok(contractService.getByClientIdAndStatus(clientId, status));
    }

    @GetMapping("/by-type")
    public ResponseEntity<List<Contract>> getContractsByType(
            @RequestParam String clientId, @RequestParam ContractType type) {
        return ResponseEntity.ok(contractService.getByClientIdAndType(clientId, type));
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics(@RequestParam String clientId) {
        return ResponseEntity.ok(contractService.getClientStatistics(clientId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contract> getContractById(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Contract> updateContract(
            @PathVariable Long id, @RequestBody Contract contract) {
        return ResponseEntity.ok(contractService.update(id, contract));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContract(@PathVariable Long id) {
        contractService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── CONVERSION DEVISE ────────────────────────────────────────

    @GetMapping("/{id}/convert")
    public ResponseEntity<?> convertAmount(
            @PathVariable Long id,
            @RequestParam String to) {
        try {
            Contract contract = contractService.getById(id);

            if (contract.getTotalAmount() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Contract has no total amount defined"));
            }

            double amount = contract.getTotalAmount().doubleValue();
            // currency est un enum → .name() retourne le String "EUR", "USD", etc.
            String from = contract.getCurrency() != null
                    ? contract.getCurrency().name() : "EUR";

            Map<String, Object> result = currencyConversionService.convert(amount, from, to);
            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Conversion failed: " + e.getMessage()));
        }
    }

    // ── PDF ──────────────────────────────────────────────────────

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> generatePdf(@PathVariable Long id) {
        try {
            Contract contract   = contractService.getById(id);
            byte[]  pdfContent  = pdfGenerationService.generateContractPdf(contract);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("contract-" + contract.getContractNumber() + ".pdf")
                    .build());
            return ResponseEntity.ok().headers(headers).body(pdfContent);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── WORKFLOW SIGNATURES ──────────────────────────────────────

    @PostMapping("/{id}/sign")
    public ResponseEntity<Contract> signContract(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(contractService.signByClient(id, body.get("signatureHash")));
    }

    @PostMapping("/{id}/sign-with-code")
    public ResponseEntity<?> signContractWithCode(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(contractService.signByClientWithCode(id, body.get("signatureCode")));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/{id}/my-signature-code")
    public ResponseEntity<Map<String, String>> getClientSignatureCode(@PathVariable Long id) {
        Contract contract = contractService.getById(id);
        Map<String, String> result = new HashMap<>();
        result.put("code",   contract.getClientSignatureCode() != null
                ? contract.getClientSignatureCode() : "");
        result.put("signed", contract.getClientSignedAt() != null ? "true" : "false");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/send-to-freelancer")
    public ResponseEntity<Contract> sendToFreelancer(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.sendToFreelancer(id));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Contract> activateContract(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.activate(id));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Contract> completeContract(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.complete(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Contract> cancelContract(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.cancel(id));
    }

    // ── PAIEMENTS ────────────────────────────────────────────────

    @PostMapping("/{contractId}/payments")
    public ResponseEntity<PaymentSchedule> addPayment(
            @PathVariable Long contractId, @RequestBody PaymentSchedule payment) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.create(contractId, payment));
    }

    @GetMapping("/{contractId}/payments")
    public ResponseEntity<List<PaymentSchedule>> getPayments(@PathVariable Long contractId) {
        return ResponseEntity.ok(paymentService.getByContract(contractId));
    }

    @PutMapping("/payments/{paymentId}/mark-paid")
    public ResponseEntity<PaymentSchedule> markPaymentAsPaid(
            @PathVariable Long paymentId,
            @RequestParam(required = false) String invoiceNumber) {
        return ResponseEntity.ok(paymentService.markAsPaid(paymentId, invoiceNumber));
    }

    @PutMapping("/payments/{paymentId}/status")
    public ResponseEntity<PaymentSchedule> updatePaymentStatus(
            @PathVariable Long paymentId, @RequestParam PaymentStatus status) {
        return ResponseEntity.ok(paymentService.updateStatus(paymentId, status));
    }

    @DeleteMapping("/payments/{paymentId}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long paymentId) {
        paymentService.delete(paymentId);
        return ResponseEntity.noContent().build();
    }

    // ── JALONS ───────────────────────────────────────────────────

    @PostMapping("/{contractId}/milestones")
    public ResponseEntity<Milestone> addMilestone(
            @PathVariable Long contractId, @RequestBody Milestone milestone) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(milestoneService.create(contractId, milestone));
    }

    @GetMapping("/{contractId}/milestones")
    public ResponseEntity<List<Milestone>> getMilestones(@PathVariable Long contractId) {
        return ResponseEntity.ok(milestoneService.getByContract(contractId));
    }

    @PutMapping("/milestones/{milestoneId}/validate")
    public ResponseEntity<Milestone> validateMilestone(
            @PathVariable Long milestoneId,
            @RequestParam String validatorId,
            @RequestParam(required = false) String comment) {
        return ResponseEntity.ok(milestoneService.validate(milestoneId, validatorId, comment));
    }

    @PutMapping("/milestones/{milestoneId}/reject")
    public ResponseEntity<Milestone> rejectMilestone(
            @PathVariable Long milestoneId, @RequestParam String reason) {
        return ResponseEntity.ok(milestoneService.reject(milestoneId, reason));
    }

    @PutMapping("/milestones/{milestoneId}/status")
    public ResponseEntity<Milestone> updateMilestoneStatus(
            @PathVariable Long milestoneId, @RequestParam MilestoneStatus status) {
        return ResponseEntity.ok(milestoneService.updateStatus(milestoneId, status));
    }

    @DeleteMapping("/milestones/{milestoneId}")
    public ResponseEntity<Void> deleteMilestone(@PathVariable Long milestoneId) {
        milestoneService.delete(milestoneId);
        return ResponseEntity.noContent().build();
    }

    // ── CLAUSES ──────────────────────────────────────────────────

    @PostMapping("/{contractId}/clauses")
    public ResponseEntity<CustomClause> addCustomClause(
            @PathVariable Long contractId, @RequestBody CustomClause clause) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contractService.addCustomClause(contractId, clause));
    }

    @GetMapping("/{contractId}/clauses")
    public ResponseEntity<List<CustomClause>> getCustomClauses(@PathVariable Long contractId) {
        return ResponseEntity.ok(contractService.getCustomClauses(contractId));
    }

    @DeleteMapping("/clauses/{clauseId}")
    public ResponseEntity<Void> deleteCustomClause(@PathVariable Long clauseId) {
        contractService.deleteCustomClause(clauseId);
        return ResponseEntity.noContent().build();
    }
}