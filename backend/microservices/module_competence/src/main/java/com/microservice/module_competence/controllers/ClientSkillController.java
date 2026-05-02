package com.microservice.module_competence.controllers;

import com.microservice.module_competence.dto.*;
import com.microservice.module_competence.enums.Level;
import com.microservice.module_competence.services.EndorsementService;
import com.microservice.module_competence.services.SkillService;
import com.microservice.module_competence.services.UserSkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;


@RestController
@RequestMapping("/api/client/skills")
@RequiredArgsConstructor
public class ClientSkillController {

    private final SkillService skillService;
    private final UserSkillService userSkillService;
    private final EndorsementService endorsementService;

    // POST /api/client/skills/endorse/{freelancerId}/{skillId}
    @PostMapping("/endorse/{freelancerId}/{skillId}")
    public ResponseEntity<Map<String, Long>> endorse(
            @PathVariable String freelancerId,
            @PathVariable Long skillId,
            Authentication authentication) {
        String clientId = extractUserId(authentication);
        long count = endorsementService.endorse(clientId, freelancerId, skillId);
        return ResponseEntity.ok(Map.of("endorsements", count));
    }

    // DELETE /api/client/skills/endorse/{freelancerId}/{skillId}
    @DeleteMapping("/endorse/{freelancerId}/{skillId}")
    public ResponseEntity<Map<String, Long>> removeEndorsement(
            @PathVariable String freelancerId,
            @PathVariable Long skillId,
            Authentication authentication) {
        String clientId = extractUserId(authentication);
        long count = endorsementService.removeEndorsement(clientId, freelancerId, skillId);
        return ResponseEntity.ok(Map.of("endorsements", count));
    }

    // GET /api/client/skills/endorse/{freelancerId}/{skillId}/count
    @GetMapping("/endorse/{freelancerId}/{skillId}/count")
    public ResponseEntity<Map<String, Long>> getCount(
            @PathVariable String freelancerId,
            @PathVariable Long skillId) {
        return ResponseEntity.ok(Map.of("endorsements",
                endorsementService.getEndorsementCount(freelancerId, skillId)));
    }

    // GET /api/client/skills/endorse/{freelancerId}/{skillId}/has-endorsed
    @GetMapping("/endorse/{freelancerId}/{skillId}/has-endorsed")
    public ResponseEntity<Map<String, Boolean>> hasEndorsed(
            @PathVariable String freelancerId,
            @PathVariable Long skillId,
            Authentication authentication) {
        String clientId = extractUserId(authentication);
        return ResponseEntity.ok(Map.of("hasEndorsed",
                endorsementService.hasEndorsed(clientId, freelancerId, skillId)));
    }

    private String extractUserId(Authentication authentication) {
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof io.jsonwebtoken.Claims claims) {
                return claims.get("userId", String.class);
            }
        }
        throw new RuntimeException("Unable to extract userId from token");
    }

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