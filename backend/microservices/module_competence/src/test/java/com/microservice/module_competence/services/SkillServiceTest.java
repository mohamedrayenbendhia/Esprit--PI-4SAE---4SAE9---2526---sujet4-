package com.microservice.module_competence.services;

import com.microservice.module_competence.dto.SkillRequest;
import com.microservice.module_competence.dto.SkillResponse;
import com.microservice.module_competence.entities.Skill;
import com.microservice.module_competence.exceptions.DuplicateResourceException;
import com.microservice.module_competence.exceptions.ResourceNotFoundException;
import com.microservice.module_competence.repositories.SkillRepository;
import com.microservice.module_competence.repositories.UserSkillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Arrays;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock SkillRepository skillRepository;
    @Mock UserSkillRepository userSkillRepository;
    @InjectMocks SkillService skillService;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private Skill skill(Long id, String name) {
        Skill s = new Skill(); s.setId(id); s.setName(name); return s;
    }

    private SkillRequest request(String name) {
        SkillRequest r = new SkillRequest(); r.setName(name); return r;
    }

    // ── createSkill ───────────────────────────────────────────────────────────

    @Test
    void createSkill_success() {
        when(skillRepository.existsByNameIgnoreCase("Java")).thenReturn(false);
        when(skillRepository.save(any())).thenReturn(skill(1L, "Java"));

        SkillResponse res = skillService.createSkill(request("Java"));

        assertThat(res.getName()).isEqualTo("Java");
    }

    @Test
    void createSkill_duplicate_throws() {
        when(skillRepository.existsByNameIgnoreCase("Java")).thenReturn(true);

        assertThatThrownBy(() -> skillService.createSkill(request("Java")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    // ── getAllSkills ──────────────────────────────────────────────────────────

    @Test
    void getAllSkills_returnsList() {
        List<Skill> skills = Arrays.asList(skill(1L, "Java"), skill(2L, "Python"));
        doReturn(skills).when(skillRepository).findAll();

        assertThat(skillService.getAllSkills()).hasSize(2);
    }

    // ── getSkillById ──────────────────────────────────────────────────────────

    @Test
    void getSkillById_found() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill(1L, "Java")));

        assertThat(skillService.getSkillById(1L).getName()).isEqualTo("Java");
    }

    @Test
    void getSkillById_notFound_throws() {
        when(skillRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillService.getSkillById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateSkill ───────────────────────────────────────────────────────────

    @Test
    void updateSkill_success() {
        Skill existing = skill(1L, "Java");
        when(skillRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(skillRepository.existsByNameIgnoreCase("Kotlin")).thenReturn(false);
        when(skillRepository.save(any())).thenReturn(skill(1L, "Kotlin"));

        SkillResponse res = skillService.updateSkill(1L, request("Kotlin"));

        assertThat(res.getName()).isEqualTo("Kotlin");
    }

    @Test
    void updateSkill_sameName_noConflict() {
        Skill existing = skill(1L, "Java");
        when(skillRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(skillRepository.save(any())).thenReturn(skill(1L, "Java"));

        // same name → no duplicate check triggered
        assertThatNoException().isThrownBy(() -> skillService.updateSkill(1L, request("Java")));
    }

    @Test
    void updateSkill_duplicateName_throws() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill(1L, "Java")));
        when(skillRepository.existsByNameIgnoreCase("Kotlin")).thenReturn(true);

        assertThatThrownBy(() -> skillService.updateSkill(1L, request("Kotlin")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    // ── deleteSkill ───────────────────────────────────────────────────────────

    @Test
    void deleteSkill_success() {
        when(skillRepository.existsById(1L)).thenReturn(true);

        skillService.deleteSkill(1L);

        verify(skillRepository).deleteById(1L);
    }

    @Test
    void deleteSkill_notFound_throws() {
        when(skillRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> skillService.deleteSkill(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getSkillStats / getGlobalStats ────────────────────────────────────────

    @Test
    void getSkillStats_mapsRows() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{1L, "Java", 5L});
        doReturn(rows).when(userSkillRepository).countFreelancersBySkill();

        var stats = skillService.getSkillStats();
        assertThat(stats).hasSize(1);
        assertThat(stats.get(0)).containsEntry("skillName", "Java");
    }

    @Test
    void getGlobalStats_aggregatesLevels() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"BEGINNER", 3L});
        rows.add(new Object[]{"INTERMEDIATE", 2L});
        rows.add(new Object[]{"EXPERT", 1L});
        doReturn(rows).when(userSkillRepository).countAllLevels();

        var stats = skillService.getGlobalStats();
        assertThat(stats).containsEntry("total", 6L);
    }
}