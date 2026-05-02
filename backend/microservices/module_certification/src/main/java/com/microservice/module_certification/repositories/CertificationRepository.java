package com.microservice.module_certification.repositories;

import com.microservice.module_certification.entities.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;

@Repository
public interface CertificationRepository extends JpaRepository<Certification, Long> {
    List<Certification> findByUserId(String userId); // ✅
    boolean existsByUserIdAndUserSkillId(String userId, Long userSkillId);
    @Query("SELECT c FROM Certification c JOIN FETCH c.test WHERE c.isExpired = false AND c.expiresAt <= :today AND c.smsSent = false")
    List<Certification> findExpiredCertifications(@Param("today") LocalDate today);
}