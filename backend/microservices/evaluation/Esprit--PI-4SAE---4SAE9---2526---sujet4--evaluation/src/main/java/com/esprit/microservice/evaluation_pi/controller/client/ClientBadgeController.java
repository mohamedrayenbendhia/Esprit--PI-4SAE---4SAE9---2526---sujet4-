package com.esprit.microservice.evaluation_pi.controller.client;

import com.esprit.microservice.evaluation_pi.entities.Badge;
import com.esprit.microservice.evaluation_pi.entities.UserBadge;
import com.esprit.microservice.evaluation_pi.services.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/client/badges")
@RequiredArgsConstructor
public class ClientBadgeController {

    private final BadgeService badgeService;

    //  Client peut voir tous les badges disponibles
    @GetMapping
    public ResponseEntity<List<Badge>> getAllBadges() {
        List<Badge> badges = badgeService.getAllBadges();
        return ResponseEntity.ok(badges);
    }

    // 2. Client peut voir les badges d'un freelancer x
    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<List<UserBadge>> getFreelancerBadges(
            @PathVariable String freelancerId) {
        List<UserBadge> userBadges = badgeService.getUserBadges(freelancerId);
        return ResponseEntity.ok(userBadges);
    }

    // 3. Client peut voir les détails d'un badge spécifique
    @GetMapping("/{badgeId}")
    public ResponseEntity<Badge> getBadgeDetails(
            @PathVariable Long badgeId) {  
        Badge badge = badgeService.getBadgeById(badgeId);
        return ResponseEntity.ok(badge);
    }
}