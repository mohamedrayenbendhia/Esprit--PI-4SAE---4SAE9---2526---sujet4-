package com.microservice.module_competence.controllers;

import com.microservice.module_competence.services.EndorsementService;
import com.microservice.module_competence.services.SkillService;
import com.microservice.module_competence.services.UserSkillService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientSkillController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClientSkillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SkillService skillService;

    @MockBean
    private UserSkillService userSkillService;

    @MockBean
    private EndorsementService endorsementService;

    private Authentication auth() {
        Claims claims = mock(Claims.class);
        when(claims.get("userId", String.class)).thenReturn("client1");

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(claims);

        return auth;
    }

    @Test
    void endorse_ok() throws Exception {
        doReturn(5L).when(endorsementService)
                .endorse("client1", "free1", 1L);

        mockMvc.perform(post("/api/client/skills/endorse/free1/1")
                        .principal(auth()))
                .andExpect(status().isOk());
    }

    @Test
    void removeEndorsement_ok() throws Exception {
        doReturn(3L).when(endorsementService)
                .removeEndorsement("client1", "free1", 1L);

        mockMvc.perform(delete("/api/client/skills/endorse/free1/1")
                        .principal(auth()))
                .andExpect(status().isOk());
    }

    @Test
    void getCount_ok() throws Exception {
        doReturn(4L).when(endorsementService)
                .getEndorsementCount("free1", 1L);

        mockMvc.perform(get("/api/client/skills/endorse/free1/1/count"))
                .andExpect(status().isOk());
    }

    @Test
    void hasEndorsed_ok() throws Exception {
        doReturn(true).when(endorsementService)
                .hasEndorsed("client1", "free1", 1L);

        mockMvc.perform(get("/api/client/skills/endorse/free1/1/has-endorsed")
                        .principal(auth()))
                .andExpect(status().isOk());
    }

    @Test
    void getAllSkills_ok() throws Exception {
        doReturn(List.of()).when(skillService).getAllSkills();

        mockMvc.perform(get("/api/client/skills"))
                .andExpect(status().isOk());
    }
}