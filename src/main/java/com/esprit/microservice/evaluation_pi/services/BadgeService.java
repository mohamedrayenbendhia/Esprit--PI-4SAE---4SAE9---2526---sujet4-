package com.esprit.microservice.evaluation_pi.services;

import com.esprit.microservice.evaluation_pi.entities.Badge;
import com.esprit.microservice.evaluation_pi.entities.UserBadge;
import com.esprit.microservice.evaluation_pi.repositories.BadgeRepository;
import com.esprit.microservice.evaluation_pi.repositories.UserBadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BadgeService implements IBadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;

    @Override
    public Badge createBadge(Badge badge) {
        badge.setCreatedAt(LocalDateTime.now());
        return badgeRepository.save(badge);
    }

    public Badge getBadgeById(Long badgeId) {
        return badgeRepository.findById(badgeId)
                .orElseThrow(() -> new RuntimeException("Badge non trouvé avec l'id: " + badgeId));
    }

    public Badge updateBadge(Long badgeId, Badge badgeDetails) {
        Badge badge = getBadgeById(badgeId);
        badge.setName(badgeDetails.getName());
        badge.setDescription(badgeDetails.getDescription());
        badge.setMinScore(badgeDetails.getMinScore());
        badge.setMinProjects(badgeDetails.getMinProjects());
        badge.setIcon(badgeDetails.getIcon());
        return badgeRepository.save(badge);
    }

    public void deleteBadge(Long badgeId) {
        Badge badge = getBadgeById(badgeId);
        badgeRepository.delete(badge);
    }

    @Override
    public List<Badge> getAllBadges() {
        return badgeRepository.findAll();
    }

    @Override
    public List<UserBadge> getUserBadges(String userId) { // ✅ String
        return userBadgeRepository.findByUserId(userId);
    }

    @Override
    public void checkAndAssignBadges(String userId, Double averageScore, Long totalProjects) { // ✅ String
        System.out.println("=== Attribution des badges pour user " + userId + " ===");
        System.out.println("Score: " + averageScore + ", Projets: " + totalProjects);

        List<Badge> allBadges = badgeRepository.findAll();
        System.out.println("Nombre de badges disponibles: " + allBadges.size());

        for (Badge badge : allBadges) {
            System.out.println("Vérification badge: " + badge.getName());

            if (averageScore >= badge.getMinScore() && totalProjects >= badge.getMinProjects()) {
                boolean hasBadge = userBadgeRepository.findByUserId(userId).stream()
                        .anyMatch(ub -> ub.getBadge().getId().equals(badge.getId()));

                if (!hasBadge) {
                    System.out.println("Attribution du badge: " + badge.getName());
                    UserBadge userBadge = new UserBadge();
                    userBadge.setUserId(userId); // ✅ String
                    userBadge.setBadge(badge);
                    userBadge.setAssignedAt(LocalDateTime.now());
                    userBadgeRepository.save(userBadge);
                    System.out.println("Badge attribué avec succès!");
                }
            }
        }
    }
}