package com.smartfreelance.microservice.complaintservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfreelance.microservice.complaintservice.dto.InvolveReportedRequest;
import com.smartfreelance.microservice.complaintservice.dto.SupportMessageDTO;
import com.smartfreelance.microservice.complaintservice.dto.SupportMessageRequestDTO;
import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.entity.SupportMessage;
import com.smartfreelance.microservice.complaintservice.entity.SupportMessage.ConversationType;
import com.smartfreelance.microservice.complaintservice.mapper.SupportMessageMapper;
import com.smartfreelance.microservice.complaintservice.notification.ComplaintNotificationService;
import com.smartfreelance.microservice.complaintservice.repository.SupportMessageRepository;
import com.smartfreelance.microservice.complaintservice.service.ComplaintService;
import com.smartfreelance.microservice.complaintservice.service.SupportMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
// ...existing imports...

import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class SupportMessageControllerTest {

	@Mock private SupportMessageService messageService;
	@Mock private SupportMessageMapper messageMapper;
	@Mock private ComplaintService complaintService;
	@Mock private ComplaintNotificationService notificationService;
	@Mock private SupportMessageRepository messageRepository;
	@Mock private WebClient webClient;
	@Mock private com.smartfreelance.microservice.complaintservice.service.SlaService slaService;

	@InjectMocks private SupportMessageController controller;

	private MockMvc mockMvc;
	private final ObjectMapper mapper = new ObjectMapper();

	private Complaint complaint;

	@BeforeEach
	void setUp() throws Exception {
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

		complaint = new Complaint();
		complaint.setId("c-1");
		complaint.setReporterId("user-1");
		complaint.setReportedUserId("reported-1");
		complaint.setAssignedToId("agent-1");
		complaint.setStatus(Complaint.Status.OPEN);

		// inject mocked webClient to avoid real HTTP calls in resolveSenderName
		Field f = SupportMessageController.class.getDeclaredField("userServiceClient");
		f.setAccessible(true);
		f.set(controller, webClient);
	}

	@Test
	void createMessage_supportAgentHappyPath_shouldReturnCreatedAndNotify() throws Exception {
		SupportMessageRequestDTO req = new SupportMessageRequestDTO();
		req.setComplaintId("c-1");
		req.setContent("Hello from agent");
		req.setConversationType(ConversationType.COMPLAINANT);

		when(complaintService.getComplaintById("c-1")).thenReturn(complaint);

		SupportMessage created = SupportMessage.builder()
				.id("m-1")
				.complaintId("c-1")
				.content("Hello from agent")
				.conversationType(ConversationType.COMPLAINANT)
				.senderType(SupportMessage.SenderType.SUPPORT)
				.createdAt(LocalDateTime.now())
				.build();

		SupportMessageDTO dto = new SupportMessageDTO();
		dto.setId("m-1");

		when(messageMapper.toEntity(any())).thenReturn(SupportMessage.builder().id("m-1").conversationType(ConversationType.COMPLAINANT).build());
		when(messageService.createMessage(any())).thenReturn(created);
		when(messageMapper.toDTO(created)).thenReturn(dto);


		MvcResult mr = mockMvc.perform(post("/api/support-messages")
					.contentType("application/json")
					.content(mapper.writeValueAsString(req))
					.header("X-User-Id", "agent-1")
					.header("X-User-Role", "SUPPORT_AGENT"))
			.andExpect(status().isCreated())
			.andReturn();

		String body = mr.getResponse().getContentAsString();
		SupportMessageDTO resp = mapper.readValue(body, SupportMessageDTO.class);
		assertThat(resp).isNotNull();
		assertThat(resp.getId()).isEqualTo("m-1");

		verify(messageService).createMessage(any());
		verify(notificationService).handle(any());
	}

	@Test
	void createMessage_forbiddenWhenComplaintClosed() throws Exception {
		SupportMessageRequestDTO req = new SupportMessageRequestDTO();
		req.setComplaintId("c-1");
		req.setContent("Hello");

		complaint.setStatus(Complaint.Status.CLOSED);
		when(complaintService.getComplaintById("c-1")).thenReturn(complaint);


		MvcResult mr = mockMvc.perform(post("/api/support-messages")
					.contentType("application/json")
					.content(mapper.writeValueAsString(req))
					.header("X-User-Id", "user-1")
					.header("X-User-Role", "CLIENT"))
			.andExpect(status().isForbidden())
			.andReturn();

		String body = mr.getResponse().getContentAsString();
		java.util.Map<String, Object> map = mapper.readValue(body, java.util.Map.class);
		assertThat(map).containsKey("error");
	}

	@Test
	void createMessage_agentNotAssigned_shouldBeForbidden() throws Exception {
		SupportMessageRequestDTO req = new SupportMessageRequestDTO();
		req.setComplaintId("c-1");
		req.setContent("Hello");

		when(complaintService.getComplaintById("c-1")).thenReturn(complaint);


		MvcResult mr2 = mockMvc.perform(post("/api/support-messages")
					.contentType("application/json")
					.content(mapper.writeValueAsString(req))
					.header("X-User-Id", "agent-2")
					.header("X-User-Role", "SUPPORT_AGENT"))
			.andExpect(status().isForbidden())
			.andReturn();

		String body2 = mr2.getResponse().getContentAsString();
		java.util.Map<String, Object> map2 = mapper.readValue(body2, java.util.Map.class);
		assertThat(map2.get("error")).isEqualTo("Vous ne pouvez répondre que sur les réclamations qui vous sont assignées.");
	}

	@Test
	void getMessageById_shouldReturnDTO() throws Exception {
		SupportMessage m = SupportMessage.builder().id("m-2").content("ok").build();
		SupportMessageDTO dto = new SupportMessageDTO(); dto.setId("m-2");

		when(messageService.getMessageById("m-2")).thenReturn(m);
		when(messageMapper.toDTO(m)).thenReturn(dto);


		MvcResult mr3 = mockMvc.perform(get("/api/support-messages/m-2"))
				.andExpect(status().isOk())
				.andReturn();

		SupportMessageDTO got = mapper.readValue(mr3.getResponse().getContentAsString(), SupportMessageDTO.class);
		assertThat(got.getId()).isEqualTo("m-2");
	}

	@Test
	void getMessagesByComplaint_privilegedShouldReturnList() throws Exception {
		SupportMessage m = SupportMessage.builder().id("m-3").content("ok").build();
		SupportMessageDTO dto = new SupportMessageDTO(); dto.setId("m-3");

		when(messageService.getMessagesByComplaint("c-1")).thenReturn(List.of(m));
		when(messageMapper.toDTOList(List.of(m))).thenReturn(List.of(dto));


		MvcResult mr4 = mockMvc.perform(get("/api/support-messages/complaint/c-1")
					.header("X-User-Id", "agent-1")
					.header("X-User-Role", "SUPPORT_AGENT"))
			.andExpect(status().isOk())
			.andReturn();

		java.util.List<SupportMessageDTO> list = mapper.readValue(mr4.getResponse().getContentAsString(), mapper.getTypeFactory().constructCollectionType(List.class, SupportMessageDTO.class));
		assertThat(list).hasSize(1);
		assertThat(list.get(0).getId()).isEqualTo("m-3");
	}

	@Test
	void getMessagesByComplaint_userForbiddenWhenNotRelated() throws Exception {
		when(complaintService.getComplaintById("c-1")).thenReturn(complaint);
		// user not reporter or reported

		MvcResult mr5 = mockMvc.perform(get("/api/support-messages/complaint/c-1")
					.header("X-User-Id", "other")
					.header("X-User-Role", "CLIENT"))
			.andExpect(status().isForbidden())
			.andReturn();

		java.util.Map<String, Object> map5 = mapper.readValue(mr5.getResponse().getContentAsString(), java.util.Map.class);
		assertThat(map5.get("error")).isEqualTo("Accès refusé");
	}

	@Test
	void involveReportedUser_successCreatesInvitationAndNotifies() throws Exception {
		InvolveReportedRequest req = new InvolveReportedRequest();
		req.setInvitationMessage("Please join");

		when(complaintService.getComplaintById("c-1")).thenReturn(complaint);
		when(messageRepository.existsByComplaintIdAndConversationType("c-1", ConversationType.REPORTED)).thenReturn(false);


		MvcResult mr6 = mockMvc.perform(post("/api/support-messages/complaint/c-1/involve-reported")
					.contentType("application/json")
					.content(mapper.writeValueAsString(req))
					.header("X-User-Id", "agent-1")
					.header("X-User-Role", "SUPPORT_AGENT"))
			.andExpect(status().isOk())
			.andReturn();

		java.util.Map<String, Object> map6 = mapper.readValue(mr6.getResponse().getContentAsString(), java.util.Map.class);
		assertThat(map6.get("reportedUserId")).isEqualTo("reported-1");

		verify(messageService).createMessage(any());
		verify(notificationService).handle(any());
	}

	@Test
	void markAsRead_shouldReturnDTO() throws Exception {
		SupportMessage m = SupportMessage.builder().id("m-4").content("ok").build();
		SupportMessageDTO dto = new SupportMessageDTO(); dto.setId("m-4");

		when(messageService.markAsRead("m-4")).thenReturn(m);
		when(messageMapper.toDTO(m)).thenReturn(dto);


		MvcResult mr7 = mockMvc.perform(put("/api/support-messages/m-4/mark-read"))
			.andExpect(status().isOk())
			.andReturn();

		SupportMessageDTO dto7 = mapper.readValue(mr7.getResponse().getContentAsString(), SupportMessageDTO.class);
		assertThat(dto7.getId()).isEqualTo("m-4");
	}

	@Test
	void markAllAsRead_shouldReturnList() throws Exception {
		SupportMessage m = SupportMessage.builder().id("m-5").isRead(false).build();
		SupportMessageDTO dto = new SupportMessageDTO(); dto.setId("m-5");

		when(messageService.markAllAsRead("c-1")).thenReturn(List.of(m));
		when(messageMapper.toDTOList(List.of(m))).thenReturn(List.of(dto));


		MvcResult mr8 = mockMvc.perform(put("/api/support-messages/complaint/c-1/mark-all-read")
					.header("X-User-Role", "SUPPORT_AGENT"))
			.andExpect(status().isOk())
			.andReturn();

		java.util.List<SupportMessageDTO> dto8 = mapper.readValue(mr8.getResponse().getContentAsString(), mapper.getTypeFactory().constructCollectionType(List.class, SupportMessageDTO.class));
		assertThat(dto8).hasSize(1);
		assertThat(dto8.get(0).getId()).isEqualTo("m-5");
	}

	@Test
	void countUnreadMessages_shouldReturnCount() throws Exception {
		when(messageService.countUnreadMessages("c-1")).thenReturn(7L);

		MvcResult mr9 = mockMvc.perform(get("/api/support-messages/complaint/c-1/unread-count"))
			.andExpect(status().isOk())
			.andReturn();

		java.util.Map<String, Object> map9 = mapper.readValue(mr9.getResponse().getContentAsString(), java.util.Map.class);
		assertThat(map9.get("unreadCount")).isEqualTo(7);
	}

	@Test
	void getLatestMessage_shouldReturnDTO() throws Exception {
		SupportMessage m = SupportMessage.builder().id("m-6").content("latest").build();
		SupportMessageDTO dto = new SupportMessageDTO(); dto.setId("m-6");

		when(messageService.getLatestMessage("c-1")).thenReturn(m);
		when(messageMapper.toDTO(m)).thenReturn(dto);


		MvcResult mr10 = mockMvc.perform(get("/api/support-messages/complaint/c-1/latest"))
			.andExpect(status().isOk())
			.andReturn();

		SupportMessageDTO dto10 = mapper.readValue(mr10.getResponse().getContentAsString(), SupportMessageDTO.class);
		assertThat(dto10.getId()).isEqualTo("m-6");
	}

	@Test
	void getMessagesBySender_shouldReturnList() throws Exception {
		SupportMessage m = SupportMessage.builder().id("m-7").content("s").build();
		SupportMessageDTO dto = new SupportMessageDTO(); dto.setId("m-7");

		when(messageService.getMessagesBySender("sender-1")).thenReturn(List.of(m));
		when(messageMapper.toDTOList(List.of(m))).thenReturn(List.of(dto));


		MvcResult mr11 = mockMvc.perform(get("/api/support-messages/sender/sender-1")
					.header("X-User-Role", "SUPPORT_AGENT"))
			.andExpect(status().isOk())
			.andReturn();

		java.util.List<SupportMessageDTO> dto11 = mapper.readValue(mr11.getResponse().getContentAsString(), mapper.getTypeFactory().constructCollectionType(List.class, SupportMessageDTO.class));
		assertThat(dto11.get(0).getId()).isEqualTo("m-7");
	}
}
