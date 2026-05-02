package com.microservice.module_competence.controllers;

import com.microservice.module_competence.dto.*;
import com.microservice.module_competence.enums.Level;
import com.microservice.module_competence.services.SkillService;
import com.microservice.module_competence.services.UserSkillService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/freelancer/skills")
@RequiredArgsConstructor
public class FreelancerSkillController {

    private final UserSkillService userSkillService;
    private final SkillService skillService;

    @GetMapping
    public ResponseEntity<List<SkillResponse>> getAllSkills() {
        return ResponseEntity.ok(skillService.getAllSkills());
    }

    @PostMapping("/user-skills")
    public ResponseEntity<UserSkillResponse> addSkill(
            @Valid @RequestBody UserSkillRequest request,
            Authentication authentication) {
        request.setUserId(extractUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userSkillService.addSkillToUser(request));
    }

    @GetMapping("/user-skills/me")
    public ResponseEntity<List<UserSkillResponse>> getMySkills(Authentication authentication) {
        return ResponseEntity.ok(userSkillService.getSkillsByUser(extractUserId(authentication)));
    }

    @GetMapping("/user-skills/me/level")
    public ResponseEntity<List<UserSkillResponse>> getMySkillsByLevel(
            @RequestParam Level level,
            Authentication authentication) {
        return ResponseEntity.ok(userSkillService.getSkillsByUserAndLevel(extractUserId(authentication), level));
    }

    @PutMapping("/user-skills/{id}")
    public ResponseEntity<UserSkillResponse> updateSkill(
            @PathVariable Long id,
            @Valid @RequestBody UserSkillRequest request,
            Authentication authentication) {
        request.setUserId(extractUserId(authentication));
        return ResponseEntity.ok(userSkillService.updateUserSkill(id, request));
    }

    @DeleteMapping("/user-skills/{id}")
    public ResponseEntity<Void> deleteSkill(@PathVariable Long id) {
        userSkillService.deleteUserSkill(id);
        return ResponseEntity.noContent().build();
    }

    private String extractUserId(Authentication authentication) {
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof io.jsonwebtoken.Claims claims) {
                String userId = claims.get("userId", String.class);
                if (userId != null) return userId;
            }
        }
        throw new RuntimeException("Unable to extract userId from token");
    }
}