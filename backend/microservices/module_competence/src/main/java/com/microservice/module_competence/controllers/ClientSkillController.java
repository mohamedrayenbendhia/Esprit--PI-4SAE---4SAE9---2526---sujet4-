package com.microservice.module_competence.controllers;

import com.microservice.module_competence.dto.*;
import com.microservice.module_competence.enums.Level;
import com.microservice.module_competence.services.SkillService;
import com.microservice.module_competence.services.UserSkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/client/skills")
@RequiredArgsConstructor
public class ClientSkillController {

    private final SkillService skillService;
    private final UserSkillService userSkillService;

    // GET /api/client/skills
    // Voir tous les skills disponibles
    @GetMapping
    public ResponseEntity<List<SkillResponse>> getAllSkills() {
        return ResponseEntity.ok(skillService.getAllSkills());
    }

    // GET /api/client/skills/user-skills/{userId}
    @GetMapping("/user-skills/{userId}")
    public ResponseEntity<List<UserSkillResponse>> getFreelancerSkills(
            @PathVariable String userId) { // ✅
        return ResponseEntity.ok(userSkillService.getSkillsByUser(userId));
    }

    @GetMapping("/user-skills/skill/{skillId}")
    public ResponseEntity<List<UserSkillResponse>> getFreelancersBySkill(
            @PathVariable Long skillId) {
        return ResponseEntity.ok(userSkillService.getSkillsBySkillId(skillId));
    }
}