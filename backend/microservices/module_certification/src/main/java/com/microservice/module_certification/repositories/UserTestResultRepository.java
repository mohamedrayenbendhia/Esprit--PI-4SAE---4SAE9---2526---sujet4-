package com.microservice.module_certification.repositories;

import com.microservice.module_certification.entities.UserTestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

@Repository
public interface UserTestResultRepository extends JpaRepository<UserTestResult, Long> {
    List<UserTestResult> findByUserId(String userId); // ✅
    boolean existsByUserIdAndTestIdAndIsPassed(String userId, Long testId, boolean isPassed);
    Optional<UserTestResult> findTopByUserIdAndTestIdOrderByLastAttemptAtDesc(String userId, Long testId);
    @Query("SELECT r FROM UserTestResult r JOIN FETCH r.test WHERE r.isPassed = false " +
            "AND r.notificationSent = false " +
            "AND r.lastAttemptAt <= :cooldownLimit " +
            "AND r.lastAttemptAt >= :windowStart")
    List<UserTestResult> findExpiredCooldowns(
            @Param("cooldownLimit") LocalDateTime cooldownLimit,
            @Param("windowStart") LocalDateTime windowStart);
}