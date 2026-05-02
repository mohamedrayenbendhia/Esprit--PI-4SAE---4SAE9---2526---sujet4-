package com.smartfreelance.microservice.organizationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartfreelance.microservice.organizationservice.dto.request.CreatePortfolioItemRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.PortfolioItemResponse;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.GlobalExceptionHandler;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.service.PortfolioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
@DisplayName("PortfolioController Unit Tests")
class PortfolioControllerTest {

    @Mock private PortfolioService portfolioService;
    @InjectMocks private PortfolioController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
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

    private PortfolioItemResponse buildPortfolioResponse(String id) {
        return PortfolioItemResponse.builder()
                .id(id)
                .organizationId("org-1")
                .title("Project Alpha")
                .description("A great project")
                .build();
    }

    private CreatePortfolioItemRequest buildCreateRequest() {
        CreatePortfolioItemRequest req = new CreatePortfolioItemRequest();
        req.setTitle("Project Alpha");
        req.setDescription("A great project");
        return req;
    }

    // ── POST /api/organizations/{orgId}/portfolio ─────────────────────────────

    @Test
    @DisplayName("add_validRequest_returns201")
    void add_validRequest_returns201() throws Exception {
        CreatePortfolioItemRequest req = buildCreateRequest();
        PortfolioItemResponse response = buildPortfolioResponse("item-1");
        when(portfolioService.add(eq("org-1"), any(CreatePortfolioItemRequest.class), eq(USER_ID)))
                .thenReturn(response);

        mockMvc.perform(post("/api/organizations/org-1/portfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("item-1"))
                .andExpect(jsonPath("$.title").value("Project Alpha"));
    }

    @Test
    @DisplayName("add_missingTitle_returns400")
    void add_missingTitle_returns400() throws Exception {
        CreatePortfolioItemRequest req = new CreatePortfolioItemRequest();
        req.setTitle(""); // blank title triggers validation

        mockMvc.perform(post("/api/organizations/org-1/portfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("add_orgNotFound_returns404")
    void add_orgNotFound_returns404() throws Exception {
        CreatePortfolioItemRequest req = buildCreateRequest();
        when(portfolioService.add(eq("missing"), any(), eq(USER_ID)))
                .thenThrow(new ResourceNotFoundException("Organization not found"));

        mockMvc.perform(post("/api/organizations/missing/portfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/organizations/{orgId}/portfolio ──────────────────────────────

    @Test
    @DisplayName("list_existingOrg_returnsList")
    void list_existingOrg_returnsList() throws Exception {
        List<PortfolioItemResponse> items = List.of(
                buildPortfolioResponse("item-1"),
                buildPortfolioResponse("item-2")
        );
        when(portfolioService.getByOrg("org-1")).thenReturn(items);

        mockMvc.perform(get("/api/organizations/org-1/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("item-1"));
    }

    @Test
    @DisplayName("list_emptyPortfolio_returnsEmptyList")
    void list_emptyPortfolio_returnsEmptyList() throws Exception {
        when(portfolioService.getByOrg("org-1")).thenReturn(List.of());

        mockMvc.perform(get("/api/organizations/org-1/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("list_orgNotFound_returns404")
    void list_orgNotFound_returns404() throws Exception {
        when(portfolioService.getByOrg("missing"))
                .thenThrow(new ResourceNotFoundException("Organization not found"));

        mockMvc.perform(get("/api/organizations/missing/portfolio"))
                .andExpect(status().isNotFound());
    }

    // ── PUT /api/organizations/{orgId}/portfolio/{itemId} ─────────────────────

    @Test
    @DisplayName("update_validRequest_returns200")
    void update_validRequest_returns200() throws Exception {
        CreatePortfolioItemRequest req = buildCreateRequest();
        req.setTitle("Project Beta");
        PortfolioItemResponse response = PortfolioItemResponse.builder()
                .id("item-1").organizationId("org-1").title("Project Beta").build();
        when(portfolioService.update(eq("item-1"), eq("org-1"), any(CreatePortfolioItemRequest.class), eq(USER_ID)))
                .thenReturn(response);

        mockMvc.perform(put("/api/organizations/org-1/portfolio/item-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Project Beta"));
    }

    @Test
    @DisplayName("update_itemNotFound_returns404")
    void update_itemNotFound_returns404() throws Exception {
        CreatePortfolioItemRequest req = buildCreateRequest();
        when(portfolioService.update(eq("missing"), eq("org-1"), any(), eq(USER_ID)))
                .thenThrow(new ResourceNotFoundException("Portfolio item not found: missing"));

        mockMvc.perform(put("/api/organizations/org-1/portfolio/missing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("update_itemBelongsToDifferentOrg_returns422")
    void update_itemBelongsToDifferentOrg_returns422() throws Exception {
        CreatePortfolioItemRequest req = buildCreateRequest();
        when(portfolioService.update(eq("item-1"), eq("org-2"), any(), eq(USER_ID)))
                .thenThrow(new BusinessRuleException("Portfolio item does not belong to this organization."));

        mockMvc.perform(put("/api/organizations/org-2/portfolio/item-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(mockAuth()))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── DELETE /api/organizations/{orgId}/portfolio/{itemId} ──────────────────

    @Test
    @DisplayName("delete_validItem_returns204")
    void delete_validItem_returns204() throws Exception {
        doNothing().when(portfolioService).delete("item-1", "org-1", USER_ID);

        mockMvc.perform(delete("/api/organizations/org-1/portfolio/item-1")
                        .principal(mockAuth()))
                .andExpect(status().isNoContent());

        verify(portfolioService).delete("item-1", "org-1", USER_ID);
    }

    @Test
    @DisplayName("delete_itemNotFound_returns404")
    void delete_itemNotFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Portfolio item not found: missing"))
                .when(portfolioService).delete("missing", "org-1", USER_ID);

        mockMvc.perform(delete("/api/organizations/org-1/portfolio/missing")
                        .principal(mockAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("delete_itemBelongsToDifferentOrg_returns422")
    void delete_itemBelongsToDifferentOrg_returns422() throws Exception {
        doThrow(new BusinessRuleException("Portfolio item does not belong to this organization."))
                .when(portfolioService).delete("item-1", "org-2", USER_ID);

        mockMvc.perform(delete("/api/organizations/org-2/portfolio/item-1")
                        .principal(mockAuth()))
                .andExpect(status().isUnprocessableEntity());
    }
}
