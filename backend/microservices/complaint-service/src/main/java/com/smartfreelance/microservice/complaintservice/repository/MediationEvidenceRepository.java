package com.smartfreelance.microservice.complaintservice.repository;

import com.smartfreelance.microservice.complaintservice.entity.MediationEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediationEvidenceRepository extends JpaRepository<MediationEvidence, String> {
    List<MediationEvidence> findBySessionId(String sessionId);
    List<MediationEvidence> findBySessionIdAndPartyType(String sessionId, MediationEvidence.PartyType partyType);
    long countBySessionId(String sessionId);
}
