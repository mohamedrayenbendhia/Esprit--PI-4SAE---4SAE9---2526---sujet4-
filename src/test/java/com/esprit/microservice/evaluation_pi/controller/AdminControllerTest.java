package com.esprit.microservice.evaluation_pi.controller;

import com.esprit.microservice.evaluation_pi.controller.admin.AdminEvaluationController;
import com.esprit.microservice.evaluation_pi.entities.Badge;
import com.esprit.microservice.evaluation_pi.services.BadgeService;
import com.esprit.microservice.evaluation_pi.services.EvaluationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminEvaluationController.class) // On cible uniquement ce contrôleur
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc; // Pour simuler les appels HTTP (GET, POST, etc.)

    @MockBean
    private EvaluationService evaluationService; // On simule le service

    @MockBean
    private BadgeService badgeService; // On simule le service

    @Test
    void testGetAllBadges_ShouldReturnList() throws Exception {
        // Given
        Badge b1 = new Badge();
        b1.setId(1L);
        b1.setName("Gold");

        when(badgeService.getAllBadges()).thenReturn(Arrays.asList(b1));

        // When & Then
        mockMvc.perform(get("/api/admin/evaluations/badges")) // Appel GET
                .andExpect(status().isOk()) // Attend un code 200
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)) // Attend du JSON
                .andExpect(jsonPath("$[0].name").value("Gold")); // Vérifie le contenu JSON
    }

    @Test
    void testDeleteBadge_ShouldReturnNoContent() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/admin/evaluations/badges/1")) // Appel DELETE
                .andExpect(status().isNoContent()); // Attend un code 204
    }

    @Test
    void testCreateBadge_ShouldReturnBadge() throws Exception {
        // Given
        String badgeJson = "{\"name\":\"Expert\", \"minScore\":15.0}";
        Badge savedBadge = new Badge();
        savedBadge.setName("Expert");

        when(badgeService.createBadge(any(Badge.class))).thenReturn(savedBadge);

        // When & Then
        mockMvc.perform(post("/api/admin/evaluations/badges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badgeJson)) // Envoie le JSON dans le corps
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Expert"));
    }
}