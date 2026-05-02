package com.esprit.microservice.evaluation_pi.controller;

import com.esprit.microservice.evaluation_pi.controller.client.ClientEvaluationController;
import com.esprit.microservice.evaluation_pi.entities.Evaluation;
import com.esprit.microservice.evaluation_pi.entities.ReportReason;
import com.esprit.microservice.evaluation_pi.services.BadgeService;
import com.esprit.microservice.evaluation_pi.services.EvaluationService;
import com.esprit.microservice.evaluation_pi.services.VoteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientEvaluationController.class)
@TestPropertySource(properties = "spring.cloud.config.enabled=false")
public class ClientEvaluationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EvaluationService evaluationService;

    @MockBean
    private VoteService voteService;

    @MockBean
    private BadgeService badgeService;

    @Test
    void testEvaluateFreelancer_Success() throws Exception {
        Evaluation eval = new Evaluation();
        eval.setRatingGlobal(5.0);

        when(evaluationService.hasClientEvaluatedProject(any(), any())).thenReturn(false);
        when(evaluationService.createEvaluation(any(Evaluation.class))).thenReturn(eval);

        String json = "{\"ratingGlobal\": 5.0, \"comment\": \"Excellent travail\"}";

        mockMvc.perform(post("/api/client/evaluations/freelancer/khalil@esprit.tn")
                        .param("clientId", "client_1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    @Test
    void testGetFreelancerDetails_NotFound() throws Exception {
        when(evaluationService.getEvaluationsByEvaluatedId(anyString())).thenReturn(Collections.emptyList());
        when(badgeService.getUserBadges(anyString())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/client/evaluations/freelancer/unknown@esprit.tn/details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvaluations").value(0));
    }

    @Test
    void testVoteEvaluation_Forbidden_AlreadyVoted() throws Exception {
        when(voteService.hasUserVoted(anyLong(), anyString())).thenReturn(true);

        mockMvc.perform(post("/api/client/evaluations/evaluation/1/vote")
                        .param("clientId", "client_1")
                        .param("helpful", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Vous avez déjà voté"));
    }
}