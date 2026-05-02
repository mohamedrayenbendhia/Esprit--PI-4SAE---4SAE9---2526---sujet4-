package com.smartfreelance.microservice.complaintservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfreelance.microservice.complaintservice.dto.advanced.CreateTemplateRequest;
import com.smartfreelance.microservice.complaintservice.dto.advanced.ResponseTemplateResponse;
import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.service.ResponseTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ResponseTemplateControllerTest {

	@Mock
	private ResponseTemplateService templateService;

	@InjectMocks
	private ResponseTemplateController controller;

	private MockMvc mockMvc;
	private ObjectMapper mapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@Test
	void getAll_shouldReturnList() throws Exception {
		ResponseTemplateResponse r = new ResponseTemplateResponse();
		when(templateService.getAll()).thenReturn(List.of(r));

		mockMvc.perform(get("/api/response-templates"))
				.andExpect(status().isOk());
	}

	@Test
	void create_shouldReturnCreated() throws Exception {
		CreateTemplateRequest req = new CreateTemplateRequest();
		req.setTitle("Late delivery template");
		req.setContent("We apologize for the inconvenience.");
		req.setCategory(Complaint.ComplaintCategory.PAYMENT_ISSUE);
		ResponseTemplateResponse resp = new ResponseTemplateResponse();
		when(templateService.create(any(), any())).thenReturn(resp);

		mockMvc.perform(post("/api/response-templates")
						.contentType("application/json")
						.content(mapper.writeValueAsString(req))
						.header("X-User-Id", "admin-1"))
				.andExpect(status().isCreated());

		verify(templateService).create(any(), eq("admin-1"));
	}

	@Test
	void delete_shouldReturnNoContent() throws Exception {
		doNothing().when(templateService).delete("t1");

		mockMvc.perform(delete("/api/response-templates/t1"))
				.andExpect(status().isNoContent());

		verify(templateService).delete("t1");
	}

	@Test
	void recordUsage_shouldReturnOk() throws Exception {
		ResponseTemplateResponse resp = new ResponseTemplateResponse();
		when(templateService.recordUsage("t1")).thenReturn(resp);

		mockMvc.perform(post("/api/response-templates/t1/use"))
				.andExpect(status().isOk());
	}
}
