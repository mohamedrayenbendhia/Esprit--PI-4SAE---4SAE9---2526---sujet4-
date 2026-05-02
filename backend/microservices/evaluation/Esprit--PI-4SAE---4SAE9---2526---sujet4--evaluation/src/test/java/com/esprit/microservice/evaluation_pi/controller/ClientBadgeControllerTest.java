package com.esprit.microservice.evaluation_pi.controller;

import com.esprit.microservice.evaluation_pi.controller.client.ClientBadgeController;
import com.esprit.microservice.evaluation_pi.entities.Badge;
import com.esprit.microservice.evaluation_pi.entities.UserBadge;
import com.esprit.microservice.evaluation_pi.services.BadgeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientBadgeController.class)
class ClientBadgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BadgeService badgeService; // On mocke le service pour isoler le controller

    @Test
    void testGetAllBadges_ShouldReturnList() throws Exception {
        // Given
        Badge b1 = new Badge();
        b1.setName("Top Rated");
        when(badgeService.getAllBadges()).thenReturn(Arrays.asList(b1));

        // When & Then
        mockMvc.perform(get("/api/client/badges"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("Top Rated"));
    }

    @Test
    void testGetFreelancerBadges_ShouldReturnUserBadges() throws Exception {
        // Given
        String freelancerId = "free789";
        UserBadge ub = new UserBadge();
        // On suppose que UserBadge a une relation ou un champ pour le nom du badge
        // Ajustez selon votre entité réelle

        when(badgeService.getUserBadges(freelancerId)).thenReturn(Arrays.asList(ub));

        // When & Then
        mockMvc.perform(get("/api/client/badges/freelancer/" + freelancerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testGetBadgeDetails_ShouldReturnBadge() throws Exception {
        // Given
        Long badgeId = 1L;
        Badge badge = new Badge();
        badge.setId(badgeId);
        badge.setName("Expert");

        when(badgeService.getBadgeById(badgeId)).thenReturn(badge);

        // When & Then
        mockMvc.perform(get("/api/client/badges/" + badgeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Expert"))
                .andExpect(jsonPath("$.id").value(1));
    }
}