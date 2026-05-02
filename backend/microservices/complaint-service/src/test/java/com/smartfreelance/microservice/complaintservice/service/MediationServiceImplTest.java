package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.dto.advanced.*;
import com.smartfreelance.microservice.complaintservice.entity.*;
import com.smartfreelance.microservice.complaintservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.complaintservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediationServiceImplTest {

    @Mock private MediationSessionRepository sessionRepo;
    @Mock private MediationEvidenceRepository evidenceRepo;
    @Mock private ComplaintRepository complaintRepo;
    @Mock private SanctionService sanctionService;
    @Mock private com.smartfreelance.microservice.complaintservice.notification.ComplaintNotificationService notificationService;

    @InjectMocks private MediationServiceImpl mediationService;

    private Complaint sampleComplaint;
    private MediationSession sampleSession;

    @BeforeEach
    void setUp() {
        sampleComplaint = new Complaint();
        sampleComplaint.setId("comp-1");
        sampleComplaint.setReporterId("user-reporter");
        sampleComplaint.setStatus(Complaint.Status.OPEN);

        sampleSession = MediationSession.builder()
                .id("sess-1")
                .complaintId("comp-1")
                .status(MediationSession.MediationStatus.EVIDENCE_PHASE)
                .evidenceDeadline(LocalDateTime.now().plusDays(1))
                .build();
    }

    @Test
    void openSession_Success() {
        OpenMediationRequest req = new OpenMediationRequest();
        req.setEvidenceWindowHours(24);
        req.setDecisionWindowHours(48);

        when(sessionRepo.existsByComplaintId("comp-1")).thenReturn(false);
        when(complaintRepo.findById("comp-1")).thenReturn(Optional.of(sampleComplaint));
        when(sessionRepo.save(any(MediationSession.class))).thenReturn(sampleSession);

        MediationSessionResponse res = mediationService.openSession("comp-1", "admin-1", req);

        assertNotNull(res);
        assertEquals(Complaint.Status.ESCALATED, sampleComplaint.getStatus());
        verify(complaintRepo).save(sampleComplaint);
    }

    @Test
    void submitEvidence_Success() {
        SubmitEvidenceRequest req = new SubmitEvidenceRequest();
        req.setDescription("Preuve");

        when(sessionRepo.findById("sess-1")).thenReturn(Optional.of(sampleSession));
        when(complaintRepo.findById("comp-1")).thenReturn(Optional.of(sampleComplaint));
        when(evidenceRepo.findBySessionId("sess-1")).thenReturn(Collections.emptyList());

        mediationService.submitEvidence("sess-1", "user-reporter", req);

        verify(evidenceRepo).save(any(MediationEvidence.class));
    }

    @Test
    void decide_Success() {
        MediationDecisionRequest req = new MediationDecisionRequest();

        // On utilise la première valeur de l'outcome pour la stabilité du test
        if (MediationSession.MediationOutcome.values().length > 0) {
            req.setOutcome(MediationSession.MediationOutcome.values()[0]);
        }

        req.setReasoning("Décision suite à médiation.");
        req.setTargetUserId("user-reported");

        // CORRECTION CRUCIALE :
        // On pointe vers l'énumération INTERNE au DTO comme demandé par l'erreur
        req.setSanction(MediationDecisionRequest.UserSanction.SanctionType.TEMP_SUSPENSION);

        when(sessionRepo.findById("sess-1")).thenReturn(Optional.of(sampleSession));
        when(complaintRepo.findById("comp-1")).thenReturn(Optional.of(sampleComplaint));
        when(sessionRepo.save(any())).thenReturn(sampleSession);

        mediationService.decide("sess-1", "admin-1", req);

        verify(sessionRepo).save(any());
        verify(complaintRepo).save(any());
    }
    @Test
    void submitEvidence_ThrowsException_IfDeadlinePassed() {
        sampleSession.setEvidenceDeadline(LocalDateTime.now().minusHours(1));
        when(sessionRepo.findById("sess-1")).thenReturn(Optional.of(sampleSession));

        SubmitEvidenceRequest req = new SubmitEvidenceRequest();

        assertThrows(IllegalStateException.class,
                () -> mediationService.submitEvidence("sess-1", "user-any", req));
    }

    @Test
    void getSession_NotFound_ThrowsException() {
        when(sessionRepo.findByComplaintId("any")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> mediationService.getSession("any"));
    }
}