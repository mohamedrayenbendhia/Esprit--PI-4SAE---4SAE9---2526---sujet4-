package com.smartfreelance.microservice.complaintservice.repository;

import com.smartfreelance.microservice.complaintservice.entity.MediationSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MediationSessionRepository extends JpaRepository<MediationSession, String> {
    Optional<MediationSession> findByComplaintId(String complaintId);
    boolean existsByComplaintId(String complaintId);
    List<MediationSession> findByStatus(MediationSession.MediationStatus status);
    List<MediationSession> findByOpenedByAdminId(String adminId);
}
