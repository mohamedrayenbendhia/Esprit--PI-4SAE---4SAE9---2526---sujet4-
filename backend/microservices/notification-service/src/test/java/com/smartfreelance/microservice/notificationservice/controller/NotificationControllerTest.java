package com.smartfreelance.microservice.notificationservice.controller;

import com.smartfreelance.microservice.notificationservice.dto.NotificationRequestDTO;
import com.smartfreelance.microservice.notificationservice.dto.NotificationResponseDTO;
import com.smartfreelance.microservice.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService service;

    @InjectMocks
    private NotificationController controller;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
    }

    @Test
    void create_shouldReturnCreated() throws Exception {
        NotificationRequestDTO req = new NotificationRequestDTO();
        req.setRecipientId("user-1");
        req.setType("INFO");
        req.setTitle("T");
        req.setMessage("Test notification message");

        NotificationResponseDTO resp = NotificationResponseDTO.builder()
                .id("n1").recipientId("user-1").type("INFO").title("T").createdAt(LocalDateTime.now()).build();

        when(service.create(any())).thenReturn(resp);

        mockMvc.perform(post("/api/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("n1"));
    }

    @Test
    void getUnread_shouldReturnList() throws Exception {
        NotificationResponseDTO resp = NotificationResponseDTO.builder().id("n1").recipientId("user-1").createdAt(LocalDateTime.now()).build();
        when(service.getUnreadByUser("user-1")).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/notifications/user/user-1/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("n1"));
    }

    @Test
    void countUnread_shouldReturnCount() throws Exception {
        when(service.countUnread("user-1")).thenReturn(4L);

        mockMvc.perform(get("/api/notifications/user/user-1/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(4));
    }

    @Test
    void markAsRead_shouldReturnUpdated() throws Exception {
        NotificationResponseDTO resp = NotificationResponseDTO.builder().id("n1").recipientId("user-1").read(true).readAt(LocalDateTime.now()).build();
        when(service.markAsRead("n1")).thenReturn(resp);

        mockMvc.perform(put("/api/notifications/n1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("n1"))
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void markAllAsRead_shouldReturnMarkedCount() throws Exception {
        when(service.markAllAsRead("user-1")).thenReturn(2);

        mockMvc.perform(put("/api/notifications/user/user-1/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marked").value(2));
    }

    @Test
    void getByUser_shouldReturnPage() throws Exception {
        NotificationResponseDTO resp = NotificationResponseDTO.builder().id("n1").recipientId("user-1").createdAt(LocalDateTime.now()).build();
        when(service.getByUser(eq("user-1"), eq(0), eq(20))).thenReturn(
                new org.springframework.data.domain.PageImpl<>(
                        List.of(resp),
                        org.springframework.data.domain.PageRequest.of(0, 20),
                        1));

        mockMvc.perform(get("/api/notifications/user/user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("n1"));
    }

    @Test
    void getByUser_withCustomPagination_shouldReturnCorrectPage() throws Exception {
        NotificationResponseDTO resp = NotificationResponseDTO.builder().id("n2").recipientId("user-2").createdAt(LocalDateTime.now()).build();
        when(service.getByUser(eq("user-2"), eq(1), eq(5))).thenReturn(
                new org.springframework.data.domain.PageImpl<>(
                        List.of(resp),
                        org.springframework.data.domain.PageRequest.of(1, 5),
                        6));

        mockMvc.perform(get("/api/notifications/user/user-2")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("n2"))
                .andExpect(jsonPath("$.totalElements").value(6));
    }

    @Test
    void getUnread_emptyList_shouldReturn200AndEmptyArray() throws Exception {
        when(service.getUnreadByUser("user-3")).thenReturn(List.of());

        mockMvc.perform(get("/api/notifications/user/user-3/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void countUnread_whenZero_shouldReturnZero() throws Exception {
        when(service.countUnread("user-4")).thenReturn(0L);

        mockMvc.perform(get("/api/notifications/user/user-4/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void markAllAsRead_whenNoUnread_shouldReturnZero() throws Exception {
        when(service.markAllAsRead("user-5")).thenReturn(0);

        mockMvc.perform(put("/api/notifications/user/user-5/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marked").value(0));
    }

    @Test
    void markAsRead_shouldThrowServletException_whenNotFound() {
        // In standalone MockMvc there is no global exception handler, so an uncaught
        // IllegalArgumentException from the service propagates as a ServletException.
        when(service.markAsRead("ghost")).thenThrow(new IllegalArgumentException("Notification not found: ghost"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                mockMvc.perform(put("/api/notifications/ghost/read")))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("Notification not found: ghost");
    }
}
