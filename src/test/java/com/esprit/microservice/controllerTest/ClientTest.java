package com.esprit.microservice.controllerTest;

import com.esprit.microservice.contrat_backend.controller.ClientContractController;
import com.esprit.microservice.contrat_backend.entities.Contract;
import com.esprit.microservice.contrat_backend.services.*;
import com.esprit.microservice.contrat_backend.ContratBackendApplication; // Remplace par le nom exact de ta classe main
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientContractController.class)
@ContextConfiguration(classes = ContratBackendApplication.class) // INDISPENSABLE car ton package est différent
public class ClientTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean private IContractService contractService;
    @MockBean private IPaymentService paymentService;
    @MockBean private IMilestoneService milestoneService;
    @MockBean private PdfGenerationService pdfService;
    @MockBean private CurrencyConversionService convService;

    @Test
    public void testCreateContract() throws Exception {
        Contract c = new Contract();
        c.setMissionTitle("Test Mission");

        when(contractService.create(any())).thenReturn(c);

        mockMvc.perform(post("/api/client/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(c)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.missionTitle").value("Test Mission"));
    }

    @Test
    public void testGetContracts() throws Exception {
        mockMvc.perform(get("/api/client/contracts/my-contracts")
                        .param("clientId", "123"))
                .andExpect(status().isOk());
    }
}