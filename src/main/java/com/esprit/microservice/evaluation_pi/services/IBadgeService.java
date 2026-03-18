package com.esprit.microservice.evaluation_pi.services;

import com.esprit.microservice.evaluation_pi.entities.Badge;
import com.esprit.microservice.evaluation_pi.entities.UserBadge;
import java.util.List;

public interface IBadgeService {
    Badge createBadge(Badge badge);
    List<Badge> getAllBadges();
    List<UserBadge> getUserBadges(String userId);
    void checkAndAssignBadges(String userId, Double averageScore, Long totalProjects);
}