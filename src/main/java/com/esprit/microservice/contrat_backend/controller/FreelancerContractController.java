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
@RequestMapping("/api/freelancer/contracts")
@RequiredArgsConstructor
@CrossOrigin(
        origins = "*",
        allowedHeaders = "*",
        exposedHeaders = "Content-Disposition",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
                RequestMethod.DELETE, RequestMethod.OPTIONS}
)
public class FreelancerContractController {

    private final IContractService          contractService;
    private final IPaymentService           paymentService;
    private final IMilestoneService         milestoneService;
    private final PdfGenerationService      pdfGenerationService;
    private final CurrencyConversionService currencyConversionService;

    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptions() {
        return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin",  "*")
                .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
                .header("Access-Control-Allow-Headers", "authorization, content-type, x-requested-with")
                .header("Access-Control-Max-Age",       "3600")
                .build();
    }

    // ── CONTRATS ─────────────────────────────────────────────────

    @GetMapping("/my-contracts")
    public ResponseEntity<List<Contract>> getMyContracts(@RequestParam String freelancerId) {
        return ResponseEntity.ok(contractService.getByFreelancerId(freelancerId));
    }

    @GetMapping("/by-status")
    public ResponseEntity<List<Contract>> getContractsByStatus(
            @RequestParam String freelancerId, @RequestParam ContractStatus status) {
        return ResponseEntity.ok(contractService.getByFreelancerIdAndStatus(freelancerId, status));
    }

    @GetMapping("/by-type")
    public ResponseEntity<List<Contract>> getContractsByType(
            @RequestParam String freelancerId, @RequestParam ContractType type) {
        return ResponseEntity.ok(contractService.getByFreelancerIdAndType(freelancerId, type));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Contract>> searchContracts(
            @RequestParam String freelancerId,
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(required = false) String keyword) {
        if (status != null)
            return ResponseEntity.ok(contractService.getByFreelancerIdAndStatus(freelancerId, status));
        if (keyword != null && !keyword.isEmpty())
            return ResponseEntity.ok(contractService.searchByKeywordForFreelancer(freelancerId, keyword));
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

    @GetMapping("/{id}")
    public ResponseEntity<Contract> getContractById(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.getById(id));
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
        return ResponseEntity.ok(contractService.signByFreelancer(id, body.get("signatureHash")));
    }

    @PostMapping("/{id}/sign-with-code")
    public ResponseEntity<?> signContractWithCode(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(contractService.signByFreelancerWithCode(id, body.get("signatureCode")));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/{id}/my-signature-code")
    public ResponseEntity<Map<String, String>> getFreelancerSignatureCode(@PathVariable Long id) {
        Contract contract = contractService.getById(id);
        Map<String, String> result = new HashMap<>();
        result.put("code",   contract.getFreelancerSignatureCode() != null
                ? contract.getFreelancerSignatureCode() : "");
        result.put("signed", contract.getFreelancerSignedAt() != null ? "true" : "false");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/request-modification")
    public ResponseEntity<Contract> requestModification(
            @PathVariable Long id, @RequestParam String reason) {
        return ResponseEntity.ok(contractService.requestModification(id, reason));
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<Contract> declineContract(
            @PathVariable Long id, @RequestParam String reason) {
        return ResponseEntity.ok(contractService.declineContract(id, reason));
    }

    // ── PAIEMENTS ────────────────────────────────────────────────

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

    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<PaymentSchedule> getPaymentDetails(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getById(paymentId));
    }

    // ── JALONS ───────────────────────────────────────────────────

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

    // ── CLAUSES ──────────────────────────────────────────────────

    @GetMapping("/{contractId}/clauses")
    public ResponseEntity<List<CustomClause>> getCustomClauses(@PathVariable Long contractId) {
        return ResponseEntity.ok(contractService.getCustomClauses(contractId));
    }
}