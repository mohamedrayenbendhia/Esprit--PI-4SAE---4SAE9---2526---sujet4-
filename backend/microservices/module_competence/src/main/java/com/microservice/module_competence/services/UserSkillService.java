package com.microservice.module_competence.services;

import com.microservice.module_competence.dto.*;
import com.microservice.module_competence.entities.*;
import com.microservice.module_competence.enums.Level;
import com.microservice.module_competence.exceptions.*;
import com.microservice.module_competence.repositories.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserSkillService {

    private final UserSkillRepository userSkillRepository;
    private final SkillService skillService;

    @Transactional
    public UserSkillResponse addSkillToUser(UserSkillRequest request) {
        if (userSkillRepository.existsByUserIdAndSkillId(request.getUserId(), request.getSkillId())) {
            throw new DuplicateResourceException("User already has this skill");
        }
        Skill skill = skillService.findById(request.getSkillId());
        UserSkill userSkill = UserSkill.builder()
                .userId(request.getUserId())
                .skill(skill)
                .level(request.getLevel())
                .yearsOfExperience(request.getYearsOfExperience())
                .build();
        return toResponse(userSkillRepository.save(userSkill));
    }

    public List<UserSkillResponse> getSkillsByUser(String userId) { // ✅
        return userSkillRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<UserSkillResponse> getSkillsBySkillId(Long skillId) {
        return userSkillRepository.findBySkillId(skillId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<UserSkillResponse> getSkillsByUserAndLevel(String userId, Level level) { // ✅
        return userSkillRepository.findByUserIdAndLevel(userId, level)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserSkillResponse getUserSkillById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public UserSkillResponse updateUserSkill(Long id, UserSkillRequest request) {
        UserSkill userSkill = findById(id);
        Skill skill = skillService.findById(request.getSkillId());
        userSkill.setSkill(skill);
        userSkill.setLevel(request.getLevel());
        userSkill.setYearsOfExperience(request.getYearsOfExperience());
        return toResponse(userSkillRepository.save(userSkill));
    }

    public void deleteUserSkill(Long id) {
        if (!userSkillRepository.existsById(id)) {
            throw new ResourceNotFoundException("UserSkill not found with id: " + id);
        }
        userSkillRepository.deleteById(id);
    }

    private UserSkillResponse toResponse(UserSkill us) {
        return UserSkillResponse.builder()
                .id(us.getId())
                .userId(us.getUserId())
                .skill(SkillResponse.builder()
                        .id(us.getSkill().getId())
                        .name(us.getSkill().getName())
                        .build())
                .level(us.getLevel())
                .yearsOfExperience(us.getYearsOfExperience())
                .build();
    }

    public UserSkill findById(Long id) {
        return userSkillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserSkill not found with id: " + id));
    }
}