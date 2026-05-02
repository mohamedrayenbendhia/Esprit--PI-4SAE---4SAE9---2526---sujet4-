package com.microservice.module_competence.services;

import com.microservice.module_competence.dto.UserSkillRequest;
import com.microservice.module_competence.dto.UserSkillResponse;
import com.microservice.module_competence.entities.Skill;
import com.microservice.module_competence.entities.UserSkill;
import com.microservice.module_competence.enums.Level;
import com.microservice.module_competence.exceptions.DuplicateResourceException;
import com.microservice.module_competence.exceptions.ResourceNotFoundException;
import com.microservice.module_competence.repositories.UserSkillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSkillServiceTest {

    @Mock UserSkillRepository userSkillRepository;
    @Mock SkillService skillService;
    @InjectMocks UserSkillService userSkillService;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private Skill skill(Long id) { Skill s = new Skill(); s.setId(id); s.setName("Java"); return s; }

    private UserSkill userSkill(Long id, String userId) {
        UserSkill us = new UserSkill();
        us.setId(id);
        us.setUserId(userId);
        us.setSkill(skill(1L));
        us.setLevel(Level.INTERMEDIATE);
        us.setYearsOfExperience(3);
        return us;
    }

    private UserSkillRequest request(String userId, Long skillId) {
        UserSkillRequest r = new UserSkillRequest();
        r.setUserId(userId);
        r.setSkillId(skillId);
        r.setLevel(Level.INTERMEDIATE);
        r.setYearsOfExperience(3);
        return r;
    }

    // ── addSkillToUser ────────────────────────────────────────────────────────

    @Test
    void addSkillToUser_success() {
        when(userSkillRepository.existsByUserIdAndSkillId("u1", 1L)).thenReturn(false);
        when(skillService.findById(1L)).thenReturn(skill(1L));
        when(userSkillRepository.save(any())).thenReturn(userSkill(10L, "u1"));

        UserSkillResponse res = userSkillService.addSkillToUser(request("u1", 1L));

        assertThat(res.getUserId()).isEqualTo("u1");
    }

    @Test
    void addSkillToUser_alreadyExists_throws() {
        when(userSkillRepository.existsByUserIdAndSkillId("u1", 1L)).thenReturn(true);

        assertThatThrownBy(() -> userSkillService.addSkillToUser(request("u1", 1L)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    // ── getSkillsByUser ───────────────────────────────────────────────────────

    @Test
    void getSkillsByUser_returnsList() {
        when(userSkillRepository.findByUserId("u1")).thenReturn(List.of(userSkill(1L, "u1")));

        assertThat(userSkillService.getSkillsByUser("u1")).hasSize(1);
    }

    // ── getSkillsBySkillId ────────────────────────────────────────────────────

    @Test
    void getSkillsBySkillId_returnsList() {
        when(userSkillRepository.findBySkillId(1L)).thenReturn(List.of(userSkill(1L, "u1")));

        assertThat(userSkillService.getSkillsBySkillId(1L)).hasSize(1);
    }

    // ── getSkillsByUserAndLevel ───────────────────────────────────────────────

    @Test
    void getSkillsByUserAndLevel_filtered() {
        when(userSkillRepository.findByUserIdAndLevel("u1", Level.INTERMEDIATE))
                .thenReturn(List.of(userSkill(1L, "u1")));

        assertThat(userSkillService.getSkillsByUserAndLevel("u1", Level.INTERMEDIATE)).hasSize(1);
    }

    // ── getUserSkillById ──────────────────────────────────────────────────────

    @Test
    void getUserSkillById_found() {
        when(userSkillRepository.findById(1L)).thenReturn(Optional.of(userSkill(1L, "u1")));

        assertThat(userSkillService.getUserSkillById(1L).getId()).isEqualTo(1L);
    }

    @Test
    void getUserSkillById_notFound_throws() {
        when(userSkillRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userSkillService.getUserSkillById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateUserSkill ───────────────────────────────────────────────────────

    @Test
    void updateUserSkill_success() {
        UserSkill existing = userSkill(1L, "u1");
        when(userSkillRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(skillService.findById(1L)).thenReturn(skill(1L));
        when(userSkillRepository.save(any())).thenReturn(existing);

        UserSkillResponse res = userSkillService.updateUserSkill(1L, request("u1", 1L));

        assertThat(res.getLevel()).isEqualTo(Level.INTERMEDIATE);
    }

    // ── deleteUserSkill ───────────────────────────────────────────────────────

    @Test
    void deleteUserSkill_success() {
        when(userSkillRepository.existsById(1L)).thenReturn(true);

        userSkillService.deleteUserSkill(1L);

        verify(userSkillRepository).deleteById(1L);
    }

    @Test
    void deleteUserSkill_notFound_throws() {
        when(userSkillRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userSkillService.deleteUserSkill(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}