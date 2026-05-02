package com.smartfreelance.microservice.complaintservice.controller;

import com.smartfreelance.microservice.complaintservice.dto.advanced.MediationSessionResponse;
import com.smartfreelance.microservice.complaintservice.dto.advanced.OpenMediationRequest;
import com.smartfreelance.microservice.complaintservice.service.MediationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MediationControllerTest {
	@Mock private MediationService mediationService;
	@InjectMocks private MediationController controller;

	private MockMvc mockMvc;
	private ObjectMapper mapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@Test
	void openSession_shouldReturnCreated() throws Exception {
		OpenMediationRequest req = new OpenMediationRequest();
		when(mediationService.openSession(anyString(), anyString(), any())).thenReturn(new MediationSessionResponse());

		mockMvc.perform(post("/api/mediation/comp1/open")
						.contentType("application/json")
						.content(mapper.writeValueAsString(req))
						.header("X-User-Id", "admin1"))
				.andExpect(status().isCreated());
	}

	@Test
	void getSession_shouldReturnOk() throws Exception {
		when(mediationService.getSession("comp1")).thenReturn(new MediationSessionResponse());
		mockMvc.perform(get("/api/mediation/comp1")).andExpect(status().isOk());
	}
}
