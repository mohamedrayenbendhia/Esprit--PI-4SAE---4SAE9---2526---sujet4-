package com.esprit.microservice.contrat_backend.controller;

import com.esprit.microservice.contrat_backend.dto.SignatureRequest;
import com.esprit.microservice.contrat_backend.entities.*;
import com.esprit.microservice.contrat_backend.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ContentDisposition;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/client/contracts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ClientContractController {

    private final IContractService contractService;
    private final IPaymentService paymentService;
    private final IMilestoneService milestoneService;
    private final PdfGenerationService pdfGenerationService;
    private final CurrencyService currencyService;
    private final QRCodeService qrCodeService;

    //  GESTION DES CONTRATS

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

        if (status != null) {
            return ResponseEntity.ok(contractService.getByClientIdAndStatus(clientId, status));
        }
        if (keyword != null && !keyword.isEmpty()) {
            return ResponseEntity.ok(contractService.searchByKeyword(clientId, keyword));
        }
        return ResponseEntity.ok(contractService.getByClientId(clientId));
    }

    @GetMapping("/by-status")
    public ResponseEntity<List<Contract>> getContractsByStatus(
            @RequestParam String clientId,
            @RequestParam ContractStatus status) {
        return ResponseEntity.ok(contractService.getByClientIdAndStatus(clientId, status));
    }

    @GetMapping("/by-type")
    public ResponseEntity<List<Contract>> getContractsByType(
            @RequestParam String clientId,
            @RequestParam ContractType type) {
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

    //  GÉNÉRATION PDF

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

            return ResponseEntity.ok().headers(headers).body(pdfContent);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Contract> updateContract(
            @PathVariable Long id,
            @RequestBody Contract contract) {
        return ResponseEntity.ok(contractService.update(id, contract));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContract(@PathVariable Long id) {
        contractService.delete(id);
        return ResponseEntity.noContent().build();
    }

    //  WORKFLOW CLIENT

    @PostMapping("/{id}/sign")
    public ResponseEntity<Contract> signContract(
            @PathVariable Long id,
            @RequestBody SignatureRequest signatureRequest) {
        return ResponseEntity.ok(contractService.signByClient(id, signatureRequest.getSignatureData()));
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

    //  GESTION DES PAIEMENTS

    @PostMapping("/{contractId}/payments")
    public ResponseEntity<PaymentSchedule> addPayment(
            @PathVariable Long contractId,
            @RequestBody PaymentSchedule payment) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.create(contractId, payment));
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
            @PathVariable Long paymentId,
            @RequestParam PaymentStatus status) {
        return ResponseEntity.ok(paymentService.updateStatus(paymentId, status));
    }

    @DeleteMapping("/payments/{paymentId}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long paymentId) {
        paymentService.delete(paymentId);
        return ResponseEntity.noContent().build();
    }

    //  GESTION DES JALONS

    @PostMapping("/{contractId}/milestones")
    public ResponseEntity<Milestone> addMilestone(
            @PathVariable Long contractId,
            @RequestBody Milestone milestone) {
        return ResponseEntity.status(HttpStatus.CREATED).body(milestoneService.create(contractId, milestone));
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
            @PathVariable Long milestoneId,
            @RequestParam String reason) {
        return ResponseEntity.ok(milestoneService.reject(milestoneId, reason));
    }

    @PutMapping("/milestones/{milestoneId}/status")
    public ResponseEntity<Milestone> updateMilestoneStatus(
            @PathVariable Long milestoneId,
            @RequestParam MilestoneStatus status) {
        return ResponseEntity.ok(milestoneService.updateStatus(milestoneId, status));
    }

    @DeleteMapping("/milestones/{milestoneId}")
    public ResponseEntity<Void> deleteMilestone(@PathVariable Long milestoneId) {
        milestoneService.delete(milestoneId);
        return ResponseEntity.noContent().build();
    }

    //  GESTION DES CLAUSES

    @PostMapping("/{contractId}/clauses")
    public ResponseEntity<CustomClause> addCustomClause(
            @PathVariable Long contractId,
            @RequestBody CustomClause clause) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contractService.addCustomClause(contractId, clause));
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

    //  CONVERSION DE DEVISE

    @GetMapping("/{id}/convert")
    public ResponseEntity<Map<String, Object>> convertAmount(
            @PathVariable Long id,
            @RequestParam String to) {
        try {
            Contract contract = contractService.getById(id);
            double original = contract.getTotalAmount().doubleValue();
            double converted = currencyService.convert(original, "EUR", to);

            return ResponseEntity.ok(Map.of(
                    "contractId",       id,
                    "original",         original,
                    "originalCurrency", "EUR",
                    "converted",        converted,
                    "targetCurrency",   to.toUpperCase()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    //  QR CODE SIGNATURE

    @GetMapping("/{id}/qrcode")
    public ResponseEntity<byte[]> getQRCode(@PathVariable Long id) {
        try {
            Contract contract = contractService.getById(id);
            String hash = contract.getClientSignatureHash();

            if (hash == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            byte[] qrImage = qrCodeService.generateQRCode(hash);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            return ResponseEntity.ok().headers(headers).body(qrImage);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    //  VÉRIFICATION SIGNATURE VIA QR

    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifySignature(
            @RequestParam String hash) {
        boolean found = contractService.existsByClientSignatureHash(hash);
        return ResponseEntity.ok(Map.of(
                "valid",   found,
                "hash",    hash,
                "message", found ? "Signature valide et authentique" : "Signature introuvable"
        ));
    }
}