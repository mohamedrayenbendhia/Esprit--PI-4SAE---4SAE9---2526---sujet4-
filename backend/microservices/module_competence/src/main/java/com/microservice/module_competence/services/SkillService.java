package com.microservice.module_competence.services;

import com.microservice.module_competence.dto.*;
import com.microservice.module_competence.entities.Skill;
import com.microservice.module_competence.exceptions.*;
import com.microservice.module_competence.repositories.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;

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
