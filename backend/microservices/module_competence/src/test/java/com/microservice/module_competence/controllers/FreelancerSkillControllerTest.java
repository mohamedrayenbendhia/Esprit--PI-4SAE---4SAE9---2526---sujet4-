package com.microservice.module_competence.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.module_competence.dto.*;
import com.microservice.module_competence.enums.Level;
import com.microservice.module_competence.services.SkillService;
import com.microservice.module_competence.services.UserSkillService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FreelancerSkillControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private UserSkillService userSkillService;
    @Mock private SkillService skillService;
    @Mock private Authentication authentication;
    @Mock private Claims claims;

    @InjectMocks
    private FreelancerSkillController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // Helper auth
    private void mockAuth() {
        when(authentication.getPrincipal()).thenReturn(claims);
        when(claims.get("userId", String.class)).thenReturn("user-123");
    }

    // Helper request valide
    private UserSkillRequest validRequest() {
        UserSkillRequest req = new UserSkillRequest();
        req.setSkillId(1L);
        req.setLevel(Level.BEGINNER);
        return req;
    }

    @Test
    void getAllSkills_shouldReturn200() throws Exception {
        when(skillService.getAllSkills()).thenReturn(List.of());
        mockMvc.perform(get("/api/freelancer/skills"))
                .andExpect(status().isOk());
    }

    @Test
    void addSkill_shouldReturn201() throws Exception {
        mockAuth();
        when(userSkillService.addSkillToUser(any())).thenReturn(new UserSkillResponse());

        mockMvc.perform(post("/api/freelancer/skills/user-skills")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void addSkill_shouldReturn400_whenRequestInvalid() throws Exception {
        mockMvc.perform(post("/api/freelancer/skills/user-skills")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserSkillRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMySkills_shouldReturn200() throws Exception {
        mockAuth();
        when(userSkillService.getSkillsByUser("user-123")).thenReturn(List.of());

        mockMvc.perform(get("/api/freelancer/skills/user-skills/me")
                        .principal(authentication))
                .andExpect(status().isOk());
    }

    @Test
    void getMySkillsByLevel_shouldReturn200() throws Exception {
        mockAuth();
        when(userSkillService.getSkillsByUserAndLevel("user-123", Level.BEGINNER)).thenReturn(List.of());

        mockMvc.perform(get("/api/freelancer/skills/user-skills/me/level")
                        .principal(authentication)
                        .param("level", "BEGINNER"))
                .andExpect(status().isOk());
    }

    @Test
    void updateSkill_shouldReturn200() throws Exception {
        mockAuth();
        when(userSkillService.updateUserSkill(eq(1L), any())).thenReturn(new UserSkillResponse());

        mockMvc.perform(put("/api/freelancer/skills/user-skills/1")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk());
    }

    @Test
    void deleteSkill_shouldReturn204() throws Exception {
        doNothing().when(userSkillService).deleteUserSkill(1L);
        mockMvc.perform(delete("/api/freelancer/skills/user-skills/1"))
                .andExpect(status().isNoContent());
    }
}