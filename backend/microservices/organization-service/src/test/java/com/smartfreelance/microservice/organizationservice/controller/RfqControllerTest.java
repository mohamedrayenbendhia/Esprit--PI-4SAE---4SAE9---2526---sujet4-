package com.smartfreelance.microservice.organizationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfreelance.microservice.organizationservice.dto.request.CreateRfqRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.RfqResponseRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.RfqResponse;
import com.smartfreelance.microservice.organizationservice.enums.RfqStatus;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.GlobalExceptionHandler;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.service.RfqService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RfqController Unit Tests")
class RfqControllerTest {

    @Mock private RfqService rfqService;
    @InjectMocks private RfqController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String USER_ID = "user-1";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Authentication mockAuth() {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(USER_ID);
        return auth;
    }

    private RfqResponse buildRfqResponse(String id, RfqStatus status) {
        return RfqResponse.builder()
                .id(id)
                .organizationId("org-1")
                .requesterId(USER_ID)
                .title("Need a developer")
                .description("We need a Java developer")
                .status(status)
                .build();
    }

    // ── POST /api/organizations/{orgId}/rfq ───────────────────────────────────

    @Test
    @DisplayName("create_validRequest_returns201")
    void create_validRequest_returns201() throws Exception {
        CreateRfqRequest req = new CreateRfqRequest();
        req.setTitle("Need a developer");
        req.setDescription("We need a Java developer");

        RfqResponse response = buildRfqResponse("rfq-1", RfqStatus.PENDING);
        when(rfqService.create(eq("org-1"), any(CreateRfqRequest.class), eq(USER_ID)))
                .thenReturn(response);

        mockMvc.perform(post("/api/organizations/org-1/rfq")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("rfq-1"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("create_missingTitle_returns400")
    void create_missingTitle_returns400() throws Exception {
        CreateRfqRequest req = new CreateRfqRequest();
        // title is missing (blank)
        req.setTitle("");
        req.setDescription("Some description");

        mockMvc.perform(post("/api/organizations/org-1/rfq")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("create_orgNotFound_returns404")
    void create_orgNotFound_returns404() throws Exception {
        CreateRfqRequest req = new CreateRfqRequest();
        req.setTitle("Some RFQ");
        req.setDescription("Description");

        when(rfqService.create(eq("missing"), any(), eq(USER_ID)))
                .thenThrow(new ResourceNotFoundException("Organization not found"));

        mockMvc.perform(post("/api/organizations/missing/rfq")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/organizations/{orgId}/rfq ───────────────────────────────────

    @Test
    @DisplayName("list_existingOrg_returnsPage")
    void list_existingOrg_returnsPage() throws Exception {
        RfqResponse r = buildRfqResponse("rfq-1", RfqStatus.PENDING);
        when(rfqService.getByOrg(eq("org-1"), any()))
                .thenReturn(new PageImpl<>(List.of(r), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/organizations/org-1/rfq")
                        .principal(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value("rfq-1"));
    }

    @Test
    @DisplayName("list_emptyOrg_returnsEmptyPage")
    void list_emptyOrg_returnsEmptyPage() throws Exception {
        when(rfqService.getByOrg(eq("org-1"), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/organizations/org-1/rfq")
                        .principal(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("list_multipleRfqs_returnsAll")
    void list_multipleRfqs_returnsAll() throws Exception {
        RfqResponse r1 = buildRfqResponse("rfq-1", RfqStatus.PENDING);
        RfqResponse r2 = buildRfqResponse("rfq-2", RfqStatus.RESPONDED);
        when(rfqService.getByOrg(eq("org-1"), any()))
                .thenReturn(new PageImpl<>(List.of(r1, r2), PageRequest.of(0, 10), 2));

        mockMvc.perform(get("/api/organizations/org-1/rfq")
                        .principal(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    // ── POST /api/organizations/{orgId}/rfq/{rfqId}/respond ──────────────────

    @Test
    @DisplayName("respond_validRequest_returns200")
    void respond_validRequest_returns200() throws Exception {
        RfqResponseRequest req = new RfqResponseRequest();
        req.setResponseMessage("We can help you!");

        RfqResponse response = buildRfqResponse("rfq-1", RfqStatus.RESPONDED);
        response.setResponseMessage("We can help you!");
        when(rfqService.respond(eq("rfq-1"), any(RfqResponseRequest.class), eq(USER_ID)))
                .thenReturn(response);

        mockMvc.perform(post("/api/organizations/org-1/rfq/rfq-1/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESPONDED"));
    }

    @Test
    @DisplayName("respond_rfqNotFound_returns404")
    void respond_rfqNotFound_returns404() throws Exception {
        RfqResponseRequest req = new RfqResponseRequest();
        req.setResponseMessage("We can help!");

        when(rfqService.respond(eq("missing"), any(), eq(USER_ID)))
                .thenThrow(new ResourceNotFoundException("RFQ not found: missing"));

        mockMvc.perform(post("/api/organizations/org-1/rfq/missing/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("respond_rfqNotPending_returns422")
    void respond_rfqNotPending_returns422() throws Exception {
        RfqResponseRequest req = new RfqResponseRequest();
        req.setResponseMessage("We can help!");

        when(rfqService.respond(eq("rfq-1"), any(), eq(USER_ID)))
                .thenThrow(new BusinessRuleException("RFQ is no longer pending."));

        mockMvc.perform(post("/api/organizations/org-1/rfq/rfq-1/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── POST /api/organizations/{orgId}/rfq/{rfqId}/close ────────────────────

    @Test
    @DisplayName("close_validRfq_returns204")
    void close_validRfq_returns204() throws Exception {
        doNothing().when(rfqService).close("rfq-1", USER_ID);

        mockMvc.perform(post("/api/organizations/org-1/rfq/rfq-1/close")
                        .principal(mockAuth()))
                .andExpect(status().isNoContent());

        verify(rfqService).close("rfq-1", USER_ID);
    }

    @Test
    @DisplayName("close_rfqNotFound_returns404")
    void close_rfqNotFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("RFQ not found: missing"))
                .when(rfqService).close("missing", USER_ID);

        mockMvc.perform(post("/api/organizations/org-1/rfq/missing/close")
                        .principal(mockAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("close_notOwnerOrManager_returns422")
    void close_notOwnerOrManager_returns422() throws Exception {
        doThrow(new BusinessRuleException("Only the organization owner or a manager can close an RFQ."))
                .when(rfqService).close("rfq-1", USER_ID);

        mockMvc.perform(post("/api/organizations/org-1/rfq/rfq-1/close")
                        .principal(mockAuth()))
                .andExpect(status().isUnprocessableEntity());
    }
}
