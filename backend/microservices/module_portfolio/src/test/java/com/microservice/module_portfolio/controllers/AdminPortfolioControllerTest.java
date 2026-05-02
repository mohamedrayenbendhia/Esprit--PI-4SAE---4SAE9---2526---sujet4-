package com.microservice.module_portfolio.controllers;

import com.microservice.module_portfolio.dto.PortfolioResponse;
import com.microservice.module_portfolio.services.PortfolioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

// On importe tout Mockito et MockMvc statiquement pour plus de clarté
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminPortfolioController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
public class AdminPortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortfolioService portfolioService;

    @Test
    @DisplayName("Admin : Récupérer tous les portfolios")
    void shouldGetAllPortfolios() throws Exception {
        when(portfolioService.getAll()).thenReturn(List.of(new PortfolioResponse()));

        mockMvc.perform(get("/api/admin/portfolios")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(portfolioService, times(1)).getAll();
    }

    @Test
    @DisplayName("Admin : Récupérer un portfolio par ID")
    void shouldGetPortfolioById() throws Exception {
        Long id = 1L;
        PortfolioResponse response = new PortfolioResponse();
        when(portfolioService.getById(id)).thenReturn(response);

        mockMvc.perform(get("/api/admin/portfolios/{id}", id))
                .andExpect(status().isOk());

        verify(portfolioService).getById(id);
    }

    @Test
    @DisplayName("Admin : Récupérer par UserId")
    void shouldGetPortfolioByUserId() throws Exception {
        String userId = "user-123";
        when(portfolioService.getByUserId(userId)).thenReturn(new PortfolioResponse());

        mockMvc.perform(get("/api/admin/portfolios/user/{userId}", userId))
                .andExpect(status().isOk());

        verify(portfolioService).getByUserId(userId);
    }

    @Test
    @DisplayName("Admin : Supprimer un portfolio")
    void shouldDeletePortfolio() throws Exception {
        Long id = 1L;
        // doNothing est utilisé car la méthode est probablement void
        doNothing().when(portfolioService).deletePortfolio(id);

        mockMvc.perform(delete("/api/admin/portfolios/{id}", id))
                .andExpect(status().isNoContent());

        verify(portfolioService).deletePortfolio(id);
    }
}