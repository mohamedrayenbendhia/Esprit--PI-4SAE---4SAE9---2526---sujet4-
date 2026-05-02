package com.smartfreelance.microservice.organizationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfreelance.microservice.organizationservice.dto.request.MatchingRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.OrganizationSummaryResponse;
import com.smartfreelance.microservice.organizationservice.exception.GlobalExceptionHandler;
import com.smartfreelance.microservice.organizationservice.service.MatchingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests pour MatchingController.
 * Correction : le endpoint est POST /api/organizations/matching (pas GET).
 */
@ExtendWith(MockitoExtension.class)
class MatchingControllerTest {

    @Mock private MatchingService matchingService;
    @InjectMocks private MatchingController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── POST /api/organizations/matching ─────────────────────────

    @Test
    void match_shouldReturn200_withResults() throws Exception {
        OrganizationSummaryResponse r1 = OrganizationSummaryResponse.builder().id("o1").name("TechCorp").build();
        OrganizationSummaryResponse r2 = OrganizationSummaryResponse.builder().id("o2").name("DigitalAgency").build();
        when(matchingService.match(any(MatchingRequest.class))).thenReturn(List.of(r1, r2));

        String requestJson = objectMapper.writeValueAsString(new MatchingRequest());

        mockMvc.perform(post("/api/organizations/matching")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value("o1"))
                .andExpect(jsonPath("$[1].id").value("o2"));

        verify(matchingService).match(any(MatchingRequest.class));
    }

    @Test
    void match_shouldReturn200_withEmptyResults() throws Exception {
        when(matchingService.match(any(MatchingRequest.class))).thenReturn(List.of());

        mockMvc.perform(post("/api/organizations/matching")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void match_shouldReturn400_whenBodyIsMissing() throws Exception {
        // Sans body JSON → Spring renvoie 400 (HttpMessageNotReadableException)
        mockMvc.perform(post("/api/organizations/matching"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(matchingService);
    }

    @Test
    void match_shouldDelegateRequestBodyToService() throws Exception {
        when(matchingService.match(any())).thenReturn(List.of());

        mockMvc.perform(post("/api/organizations/matching")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        verify(matchingService, times(1)).match(any(MatchingRequest.class));
    }
}
