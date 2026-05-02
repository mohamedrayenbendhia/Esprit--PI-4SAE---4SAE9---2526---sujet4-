package com.esprit.microservice.evaluation_pi.controller;

import com.esprit.microservice.evaluation_pi.controller.freelancer.FreelancerBadgeController;
import com.esprit.microservice.evaluation_pi.entities.Badge;
import com.esprit.microservice.evaluation_pi.entities.UserBadge;
import com.esprit.microservice.evaluation_pi.services.BadgeService;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FreelancerBadgeController.class)
@TestPropertySource(properties = "spring.cloud.config.enabled=false")
public class FreelancerBadgeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BadgeService badgeService;

    @MockBean
    private EvaluationService evaluationService;

    @Test
    void testGetMyBadges_Success() throws Exception {
        String email = "khalil@esprit.tn";
        UserBadge ub = new UserBadge();

        when(badgeService.getUserBadges(email)).thenReturn(Arrays.asList(ub));

        mockMvc.perform(get("/api/freelancer/badges/my-badges/" + email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testGetBadgeProgress_Calculation() throws Exception {
        Long badgeId = 1L;
        String email = "khalil@esprit.tn";

        Badge badge = new Badge();
        badge.setId(badgeId);
        badge.setMinScore(4.0);
        // Correction ici : On utilise un int (pas de L) car setMinProjects attend un Integer
        badge.setMinProjects(10);

        when(badgeService.getBadgeById(badgeId)).thenReturn(badge);
        when(evaluationService.calculateUserAverageRating(email)).thenReturn(2.0);
        // Ici on garde le L car countUserEvaluations retourne probablement un Long
        when(evaluationService.countUserEvaluations(email)).thenReturn(2L);
        when(badgeService.getUserBadges(email)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/freelancer/badges/badge/" + badgeId + "/progress/" + email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreProgress").value(50.0))
                .andExpect(jsonPath("$.projectsProgress").value(20.0))
                .andExpect(jsonPath("$.overallProgress").value(35.0));
    }

    @Test
    void testGetAllBadgesWithProgress() throws Exception {
        Badge b = new Badge();
        b.setId(1L);
        b.setMinScore(5.0);
        b.setMinProjects(1); // Correction ici : Integer (pas de L)

        when(badgeService.getAllBadges()).thenReturn(Arrays.asList(b));
        when(evaluationService.calculateUserAverageRating(anyString())).thenReturn(5.0);
        when(evaluationService.countUserEvaluations(anyString())).thenReturn(1L);

        UserBadge ub = new UserBadge();
        ub.setBadge(b);
        when(badgeService.getUserBadges(anyString())).thenReturn(Arrays.asList(ub));

        mockMvc.perform(get("/api/freelancer/badges/all-with-progress/khalil@esprit.tn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isObtained").value(true));
    }
}