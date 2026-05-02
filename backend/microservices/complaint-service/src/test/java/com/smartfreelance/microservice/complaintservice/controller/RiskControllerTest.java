package com.smartfreelance.microservice.complaintservice.controller;

import com.smartfreelance.microservice.complaintservice.dto.advanced.UserRiskProfileResponse;
import com.smartfreelance.microservice.complaintservice.service.RiskScoringService;
import com.smartfreelance.microservice.complaintservice.service.SanctionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RiskControllerTest {
	@Mock private RiskScoringService riskService;
	@Mock private SanctionService sanctionService;
	@InjectMocks private RiskController controller;

	private MockMvc mockMvc;
	private ObjectMapper mapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@Test
	void getProfile_shouldReturnOk() throws Exception {
		when(riskService.getProfile("u1")).thenReturn(new UserRiskProfileResponse());
		mockMvc.perform(get("/api/risk/users/u1")).andExpect(status().isOk());
	}

	@Test
	void getHighRisk_shouldReturnList() throws Exception {
		when(riskService.getHighRiskUsers()).thenReturn(List.of(new UserRiskProfileResponse()));
		mockMvc.perform(get("/api/risk/high-risk")).andExpect(status().isOk());
	}
}
