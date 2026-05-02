package com.microservice.module_portfolio.controllers;

import com.microservice.module_portfolio.dto.*;
import com.microservice.module_portfolio.services.PortfolioService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FreelancerPortfolioController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
public class FreelancerPortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortfolioService portfolioService;

    @MockBean
    private Authentication authentication;

    @MockBean
    private Claims claims;

    @Test
    void shouldCreatePortfolio() throws Exception {
        when(authentication.getPrincipal()).thenReturn(claims);
        when(claims.get("userId", String.class)).thenReturn("user-123");
        when(portfolioService.createPortfolio(any(PortfolioRequest.class))).thenReturn(new PortfolioResponse());

        // Correction : Ajout de "headline" qui est obligatoire
        String jsonRequest = "{\"title\": \"My Portfolio\", \"headline\": \"Expert Developer\"}";

        mockMvc.perform(post("/api/freelancer/portfolios")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldAddProject() throws Exception {
        when(portfolioService.addProject(eq(1L), any(ProjectRequest.class))).thenReturn(new ProjectResponse());

        // Correction : Utilisation de "title" au lieu de "name"
        String jsonRequest = "{\"title\": \"Project A\", \"description\": \"Description test\"}";

        mockMvc.perform(post("/api/freelancer/portfolios/1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldGetMyPortfolio() throws Exception {
        when(authentication.getPrincipal()).thenReturn(claims);
        when(claims.get("userId", String.class)).thenReturn("user-123");
        when(portfolioService.getByUserId("user-123")).thenReturn(new PortfolioResponse());

        mockMvc.perform(get("/api/freelancer/portfolios/me")
                        .principal(authentication))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDeleteProject() throws Exception {
        doNothing().when(portfolioService).deleteProject(1L);

        mockMvc.perform(delete("/api/freelancer/portfolios/projects/1"))
                .andExpect(status().isNoContent());

        verify(portfolioService).deleteProject(1L);
    }

    @Test
    void shouldToggleVisibility() throws Exception {
        when(portfolioService.toggleVisibility(1L)).thenReturn(new PortfolioResponse());

        mockMvc.perform(patch("/api/freelancer/portfolios/1/visibility"))
                .andExpect(status().isOk());
    }
}