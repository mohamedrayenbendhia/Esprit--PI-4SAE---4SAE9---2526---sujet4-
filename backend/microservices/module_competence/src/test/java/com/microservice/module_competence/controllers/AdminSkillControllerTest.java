package com.microservice.module_competence.controllers;

import com.microservice.module_competence.dto.SkillRequest;
import com.microservice.module_competence.dto.SkillResponse;
import com.microservice.module_competence.services.SkillService;
import com.microservice.module_competence.services.UserSkillService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminSkillController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminSkillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SkillService skillService;

    @MockBean
    private UserSkillService userSkillService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllSkills_ok() throws Exception {
        doReturn(List.of()).when(skillService).getAllSkills();

        mockMvc.perform(get("/api/admin/skills"))
                .andExpect(status().isOk());
    }

    @Test
    void createSkill_created() throws Exception {
        SkillRequest req = new SkillRequest();
        req.setName("Java");

        SkillResponse res = SkillResponse.builder().id(1L).name("Java").build();

        doReturn(res).when(skillService).createSkill(any());

        mockMvc.perform(post("/api/admin/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void getById_ok() throws Exception {
        doReturn(SkillResponse.builder().id(1L).name("Java").build())
                .when(skillService).getSkillById(1L);

        mockMvc.perform(get("/api/admin/skills/1"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_noContent() throws Exception {
        doNothing().when(skillService).deleteSkill(1L);

        mockMvc.perform(delete("/api/admin/skills/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getStats_ok() throws Exception {
        doReturn(List.of()).when(skillService).getSkillStats();

        mockMvc.perform(get("/api/admin/skills/stats"))
                .andExpect(status().isOk());
    }

    @Test
    void getUserSkills_ok() throws Exception {
        doReturn(List.of()).when(userSkillService).getSkillsByUser("u1");

        mockMvc.perform(get("/api/admin/skills/user-skills/u1"))
                .andExpect(status().isOk());
    }
}
