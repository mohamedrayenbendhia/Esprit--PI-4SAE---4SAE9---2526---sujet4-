package com.smartfreelance.microservice.complaintservice.controller;

import com.smartfreelance.microservice.complaintservice.entity.NpsSurvey;
import com.smartfreelance.microservice.complaintservice.dto.advanced.NpsStatsResponse;
import com.smartfreelance.microservice.complaintservice.dto.advanced.NpsResponseRequest;
import com.smartfreelance.microservice.complaintservice.service.NpsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NpsControllerTest {
	@Mock private NpsService npsService;
	@InjectMocks private NpsController controller;

	private MockMvc mockMvc;
	private ObjectMapper mapper = new ObjectMapper();

	@BeforeEach void setUp() { mockMvc = MockMvcBuilders.standaloneSetup(controller).build(); }

	@Test
	void respond_shouldReturnOk() throws Exception {
		NpsResponseRequest req = new NpsResponseRequest();
		req.setScore(8);
		when(npsService.respond(anyString(), any(), anyString())).thenReturn(new NpsSurvey());
		mockMvc.perform(post("/api/nps/comp1/respond").contentType("application/json").content(mapper.writeValueAsString(req)).header("X-User-Id","u1"))
				.andExpect(status().isOk());
	}

	@Test
	void getStats_shouldReturnOk() throws Exception {
		when(npsService.getStats()).thenReturn(new NpsStatsResponse());
		mockMvc.perform(get("/api/nps/stats")).andExpect(status().isOk());
	}
}
