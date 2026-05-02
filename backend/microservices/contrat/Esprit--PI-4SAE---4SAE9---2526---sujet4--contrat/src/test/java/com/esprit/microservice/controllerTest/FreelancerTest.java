package com.esprit.microservice.controllerTest; // Ton package de test exact

import com.esprit.microservice.contrat_backend.controller.FreelancerContractController;
import com.esprit.microservice.contrat_backend.entities.Contract;
import com.esprit.microservice.contrat_backend.entities.ContractStatus;
import com.esprit.microservice.contrat_backend.services.*;
import com.esprit.microservice.contrat_backend.ContratBackendApplication; // Vérifie ce nom (ta classe Main)
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FreelancerContractController.class)
@ContextConfiguration(classes = ContratBackendApplication.class) // Force la liaison avec l'application
public class FreelancerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private IContractService contractService;
    @MockBean private IPaymentService paymentService;
    @MockBean private IMilestoneService milestoneService;
    @MockBean private PdfGenerationService pdfGenerationService;
    @MockBean private CurrencyConversionService currencyConversionService;

    @Test
    @DisplayName("GET /my-contracts - Succès")
    void getMyContracts_Success() throws Exception {
        String freelancerId = "F-123";
        when(contractService.getByFreelancerId(freelancerId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/freelancer/contracts/my-contracts")
                        .param("freelancerId", freelancerId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /sign - Signature du contrat")
    void signContract_Success() throws Exception {
        Long contractId = 1L;
        Map<String, String> body = Map.of("signatureHash", "xyz123");

        Contract contract = new Contract();
        contract.setId(contractId);
        contract.setStatus(ContractStatus.FULLY_SIGNED);

        when(contractService.signByFreelancer(eq(contractId), any())).thenReturn(contract);

        mockMvc.perform(post("/api/freelancer/contracts/{id}/sign", contractId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULLY_SIGNED"));
    }

    @Test
    @DisplayName("GET /pdf - Vérification PDF")
    void generatePdf_ReturnsFile() throws Exception {
        Long contractId = 1L;
        Contract contract = new Contract();
        contract.setContractNumber("2024-ABC");

        when(contractService.getById(contractId)).thenReturn(contract);
        when(pdfGenerationService.generateContractPdf(any())).thenReturn(new byte[]{1,2,3});

        mockMvc.perform(get("/api/freelancer/contracts/{id}/pdf", contractId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE));
    }
}