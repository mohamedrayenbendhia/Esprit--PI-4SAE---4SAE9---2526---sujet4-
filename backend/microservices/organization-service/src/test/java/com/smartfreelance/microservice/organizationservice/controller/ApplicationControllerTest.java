package com.smartfreelance.microservice.organizationservice.controller;

import com.smartfreelance.microservice.organizationservice.service.ApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ApplicationControllerTest {

    @Mock private ApplicationService applicationService;
    @InjectMocks private ApplicationController controller;

    private MockMvc mockMvc;

    @BeforeEach void setUp() { mockMvc = MockMvcBuilders.standaloneSetup(controller).build(); }

    private Authentication mockAuth(String userId) {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(userId);
        return auth;
    }

    @Test
    void submitApplication_shouldReturnCreated() throws Exception {
        mockMvc.perform(post("/api/organizations/o1/applications")
                        .contentType("application/json")
                        .content("{\"message\":\"I am interested\"}")
                        .principal(mockAuth("u1")))
                .andExpect(status().isCreated());
        verify(applicationService).apply(eq("o1"), any(), eq("u1"));
    }
}
