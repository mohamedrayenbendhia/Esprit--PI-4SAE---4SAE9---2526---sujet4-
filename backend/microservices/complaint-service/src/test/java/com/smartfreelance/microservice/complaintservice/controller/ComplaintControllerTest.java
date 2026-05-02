package com.smartfreelance.microservice.complaintservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfreelance.microservice.complaintservice.dto.ComplaintDTO;
import com.smartfreelance.microservice.complaintservice.dto.ComplaintRequestDTO;
import com.smartfreelance.microservice.complaintservice.dto.ComplaintUpdateDTO;
import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.entity.Complaint.Priority;
import com.smartfreelance.microservice.complaintservice.entity.Complaint.Status;
import com.smartfreelance.microservice.complaintservice.mapper.ComplaintMapper;
import com.smartfreelance.microservice.complaintservice.notification.ComplaintNotificationService;
import com.smartfreelance.microservice.complaintservice.repository.SupportMessageRepository;
import com.smartfreelance.microservice.complaintservice.service.ComplaintEventService;
import com.smartfreelance.microservice.complaintservice.service.ComplaintService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class ComplaintControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock private ComplaintService complaintService;
    @Mock private ComplaintMapper complaintMapper;
    @Mock private ComplaintNotificationService notificationService;
    @Mock private SupportMessageRepository messageRepository;
    @Mock private ComplaintEventService eventService;
    @Mock private WebClient webClient;

    @InjectMocks private ComplaintController controller;

    private Complaint complaint;
    private ComplaintDTO complaintDTO;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        complaint = new Complaint();
        complaint.setId("1");
        complaint.setTicketNumber("TICK-100");
        complaint.setReporterId("user-1");

        complaintDTO = new ComplaintDTO();
        complaintDTO.setId("1");
        complaintDTO.setTicketNumber("TICK-100");

        // inject mocked webClient into controller.webClient (reflection)
        Field f = ComplaintController.class.getDeclaredField("webClient");
        f.setAccessible(true);
        f.set(controller, webClient);
    }

    @Test
    void getComplaintById_Success() throws Exception {
        when(complaintService.getComplaintById("1")).thenReturn(complaint);
        when(complaintMapper.toDTO(any())).thenReturn(complaintDTO);

        mockMvc.perform(get("/api/complaints/1")
                        .header("X-User-Id", "user-1")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.ticketNumber").value("TICK-100"));
    }

    @Test
    void getComplaintById_Forbidden_WhenNotOwner() throws Exception {
        complaint.setReporterId("other-user");
        when(complaintService.getComplaintById("1")).thenReturn(complaint);

        mockMvc.perform(get("/api/complaints/1")
                        .header("X-User-Id", "hacker-id")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getComplaintById_Forbidden_ForAgentNotAssigned() throws Exception {
        complaint.setAssignedToId("agent-2");
        when(complaintService.getComplaintById("1")).thenReturn(complaint);

        mockMvc.perform(get("/api/complaints/1")
                        .header("X-User-Id", "agent-1")
                        .header("X-User-Role", "SUPPORT_AGENT"))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignComplaint_agentReassignNotCritical_shouldReturnBadRequest() throws Exception {
        // Agent is the current assignee but complaint is not CRITICAL
        complaint.setAssignedToId("agent-1");
        complaint.setPriority(com.smartfreelance.microservice.complaintservice.entity.Complaint.Priority.MEDIUM);
        when(complaintService.getComplaintById("1")).thenReturn(complaint);

        mockMvc.perform(put("/api/complaints/1/assign")
                        .param("agentId", "agent-2")
                        .header("X-User-Id", "agent-1")
                        .header("X-User-Role", "SUPPORT_AGENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void assignComplaint_agentTargetInvalidRole_shouldReturnForbidden() throws Exception {
        // Agent is current assignee and complaint is CRITICAL -> controller will call user-service to check role
        complaint.setAssignedToId("agent-1");
        complaint.setPriority(com.smartfreelance.microservice.complaintservice.entity.Complaint.Priority.CRITICAL);
        when(complaintService.getComplaintById("1")).thenReturn(complaint);

        // mock WebClient chain to return role = CLIENT for the target
        @SuppressWarnings({"unchecked", "rawtypes"})
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        @SuppressWarnings({"unchecked", "rawtypes"})
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), (Object[]) any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(reactor.core.publisher.Mono.just(java.util.Map.of("role", "CLIENT")));

        mockMvc.perform(put("/api/complaints/1/assign")
                        .param("agentId", "some-user")
                        .header("X-User-Id", "agent-1")
                        .header("X-User-Role", "SUPPORT_AGENT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("La cible doit être un agent de support ou un administrateur"));
    }

    @Test
    void getComplaintsByAssignedAgent_forbiddenWhenAgentRequestsOtherAgentQueue() throws Exception {
        mockMvc.perform(get("/api/complaints/assigned/agent-1")
                        .header("X-User-Id", "agent-2")
                        .header("X-User-Role", "SUPPORT_AGENT"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createComplaint_shouldReturnBadRequest_whenReportedEmailResolvesToSameUser_US9() throws Exception {
        ComplaintRequestDTO req = new ComplaintRequestDTO();
        req.setSubject("sub");
        req.setDescription("desc");
        req.setCategory(Complaint.ComplaintCategory.PAYMENT_ISSUE);
        req.setReportedUserEmail("me@example.com");

        // mock WebClient chain to return map {id: "me-id"}
        @SuppressWarnings({"unchecked", "rawtypes"})
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        @SuppressWarnings({"unchecked", "rawtypes"})
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), (Object[]) any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(Map.of("id", "me-id")));

        // call endpoint with X-User-Id = "me-id" to simulate self-report
        mockMvc.perform(post("/api/complaints")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req))
                        .header("X-User-Id", "me-id")
                        .header("X-User-Role", "FREELANCE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Vous ne pouvez pas vous signaler vous-même"));
    }

    @Test
    void takeComplaint_Success() throws Exception {
        when(complaintService.getComplaintById("1")).thenReturn(complaint);
        when(complaintService.assignComplaint(anyString(), anyString())).thenReturn(complaint);
        when(complaintService.updateStatus(anyString(), any())).thenReturn(complaint);
        when(complaintMapper.toDTO(any())).thenReturn(complaintDTO);

        mockMvc.perform(put("/api/complaints/1/take")
                        .header("X-User-Id", "agent-1")
                        .header("X-User-Role", "SUPPORT_AGENT"))
                .andExpect(status().isOk());

        verify(notificationService).handle(any());
    }

    @Test
    void resolveComplaint_Success() throws Exception {
        when(complaintService.getComplaintById("1")).thenReturn(complaint);
        complaint.setAssignedToId("agent-1");

        when(complaintService.resolveComplaint(anyString(), anyString(), any())).thenReturn(complaint);
        when(complaintMapper.toDTO(any())).thenReturn(complaintDTO);

        mockMvc.perform(put("/api/complaints/1/resolve")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"resolution\":\"Problème réglé\",\"resolutionType\":\"REFUND\"}")
                        .header("X-User-Id", "agent-1")
                        .header("X-User-Role", "SUPPORT_AGENT"))
                .andExpect(status().isOk());
    }

    @Test
    void closeComplaint_AdminOnly() throws Exception {
        when(complaintService.closeComplaint("1")).thenReturn(complaint);
        when(complaintMapper.toDTO(any())).thenReturn(complaintDTO);

        mockMvc.perform(put("/api/complaints/1/close")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteComplaint_Success() throws Exception {
        mockMvc.perform(delete("/api/complaints/1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Litige supprimé"));
    }

    @Test
    void getAdminQueue_Success() throws Exception {
        when(complaintService.getUnassignedComplaints(Status.OPEN)).thenReturn(List.of(complaint));
        when(complaintService.getComplaintsByStatus(Status.ESCALATED)).thenReturn(List.of());
        when(complaintMapper.toDTOList(any())).thenReturn(List.of(complaintDTO));

        mockMvc.perform(get("/api/complaints/admin/queue")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
    }

    // ── GET /api/complaints ────────────────────────────────────────

    @Test
    void getAllComplaints_shouldReturn200_withList() throws Exception {
        when(complaintService.getAllComplaints()).thenReturn(List.of(complaint));
        when(complaintMapper.toDTOList(any())).thenReturn(List.of(complaintDTO));

        mockMvc.perform(get("/api/complaints")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"));
    }

    // ── PUT /api/complaints/{id} ───────────────────────────────────

    @Test
    void updateComplaint_shouldReturn200_whenAdmin() throws Exception {
        ComplaintUpdateDTO updateDTO = new ComplaintUpdateDTO();
        updateDTO.setSubject("Updated Subject");

        when(complaintService.getComplaintById("1")).thenReturn(complaint);
        when(complaintService.updateComplaint(eq("1"), any())).thenReturn(complaint);
        when(complaintMapper.toDTO(any())).thenReturn(complaintDTO);

        mockMvc.perform(put("/api/complaints/1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateDTO))
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"));
    }

    // ── PUT /api/complaints/{id}/status ───────────────────────────

    @Test
    void updateComplaintStatus_shouldReturn400_whenClosedDirectly() throws Exception {
        mockMvc.perform(put("/api/complaints/1/status")
                        .param("status", "CLOSED")
                        .header("X-User-Id", "admin-1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void updateComplaintStatus_shouldReturn400_whenResolvedDirectly() throws Exception {
        mockMvc.perform(put("/api/complaints/1/status")
                        .param("status", "RESOLVED")
                        .header("X-User-Id", "admin-1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void updateComplaintStatus_shouldReturn200_whenAdmin() throws Exception {
        Complaint updated = new Complaint();
        updated.setId("1");
        updated.setStatus(Status.IN_PROGRESS);
        when(complaintService.getComplaintById("1")).thenReturn(complaint);
        when(complaintService.updateStatus("1", Status.IN_PROGRESS)).thenReturn(updated);
        when(complaintMapper.toDTO(any())).thenReturn(complaintDTO);

        mockMvc.perform(put("/api/complaints/1/status")
                        .param("status", "IN_PROGRESS")
                        .header("X-User-Id", "admin-1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());

        verify(complaintService).updateStatus("1", Status.IN_PROGRESS);
        verify(notificationService).handle(any());
    }

    @Test
    void updateComplaintStatus_shouldReturn403_whenAgentNotAssigned() throws Exception {
        complaint.setAssignedToId("agent-other");
        when(complaintService.getComplaintById("1")).thenReturn(complaint);

        mockMvc.perform(put("/api/complaints/1/status")
                        .param("status", "IN_PROGRESS")
                        .header("X-User-Id", "agent-1")
                        .header("X-User-Role", "SUPPORT_AGENT"))
                .andExpect(status().isForbidden());
    }

    // ── PUT /api/complaints/{id}/priority ─────────────────────────

    @Test
    void updateComplaintPriority_shouldReturn200_whenAdmin() throws Exception {
        // complaint.getPriority() == MEDIUM; setting to HIGH → priority changed → notification
        when(complaintService.getComplaintById("1")).thenReturn(complaint);
        when(complaintService.updatePriority("1", Priority.HIGH)).thenReturn(complaint);
        when(complaintMapper.toDTO(any())).thenReturn(complaintDTO);

        mockMvc.perform(put("/api/complaints/1/priority")
                        .param("priority", "HIGH")
                        .header("X-User-Id", "admin-1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());

        verify(notificationService).handle(any()); // priority changed → notif sent
    }

    @Test
    void updateComplaintPriority_shouldReturn403_whenAgentNotAssigned() throws Exception {
        complaint.setAssignedToId("agent-other");
        when(complaintService.getComplaintById("1")).thenReturn(complaint);

        mockMvc.perform(put("/api/complaints/1/priority")
                        .param("priority", "HIGH")
                        .header("X-User-Id", "agent-1")
                        .header("X-User-Role", "SUPPORT_AGENT"))
                .andExpect(status().isForbidden());
    }

    // ── PUT /api/complaints/{id}/rate ─────────────────────────────

    @Test
    void rateComplaint_shouldReturn200_whenReporterAndClosed() throws Exception {
        complaint.setStatus(Status.CLOSED);
        when(complaintService.getComplaintById("1")).thenReturn(complaint);
        when(complaintService.rateComplaint("1", 4)).thenReturn(complaint);
        when(complaintMapper.toDTO(any())).thenReturn(complaintDTO);

        mockMvc.perform(put("/api/complaints/1/rate")
                        .param("rating", "4")
                        .header("X-User-Id", "user-1")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isOk());

        verify(complaintService).rateComplaint("1", 4);
    }

    @Test
    void rateComplaint_shouldReturn403_whenNotReporter() throws Exception {
        complaint.setReporterId("someone-else");
        when(complaintService.getComplaintById("1")).thenReturn(complaint);

        mockMvc.perform(put("/api/complaints/1/rate")
                        .param("rating", "3")
                        .header("X-User-Id", "user-1")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void rateComplaint_shouldReturn400_whenNotClosed() throws Exception {
        complaint.setReporterId("user-1");
        complaint.setStatus(Status.RESOLVED); // not CLOSED
        when(complaintService.getComplaintById("1")).thenReturn(complaint);

        mockMvc.perform(put("/api/complaints/1/rate")
                        .param("rating", "5")
                        .header("X-User-Id", "user-1")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── GET /api/complaints/reporter/{reporterId} ─────────────────

    @Test
    void getComplaintsByReporter_shouldReturn200_whenSameUser() throws Exception {
        when(complaintService.getComplaintsByReporter("user-1")).thenReturn(List.of(complaint));
        when(complaintMapper.toDTOList(any())).thenReturn(List.of(complaintDTO));

        mockMvc.perform(get("/api/complaints/reporter/user-1")
                        .header("X-User-Id", "user-1")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"));
    }

    @Test
    void getComplaintsByReporter_shouldReturn403_whenDifferentNonPrivilegedUser() throws Exception {
        mockMvc.perform(get("/api/complaints/reporter/user-1")
                        .header("X-User-Id", "hacker")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getComplaintsByReporter_shouldReturn200_whenAdmin() throws Exception {
        when(complaintService.getComplaintsByReporter("user-1")).thenReturn(List.of(complaint));
        when(complaintMapper.toDTOList(any())).thenReturn(List.of(complaintDTO));

        mockMvc.perform(get("/api/complaints/reporter/user-1")
                        .header("X-User-Id", "admin-1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
    }

    // ── GET /api/complaints/my-complaints ─────────────────────────

    @Test
    void getMyComplaints_shouldReturn200() throws Exception {
        when(complaintService.getComplaintsByReporter("user-1")).thenReturn(List.of(complaint));
        when(complaintMapper.toDTOList(any())).thenReturn(List.of(complaintDTO));

        mockMvc.perform(get("/api/complaints/my-complaints")
                        .header("X-User-Id", "user-1")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"));
    }

    // ── GET /api/complaints/overdue ───────────────────────────────

    @Test
    void getOverdueComplaints_shouldReturn200_withDefaultThreshold() throws Exception {
        when(complaintService.getOverdueComplaints(7)).thenReturn(List.of(complaint));
        when(complaintMapper.toDTOList(any())).thenReturn(List.of(complaintDTO));

        mockMvc.perform(get("/api/complaints/overdue")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"));
    }

    // ── GET /api/complaints/statistics/by-status ─────────────────

    @Test
    void getStatsByStatus_shouldReturn200_withAllStatusCounts() throws Exception {
        for (Status s : Status.values()) {
            when(complaintService.countByStatus(s)).thenReturn(1L);
        }

        mockMvc.perform(get("/api/complaints/statistics/by-status")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.OPEN").value(1))
                .andExpect(jsonPath("$.CLOSED").value(1));
    }

    // ── PUT /api/complaints/{id}/take — additional case ───────────

    @Test
    void takeComplaint_shouldReturn409_whenAlreadyAssigned() throws Exception {
        complaint.setAssignedToId("agent-other"); // already taken
        when(complaintService.getComplaintById("1")).thenReturn(complaint);

        mockMvc.perform(put("/api/complaints/1/take")
                        .header("X-User-Id", "agent-1")
                        .header("X-User-Role", "SUPPORT_AGENT"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── PUT /api/complaints/{id}/resolve — additional case ────────

    @Test
    void resolveComplaint_shouldReturn403_whenAgentNotAssigned() throws Exception {
        complaint.setAssignedToId("agent-other"); // different agent holds the ticket
        when(complaintService.getComplaintById("1")).thenReturn(complaint);

        mockMvc.perform(put("/api/complaints/1/resolve")
                        .contentType("application/json")
                        .content("{\"resolution\":\"Fixed\",\"resolutionType\":\"NO_ACTION\"}")
                        .header("X-User-Id", "agent-1")
                        .header("X-User-Role", "SUPPORT_AGENT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
    }
}