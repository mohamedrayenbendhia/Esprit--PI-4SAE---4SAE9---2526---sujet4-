package com.esprit.microservice.evaluation_pi.controller;

import com.esprit.microservice.evaluation_pi.controller.freelancer.FreelancerEvaluationController;
import com.esprit.microservice.evaluation_pi.entities.Evaluation;
import com.esprit.microservice.evaluation_pi.services.EvaluationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FreelancerEvaluationController.class)
@TestPropertySource(properties = "spring.cloud.config.enabled=false")
public class FreelancerEvaluationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EvaluationService evaluationService;

    @Test
    void testGetMyEvaluations_Success() throws Exception {
        Evaluation eval = new Evaluation();
        eval.setEvaluatedId("khalil@esprit.tn");

        when(evaluationService.getEvaluationsByEvaluatedId("khalil@esprit.tn"))
                .thenReturn(Arrays.asList(eval));

        mockMvc.perform(get("/api/freelancer/evaluations/my-evaluations")
                        .param("email", "khalil@esprit.tn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testAverageRating_Calculation() throws Exception {
        Evaluation e1 = new Evaluation(); e1.setRatingGlobal(5.0);
        Evaluation e2 = new Evaluation(); e2.setRatingGlobal(3.0);

        when(evaluationService.getEvaluationsByEvaluatedId("khalil@esprit.tn"))
                .thenReturn(Arrays.asList(e1, e2));

        mockMvc.perform(get("/api/freelancer/evaluations/average-rating/khalil@esprit.tn"))
                .andExpect(status().isOk())
                .andExpect(content().string("4.0"));
    }

    @Test
    void testRespondToEvaluation_Success() throws Exception {
        Evaluation eval = new Evaluation();
        eval.setId(1L);

        when(evaluationService.respondToEvaluation(eq(1L), anyString())).thenReturn(eval);
        // On mock aussi le retour pour le pushStatsUpdate interne
        when(evaluationService.getEvaluationsByEvaluatedId(anyString())).thenReturn(Collections.emptyList());

        String json = "{\"response\": \"Merci pour votre retour!\"}";

        mockMvc.perform(post("/api/freelancer/evaluations/evaluation/1/respond")
                        .param("freelancerEmail", "khalil@esprit.tn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    @Test
    void testEvaluateClient_InvalidEmail() throws Exception {
        mockMvc.perform(post("/api/freelancer/evaluations/evaluate-client/wrong-email")
                        .param("freelancerId", "free123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Email invalide"));
    }

    @Test
    void testStreamStats_InitialConnection() throws Exception {
        // Mock pour buildDetailedStats qui est appelé à l'ouverture du stream
        when(evaluationService.getEvaluationsByEvaluatedId(anyString())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/freelancer/evaluations/stats/khalil@esprit.tn/stream"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_EVENT_STREAM_VALUE));
    }
}