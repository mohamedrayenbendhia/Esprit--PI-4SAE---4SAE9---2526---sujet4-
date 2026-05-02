package com.smartfreelance.microservice.complaintservice.repository;

import com.smartfreelance.microservice.complaintservice.entity.UserSanction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface UserSanctionRepository extends JpaRepository<UserSanction, String> {

    List<UserSanction> findByUserId(String userId);
    List<UserSanction> findByUserIdAndActiveTrue(String userId);
    long countByUserIdAndActiveTrue(String userId);
    long countByUserId(String userId);
    boolean existsByUserIdAndTypeAndActiveTrue(String userId, UserSanction.SanctionType type);

    /** Désactive automatiquement les suspensions temporaires expirées. */
    @Modifying
    @Transactional
    @Query("UPDATE UserSanction s SET s.active = false, s.liftedAt = :now " +
           "WHERE s.active = true AND s.expiresAt IS NOT NULL AND s.expiresAt < :now")
    int expireOldSanctions(LocalDateTime now);
}
