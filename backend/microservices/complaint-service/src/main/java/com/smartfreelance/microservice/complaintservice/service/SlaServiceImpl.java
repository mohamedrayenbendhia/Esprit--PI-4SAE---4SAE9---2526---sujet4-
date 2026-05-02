package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.dto.advanced.CreateSlaRuleRequest;
import com.smartfreelance.microservice.complaintservice.dto.advanced.SlaRuleResponse;
import com.smartfreelance.microservice.complaintservice.dto.advanced.SlaTrackingResponse;
import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.entity.SlaRule;
import com.smartfreelance.microservice.complaintservice.entity.SlaTracking;
import com.smartfreelance.microservice.complaintservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.complaintservice.notification.ComplaintNotificationEvent;
import com.smartfreelance.microservice.complaintservice.notification.ComplaintNotificationService;
import com.smartfreelance.microservice.complaintservice.repository.ComplaintRepository;
import com.smartfreelance.microservice.complaintservice.repository.SlaRuleRepository;
import com.smartfreelance.microservice.complaintservice.repository.SlaTrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SlaServiceImpl implements SlaService {

    private final SlaRuleRepository           slaRuleRepo;
    private final SlaTrackingRepository        slaTrackingRepo;
    private final ComplaintRepository          complaintRepo;
    private final ComplaintNotificationService notificationService;

    // ── Règles SLA ────────────────────────────────────────────────────────

    @Override
    public SlaRuleResponse createRule(CreateSlaRuleRequest req) {
        if (slaRuleRepo.existsByPriority(req.getPriority()))
            throw new IllegalArgumentException("Une règle SLA existe déjà pour la priorité : " + req.getPriority());
        SlaRule rule = SlaRule.builder()
                .priority(req.getPriority())
                .maxFirstResponseHours(req.getMaxFirstResponseHours())
                .maxResolutionHours(req.getMaxResolutionHours())
                .warningThresholdHours(req.getWarningThresholdHours())
                .autoEscalate(req.isAutoEscalate())
                .build();
        return toRuleResponse(slaRuleRepo.save(rule));
    }

    @Override
    public SlaRuleResponse updateRule(String ruleId, CreateSlaRuleRequest req) {
        SlaRule rule = slaRuleRepo.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("SLA rule not found: " + ruleId));
        rule.setPriority(req.getPriority());
        rule.setMaxFirstResponseHours(req.getMaxFirstResponseHours());
        rule.setMaxResolutionHours(req.getMaxResolutionHours());
        rule.setWarningThresholdHours(req.getWarningThresholdHours());
        rule.setAutoEscalate(req.isAutoEscalate());
        return toRuleResponse(slaRuleRepo.save(rule));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlaRuleResponse> getAllRules() {
        return slaRuleRepo.findAll().stream().map(this::toRuleResponse).toList();
    }

    @Override
    public void deleteRule(String ruleId) {
        slaRuleRepo.deleteById(ruleId);
    }

    // ── Tracking ──────────────────────────────────────────────────────────

    @Override
    public void initTracking(String complaintId, Complaint.Priority priority) {
        if (slaTrackingRepo.findByComplaintId(complaintId).isPresent()) return;
        slaRuleRepo.findByPriority(priority).ifPresent(rule -> {
            LocalDateTime now = LocalDateTime.now();
            SlaTracking tracking = SlaTracking.builder()
                    .complaintId(complaintId)
                    .firstResponseDeadline(now.plusHours(rule.getMaxFirstResponseHours()))
                    .resolutionDeadline(now.plusHours(rule.getMaxResolutionHours()))
                    .build();
            slaTrackingRepo.save(tracking);
            log.info("SLA tracking initialized for complaint {} (priority {})", complaintId, priority);
        });
    }

    @Override
    public void recordFirstResponse(String complaintId) {
        slaTrackingRepo.findByComplaintId(complaintId).ifPresent(t -> {
            if (t.getFirstResponseAt() == null) {
                t.setFirstResponseAt(LocalDateTime.now());
                slaTrackingRepo.save(t);
            }
        });
    }

    @Override
    public void recordResolution(String complaintId) {
        slaTrackingRepo.findByComplaintId(complaintId).ifPresent(t -> {
            t.setResolvedAt(LocalDateTime.now());
            slaTrackingRepo.save(t);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public SlaTrackingResponse getTracking(String complaintId) {
        SlaTracking t = slaTrackingRepo.findByComplaintId(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("No SLA tracking for complaint: " + complaintId));
        return toTrackingResponse(t);
    }

    // ── Scheduler : détection des breaches ───────────────────────────────

    @Override
    public void processBreaches() {
        LocalDateTime now = LocalDateTime.now();

        // Première réponse dépassée
        List<SlaTracking> frBreaches = slaTrackingRepo.findFirstResponseBreaches(now);
        frBreaches.forEach(t -> {
            t.setFirstResponseBreached(true);
            slaTrackingRepo.save(t);
            // Escalade priorité → CRITICAL + GAP #1d notification
            complaintRepo.findById(t.getComplaintId()).ifPresent(c -> {
                if (c.getPriority() != Complaint.Priority.CRITICAL) {
                    c.setPriority(Complaint.Priority.CRITICAL);
                    complaintRepo.save(c);
                    log.warn("SLA BREACH (first response) — complaint {} escalated to CRITICAL", c.getId());
                }
                notificationService.handle(ComplaintNotificationEvent.builder()
                        .eventType(ComplaintNotificationEvent.EventType.SLA_BREACH)
                        .complaintId(c.getId())
                        .ticketNumber(c.getTicketNumber())
                        .complaintSubject(c.getSubject())
                        .reporterId(c.getReporterId())
                        .assignedToId(c.getAssignedToId())
                        .extraContext("FIRST_RESPONSE")
                        .build());
            });
        });

        // Résolution dépassée
        List<SlaTracking> resBreaches = slaTrackingRepo.findResolutionBreaches(now);
        resBreaches.forEach(t -> {
            t.setResolutionBreached(true);
            slaTrackingRepo.save(t);
            complaintRepo.findById(t.getComplaintId()).ifPresent(c -> {
                if (c.getStatus() != Complaint.Status.ESCALATED
                        && c.getStatus() != Complaint.Status.RESOLVED
                        && c.getStatus() != Complaint.Status.CLOSED) {

                    // Vérifier si la règle SLA de cette priorité a auto_escalate activé
                    boolean shouldAutoEscalate = slaRuleRepo.findByPriority(c.getPriority())
                            .map(SlaRule::isAutoEscalate)
                            .orElse(false);

                    c.setStatus(Complaint.Status.ESCALATED);

                    if (shouldAutoEscalate) {
                        // Désassigner de l'agent → retour dans la file admin
                        String previousAgent = c.getAssignedToId();
                        c.setAssignedToId(null);
                        complaintRepo.save(c);
                        log.warn("SLA BREACH (resolution) — complaint {} AUTO-ESCALATED to admin queue (was assigned to agent {})",
                                c.getId(), previousAgent);
                    } else {
                        complaintRepo.save(c);
                        log.warn("SLA BREACH (resolution) — complaint {} escalated (status only)", c.getId());
                    }
                }
                notificationService.handle(ComplaintNotificationEvent.builder()
                        .eventType(ComplaintNotificationEvent.EventType.SLA_BREACH)
                        .complaintId(c.getId())
                        .ticketNumber(c.getTicketNumber())
                        .complaintSubject(c.getSubject())
                        .reporterId(c.getReporterId())
                        .assignedToId(c.getAssignedToId())
                        .extraContext("RESOLUTION")
                        .build());
            });
        });

        if (!frBreaches.isEmpty() || !resBreaches.isEmpty())
            log.info("SLA breach check: {} first-response breaches, {} resolution breaches",
                    frBreaches.size(), resBreaches.size());
    }

    // ── Mappers ───────────────────────────────────────────────────────────

    private SlaRuleResponse toRuleResponse(SlaRule r) {
        return SlaRuleResponse.builder()
                .id(r.getId())
                .priority(r.getPriority())
                .maxFirstResponseHours(r.getMaxFirstResponseHours())
                .maxResolutionHours(r.getMaxResolutionHours())
                .warningThresholdHours(r.getWarningThresholdHours())
                .autoEscalate(r.isAutoEscalate())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private SlaTrackingResponse toTrackingResponse(SlaTracking t) {
        double progress = 0;
        if (t.getResolutionDeadline() != null && t.getCreatedAt() != null) {
            long total   = ChronoUnit.MINUTES.between(t.getCreatedAt(), t.getResolutionDeadline());
            long elapsed = ChronoUnit.MINUTES.between(t.getCreatedAt(), LocalDateTime.now());
            progress = total > 0 ? Math.min((double) elapsed / total * 100, 200) : 0;
        }
        return SlaTrackingResponse.builder()
                .id(t.getId())
                .complaintId(t.getComplaintId())
                .firstResponseDeadline(t.getFirstResponseDeadline())
                .resolutionDeadline(t.getResolutionDeadline())
                .firstResponseBreached(t.isFirstResponseBreached())
                .resolutionBreached(t.isResolutionBreached())
                .firstResponseAt(t.getFirstResponseAt())
                .resolvedAt(t.getResolvedAt())
                .resolutionProgressPercent(progress)
                .build();
    }
}
