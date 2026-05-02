package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.dto.advanced.*;
import com.smartfreelance.microservice.complaintservice.entity.*;
import com.smartfreelance.microservice.complaintservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.complaintservice.notification.ComplaintNotificationEvent;
import com.smartfreelance.microservice.complaintservice.notification.ComplaintNotificationService;
import com.smartfreelance.microservice.complaintservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MediationServiceImpl implements MediationService {

    private final MediationSessionRepository  sessionRepo;
    private final MediationEvidenceRepository evidenceRepo;
    private final ComplaintRepository         complaintRepo;
    private final SanctionService             sanctionService;
    private final ComplaintNotificationService notificationService;

    @Override
    public MediationSessionResponse openSession(String complaintId, String adminId, OpenMediationRequest req) {
        if (sessionRepo.existsByComplaintId(complaintId))
            throw new IllegalStateException("Une session de médiation existe déjà pour cette réclamation.");

        Complaint c = complaintRepo.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found: " + complaintId));

        LocalDateTime now = LocalDateTime.now();
        MediationSession session = MediationSession.builder()
                .complaintId(complaintId)
                .status(MediationSession.MediationStatus.EVIDENCE_PHASE)
                .openedByAdminId(adminId)
                .evidenceDeadline(now.plusHours(req.getEvidenceWindowHours()))
                .decisionDeadline(now.plusHours(req.getEvidenceWindowHours() + req.getDecisionWindowHours()))
                .build();

        c.setStatus(Complaint.Status.ESCALATED);
        complaintRepo.save(c);
        MediationSession saved = sessionRepo.save(session);
        log.info("Mediation session opened for complaint {} by admin {}", complaintId, adminId);

        // GAP #1a — notifier toutes les parties de l'ouverture de médiation
        notificationService.handle(ComplaintNotificationEvent.builder()
                .eventType(ComplaintNotificationEvent.EventType.MEDIATION_OPENED)
                .complaintId(c.getId())
                .ticketNumber(c.getTicketNumber())
                .complaintSubject(c.getSubject())
                .reporterId(c.getReporterId())
                .reportedUserId(c.getReportedUserId())
                .assignedToId(c.getAssignedToId())
                .build());

        return toResponse(saved);
    }

    @Override
    public MediationSessionResponse submitEvidence(String sessionId, String userId, SubmitEvidenceRequest req) {
        MediationSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mediation session not found: " + sessionId));

        if (session.getStatus() != MediationSession.MediationStatus.EVIDENCE_PHASE)
            throw new IllegalStateException("La phase de dépôt de preuves est terminée.");
        if (LocalDateTime.now().isAfter(session.getEvidenceDeadline()))
            throw new IllegalStateException("Le délai de dépôt de preuves est dépassé.");

        Complaint c = complaintRepo.findById(session.getComplaintId())
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));

        MediationEvidence.PartyType partyType = userId.equals(c.getReporterId())
                ? MediationEvidence.PartyType.COMPLAINANT
                : MediationEvidence.PartyType.REPORTED;

        MediationEvidence evidence = MediationEvidence.builder()
                .sessionId(sessionId)
                .submittedByUserId(userId)
                .partyType(partyType)
                .description(req.getDescription())
                .attachments(req.getAttachments())
                .build();

        evidenceRepo.save(evidence);
        return toResponse(session);
    }

    @Override
    @Transactional(readOnly = true)
    public MediationSessionResponse getSession(String complaintId) {
        MediationSession session = sessionRepo.findByComplaintId(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("No mediation session for complaint: " + complaintId));
        return toResponse(session);
    }

    @Override
    public MediationSessionResponse decide(String sessionId, String adminId, MediationDecisionRequest req) {
        MediationSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mediation session not found: " + sessionId));

        session.setStatus(MediationSession.MediationStatus.CLOSED);
        session.setOutcome(req.getOutcome());
        session.setAdminReasoning(req.getReasoning());
        session.setDecidedByAdminId(adminId);
        session.setClosedAt(LocalDateTime.now());

        // Appliquer sanction si demandée
        if (req.getTargetUserId() != null && req.getSanction() != null) {
            UserSanction.SanctionType type = UserSanction.SanctionType.valueOf(req.getSanction().name());
            sanctionService.applyManual(req.getTargetUserId(),
                    "Sanction issue de médiation — " + req.getReasoning(), type, adminId);
        }

        // Clôturer la réclamation
        Complaint c = complaintRepo.findById(session.getComplaintId()).orElseThrow();
        c.setStatus(Complaint.Status.RESOLVED);
        c.setResolutionType(Complaint.ResolutionType.MEDIATION);
        c.setResolution("Décision de médiation : " + req.getOutcome() + " — " + req.getReasoning());
        c.setResolvedAt(LocalDateTime.now());
        complaintRepo.save(c);

        log.info("Mediation session {} decided by admin {}: {}", sessionId, adminId, req.getOutcome());

        // GAP #1a — notifier toutes les parties de la décision de médiation
        notificationService.handle(ComplaintNotificationEvent.builder()
                .eventType(ComplaintNotificationEvent.EventType.MEDIATION_DECIDED)
                .complaintId(c.getId())
                .ticketNumber(c.getTicketNumber())
                .complaintSubject(c.getSubject())
                .reporterId(c.getReporterId())
                .reportedUserId(c.getReportedUserId())
                .assignedToId(c.getAssignedToId())
                .extraContext(req.getOutcome() != null ? req.getOutcome().name() : null)
                .build());

        return toResponse(sessionRepo.save(session));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediationSessionResponse> getSessionsByStatus(MediationSession.MediationStatus status) {
        return sessionRepo.findByStatus(status).stream().map(this::toResponse).toList();
    }

    private MediationSessionResponse toResponse(MediationSession s) {
        List<MediationSessionResponse.EvidenceItem> evidences = evidenceRepo.findBySessionId(s.getId())
                .stream().map(e -> MediationSessionResponse.EvidenceItem.builder()
                        .id(e.getId())
                        .submittedByUserId(e.getSubmittedByUserId())
                        .partyType(e.getPartyType())
                        .description(e.getDescription())
                        .attachments(e.getAttachments())
                        .submittedAt(e.getSubmittedAt())
                        .build()).toList();

        return MediationSessionResponse.builder()
                .id(s.getId())
                .complaintId(s.getComplaintId())
                .status(s.getStatus())
                .evidenceDeadline(s.getEvidenceDeadline())
                .decisionDeadline(s.getDecisionDeadline())
                .openedByAdminId(s.getOpenedByAdminId())
                .decidedByAdminId(s.getDecidedByAdminId())
                .outcome(s.getOutcome())
                .adminReasoning(s.getAdminReasoning())
                .createdAt(s.getCreatedAt())
                .closedAt(s.getClosedAt())
                .evidences(evidences)
                .build();
    }
}
