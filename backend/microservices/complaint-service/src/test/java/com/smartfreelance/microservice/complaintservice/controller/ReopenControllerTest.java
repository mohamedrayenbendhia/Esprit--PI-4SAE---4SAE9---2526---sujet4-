package com.smartfreelance.microservice.complaintservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfreelance.microservice.complaintservice.dto.advanced.ReopenComplaintRequest;
import com.smartfreelance.microservice.complaintservice.dto.ComplaintDTO;
import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.mapper.ComplaintMapper;
import com.smartfreelance.microservice.complaintservice.service.ReopenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReopenControllerTest {
	@Mock private ReopenService reopenService;
	@Mock private ComplaintMapper complaintMapper;
	@InjectMocks private ReopenController controller;

	private MockMvc mockMvc;
	private ObjectMapper mapper = new ObjectMapper();

	@BeforeEach void setUp() { mockMvc = MockMvcBuilders.standaloneSetup(controller).build(); }

	@Test
	void reopen_shouldReturnOk() throws Exception {
		ReopenComplaintRequest req = new ReopenComplaintRequest(); req.setReason("Need more info");
		Complaint reopened = new Complaint(); reopened.setId("c1");
		ComplaintDTO dto = new ComplaintDTO(); dto.setId("c1");

		when(reopenService.reopen("c1", "user1", req.getReason())).thenReturn(reopened);
		when(complaintMapper.toDTO(reopened)).thenReturn(dto);

		mockMvc.perform(post("/api/complaints/c1/reopen")
						.header("X-User-Id", "user1")
						.contentType("application/json")
						.content(mapper.writeValueAsString(req)))
				.andExpect(status().isOk());
	}
}
