package com.microservice.module_portfolio.controllers;

import com.microservice.module_portfolio.dto.PortfolioResponse;
import com.microservice.module_portfolio.services.PortfolioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ClientPortfolioController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
public class ClientPortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortfolioService portfolioService;

    @Test
    void shouldGetByUserIdWhenPublic() throws Exception {
        String userId = "user123";
        PortfolioResponse response = new PortfolioResponse();
        response.setId(1L);
        response.setPublic(true); // Cas portfolio public

        when(portfolioService.getByUserId(userId)).thenReturn(response);

        mockMvc.perform(get("/api/client/portfolios/user/{userId}", userId))
                .andExpect(status().isOk());

        verify(portfolioService).incrementViews(1L);
    }

    @Test
    void shouldReturnForbiddenWhenPortfolioPrivate() throws Exception {
        String userId = "user123";
        PortfolioResponse response = new PortfolioResponse();
        response.setPublic(false); // Cas portfolio privé

        when(portfolioService.getByUserId(userId)).thenReturn(response);

        mockMvc.perform(get("/api/client/portfolios/user/{userId}", userId))
                .andExpect(status().isForbidden());

        // On vérifie que les vues ne sont PAS incrémentées si c'est privé
        verify(portfolioService, never()).incrementViews(anyLong());
    }

    @Test
    void shouldGetAllPublic() throws Exception {
        when(portfolioService.getAllPublic()).thenReturn(List.of(new PortfolioResponse()));

        mockMvc.perform(get("/api/client/portfolios"))
                .andExpect(status().isOk());

        verify(portfolioService).getAllPublic();
    }
}