package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.complaintservice.notification.ComplaintNotificationEvent;
import com.smartfreelance.microservice.complaintservice.notification.ComplaintNotificationService;
import com.smartfreelance.microservice.complaintservice.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReopenServiceImpl implements ReopenService {

    private final ComplaintRepository complaintRepo;
    private final ComplaintNotificationService notificationService;

    private static final int  MAX_REOPENS      = 2;
    private static final long REOPEN_WINDOW_DAYS = 7;

    @Override
    public Complaint reopen(String complaintId, String userId, String reason) {
        Complaint c = complaintRepo.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found: " + complaintId));

        // Seul le déposant peut rouvrir
        if (!userId.equals(c.getReporterId()))
            throw new IllegalArgumentException("Seul le déposant peut rouvrir cette réclamation.");

        // Vérif statut
        if (c.getStatus() != Complaint.Status.RESOLVED && c.getStatus() != Complaint.Status.CLOSED)
            throw new IllegalStateException("Seules les réclamations RESOLVED ou CLOSED peuvent être rouvertes.");

        // Vérif quota
        if (c.getReopenCount() >= MAX_REOPENS)
            throw new IllegalStateException("Maximum " + MAX_REOPENS + " réouvertures atteint pour cette réclamation.");

        // Vérif délai 7 jours
        LocalDateTime reference = c.getClosedAt() != null ? c.getClosedAt() : c.getResolvedAt();
        if (reference != null && reference.isBefore(LocalDateTime.now().minusDays(REOPEN_WINDOW_DAYS)))
            throw new IllegalStateException("Le délai de réouverture de " + REOPEN_WINDOW_DAYS + " jours est dépassé.");

        // Mémoriser l'agent précédent avant le reset — il doit être informé de la réouverture
        String previousAgentId = c.getAssignedToId();

        c.setStatus(Complaint.Status.OPEN);
        c.setReopenCount(c.getReopenCount() + 1);
        c.setLastReopenedAt(LocalDateTime.now());
        c.setReopenReason(reason);
        c.setAssignedToId(null); // remet en file d'attente

        Complaint saved = complaintRepo.save(c);
        log.info("Complaint {} reopened by user {} (reopen #{}).", complaintId, userId, saved.getReopenCount());

        // GAP #1b — notifier l'ex-agent (ou l'admin via la queue) et confirmer au reporter
        notificationService.handle(ComplaintNotificationEvent.builder()
                .eventType(ComplaintNotificationEvent.EventType.COMPLAINT_REOPENED)
                .complaintId(saved.getId())
                .ticketNumber(saved.getTicketNumber())
                .complaintSubject(saved.getSubject())
                .reporterId(saved.getReporterId())
                .assignedToId(previousAgentId)
                .extraContext(reason)
                .build());

        return saved;
    }
}
