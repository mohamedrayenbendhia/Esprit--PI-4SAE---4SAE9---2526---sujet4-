package com.smartfreelance.microservice.complaintservice.repository;

import com.smartfreelance.microservice.complaintservice.entity.SlaTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SlaTrackingRepository extends JpaRepository<SlaTracking, String> {

    Optional<SlaTracking> findByComplaintId(String complaintId);

    /** Réclamations dont la deadline de première réponse est dépassée et non encore breachée. */
    @Query("SELECT s FROM SlaTracking s WHERE s.firstResponseBreached = false " +
           "AND s.firstResponseAt IS NULL AND s.firstResponseDeadline < :now")
    List<SlaTracking> findFirstResponseBreaches(LocalDateTime now);

    /** Réclamations dont la deadline de résolution est dépassée et non encore breachée. */
    @Query("SELECT s FROM SlaTracking s WHERE s.resolutionBreached = false " +
           "AND s.resolvedAt IS NULL AND s.resolutionDeadline < :now")
    List<SlaTracking> findResolutionBreaches(LocalDateTime now);

    /** Réclamations approchant leur deadline de résolution (pour alertes préventives). */
    @Query("SELECT s FROM SlaTracking s WHERE s.resolutionBreached = false " +
           "AND s.resolvedAt IS NULL AND s.resolutionDeadline BETWEEN :now AND :threshold")
    List<SlaTracking> findApproachingDeadlines(LocalDateTime now, LocalDateTime threshold);
}
