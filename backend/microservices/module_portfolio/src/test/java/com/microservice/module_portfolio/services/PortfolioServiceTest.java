package com.microservice.module_portfolio.services;

import com.microservice.module_portfolio.dto.*;
import com.microservice.module_portfolio.entities.Portfolio;
import com.microservice.module_portfolio.entities.Project;
import com.microservice.module_portfolio.exceptions.DuplicateResourceException;
import com.microservice.module_portfolio.exceptions.ResourceNotFoundException;
import com.microservice.module_portfolio.repositories.PortfolioRepository;
import com.microservice.module_portfolio.repositories.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PortfolioServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private PortfolioService portfolioService;

    private Portfolio portfolio;
    private PortfolioRequest portfolioRequest;

    @BeforeEach
    void setUp() {
        portfolio = Portfolio.builder()
                .id(1L)
                .userId("user-123")
                .headline("Developer")
                .isPublic(true)
                .build();

        portfolioRequest = new PortfolioRequest();
        portfolioRequest.setUserId("user-123");
        portfolioRequest.setHeadline("Developer");
    }

    @Test
    void shouldCreatePortfolioSuccessfully() {
        when(portfolioRepository.existsByUserId("user-123")).thenReturn(false);
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

        PortfolioResponse response = portfolioService.createPortfolio(portfolioRequest);

        assertNotNull(response);
        assertEquals("user-123", response.getUserId());
        verify(portfolioRepository).save(any(Portfolio.class));
    }

    @Test
    void shouldThrowExceptionWhenPortfolioAlreadyExists() {
        when(portfolioRepository.existsByUserId("user-123")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> {
            portfolioService.createPortfolio(portfolioRequest);
        });

        verify(portfolioRepository, never()).save(any());
    }

    @Test
    void shouldGetPortfolioByUserId() {
        when(portfolioRepository.findByUserId("user-123")).thenReturn(Optional.of(portfolio));

        PortfolioResponse response = portfolioService.getByUserId("user-123");

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void shouldThrowNotFoundWhenPortfolioDoesNotExist() {
        when(portfolioRepository.findByUserId("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            portfolioService.getByUserId("unknown");
        });
    }

    @Test
    void shouldIncrementViews() {
        when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio));
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

        portfolio.setViewsCount(10); // Valeur initiale

        PortfolioResponse response = portfolioService.incrementViews(1L);

        assertNotNull(response);
        verify(portfolioRepository).save(argThat(p -> p.getViewsCount() == 11));
    }

    @Test
    void shouldDeleteProject() {
        when(projectRepository.existsById(100L)).thenReturn(true);
        doNothing().when(projectRepository).deleteById(100L);

        assertDoesNotThrow(() -> portfolioService.deleteProject(100L));

        verify(projectRepository).deleteById(100L);
    }
}