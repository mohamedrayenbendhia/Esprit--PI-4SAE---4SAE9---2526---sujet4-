package com.smartfreelance.microservice.complaintservice.controller;

import com.smartfreelance.microservice.complaintservice.dto.advanced.CreateSlaRuleRequest;
import com.smartfreelance.microservice.complaintservice.dto.advanced.SlaRuleResponse;
import com.smartfreelance.microservice.complaintservice.dto.advanced.SlaTrackingResponse;
import com.smartfreelance.microservice.complaintservice.service.SlaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SlaControllerTest {
	@Mock private SlaService slaService;
	@InjectMocks private SlaController controller;

	private MockMvc mockMvc;
	private ObjectMapper mapper = new ObjectMapper();

	@BeforeEach void setUp() { mockMvc = MockMvcBuilders.standaloneSetup(controller).build(); }

	@Test
	void getTracking_shouldReturnOk() throws Exception {
		when(slaService.getTracking("c1")).thenReturn(new SlaTrackingResponse());
		mockMvc.perform(get("/api/sla/tracking/c1")).andExpect(status().isOk());
	}
}
