package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.dto.advanced.MediationDecisionRequest;
import com.smartfreelance.microservice.complaintservice.dto.advanced.MediationSessionResponse;
import com.smartfreelance.microservice.complaintservice.dto.advanced.OpenMediationRequest;
import com.smartfreelance.microservice.complaintservice.dto.advanced.SubmitEvidenceRequest;
import com.smartfreelance.microservice.complaintservice.entity.MediationSession;

import java.util.List;

public interface MediationService {
    MediationSessionResponse openSession(String complaintId, String adminId, OpenMediationRequest req);
    MediationSessionResponse submitEvidence(String sessionId, String userId, SubmitEvidenceRequest req);
    MediationSessionResponse getSession(String complaintId);
    MediationSessionResponse decide(String sessionId, String adminId, MediationDecisionRequest req);
    List<MediationSessionResponse> getSessionsByStatus(MediationSession.MediationStatus status);
}
