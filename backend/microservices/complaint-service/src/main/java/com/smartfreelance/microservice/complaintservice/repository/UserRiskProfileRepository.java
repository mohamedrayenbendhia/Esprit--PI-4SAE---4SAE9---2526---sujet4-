package com.smartfreelance.microservice.complaintservice.repository;

import com.smartfreelance.microservice.complaintservice.entity.UserRiskProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRiskProfileRepository extends JpaRepository<UserRiskProfile, String> {
    Optional<UserRiskProfile> findByUserId(String userId);
    boolean existsByUserId(String userId);
    List<UserRiskProfile> findByRiskLevel(UserRiskProfile.RiskLevel level);

    @Query("SELECT u FROM UserRiskProfile u WHERE u.riskScore >= :minScore ORDER BY u.riskScore DESC")
    List<UserRiskProfile> findHighRiskUsers(int minScore);

    @Query("SELECT COUNT(u) FROM UserRiskProfile u WHERE u.riskLevel = 'CRITICAL'")
    long countCriticalUsers();
}
