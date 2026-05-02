package com.microservice.module_competence.services;

import com.microservice.module_competence.dto.*;
import com.microservice.module_competence.entities.Skill;
import com.microservice.module_competence.exceptions.*;
import com.microservice.module_competence.repositories.SkillRepository;
import com.microservice.module_competence.repositories.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;

    public List<Map<String, Object>> getSkillStats() {
        List<Object[]> results = userSkillRepository.countFreelancersBySkill();
        return results.stream().map(row -> {
            Map<String, Object> stat = new HashMap<>();
            stat.put("skillId", row[0]);
            stat.put("skillName", row[1]);
            stat.put("freelancerCount", row[2]);
            return stat;
        }).toList();
    }

    public Map<String, Object> getGlobalStats() {
        List<Object[]> levels = userSkillRepository.countAllLevels();
        Map<String, Object> stats = new HashMap<>();
        long beginner = 0, intermediate = 0, expert = 0;
        for (Object[] row : levels) {
            String level = row[0].toString();
            long count = ((Number) row[1]).longValue();
            if (level.equals("BEGINNER")) beginner = count;
            else if (level.equals("INTERMEDIATE")) intermediate = count;
            else if (level.equals("EXPERT")) expert = count;
        }
        stats.put("beginner", beginner);
        stats.put("intermediate", intermediate);
        stats.put("expert", expert);
        stats.put("total", beginner + intermediate + expert);
        return stats;
    }

    // ── Create
    public SkillResponse createSkill(SkillRequest request) {
        if (skillRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Skill already exists: " + request.getName());
        }
        Skill skill = Skill.builder()
                .name(request.getName())
                .build();
        return toResponse(skillRepository.save(skill));
    }

    // ── Get all
    public List<SkillResponse> getAllSkills() {
        return skillRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Get by id
    public SkillResponse getSkillById(Long id) {
        return toResponse(findById(id));
    }

    // ── Update
    public SkillResponse updateSkill(Long id, SkillRequest request) {
        Skill skill = findById(id);
        if (!skill.getName().equalsIgnoreCase(request.getName())
                && skillRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Skill name already taken: " + request.getName());
        }
        skill.setName(request.getName());
        return toResponse(skillRepository.save(skill));
    }

    // ── Delete
    public void deleteSkill(Long id) {
        if (!skillRepository.existsById(id)) {
            throw new ResourceNotFoundException("Skill not found with id: " + id);
        }
        skillRepository.deleteById(id);
    }

    // ── Mapper
    private SkillResponse toResponse(Skill skill) {
        return SkillResponse.builder()
                .id(skill.getId())
                .name(skill.getName())
                .build();
    }

    public Skill findById(Long id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + id));
    }
}
