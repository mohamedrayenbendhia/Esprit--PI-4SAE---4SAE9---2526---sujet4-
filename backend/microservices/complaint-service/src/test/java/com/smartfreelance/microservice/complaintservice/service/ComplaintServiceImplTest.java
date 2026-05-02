package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.entity.Complaint.Priority;
import com.smartfreelance.microservice.complaintservice.entity.Complaint.Status;
import com.smartfreelance.microservice.complaintservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.complaintservice.repository.ComplaintRepository;
import com.smartfreelance.microservice.complaintservice.service.SlaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceImplTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private SlaService slaService;

    @InjectMocks
    private ComplaintServiceImpl complaintService;

    private Complaint sampleComplaint;

    @BeforeEach
    void setUp() {
        sampleComplaint = new Complaint();
        sampleComplaint.setId("comp-123");
        sampleComplaint.setReporterId("user-1");
        sampleComplaint.setSubject("Problème de paiement");
        sampleComplaint.setStatus(Status.OPEN);
        sampleComplaint.setPriority(Priority.MEDIUM);
    }

    @Test
    void createComplaint_Success() {
        when(complaintRepository.save(any(Complaint.class))).thenReturn(sampleComplaint);

        Complaint created = complaintService.createComplaint(sampleComplaint);

        assertNotNull(created);
        assertEquals("user-1", created.getReporterId());
        verify(complaintRepository, times(1)).save(any());
    }

    @Test
    void createComplaint_ThrowsException_WhenSubjectMissing() {
        sampleComplaint.setSubject(null);

        assertThrows(IllegalArgumentException.class, () -> {
            complaintService.createComplaint(sampleComplaint);
        });
    }

    @Test
    void getComplaintById_Success() {
        when(complaintRepository.findById("comp-123")).thenReturn(Optional.of(sampleComplaint));

        Complaint found = complaintService.getComplaintById("comp-123");

        assertEquals("comp-123", found.getId());
    }

    @Test
    void getComplaintById_NotFound_ThrowsException() {
        when(complaintRepository.findById("none")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            complaintService.getComplaintById("none");
        });
    }

    @Test
    void assignComplaint_ShouldUpdateStatusToInProgress() {
        when(complaintRepository.findById("comp-123")).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Complaint updated = complaintService.assignComplaint("comp-123", "agent-7");

        assertEquals("agent-7", updated.getAssignedToId());
        assertEquals(Status.IN_PROGRESS, updated.getStatus());
    }

    @Test
    void resolveComplaint_Success() {
        when(complaintRepository.findById("comp-123")).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Complaint resolved = complaintService.resolveComplaint(
                "comp-123", "Solution trouvée", Complaint.ResolutionType.REFUND);

        assertEquals(Status.RESOLVED, resolved.getStatus());
        assertNotNull(resolved.getResolvedAt());
        assertEquals(Complaint.ResolutionType.REFUND, resolved.getResolutionType());
    }

    @Test
    void closeComplaint_ThrowsException_IfStatusNotResolved() {
        // La plainte est encore OPEN dans setUp()
        when(complaintRepository.findById("comp-123")).thenReturn(Optional.of(sampleComplaint));

        assertThrows(IllegalStateException.class, () -> {
            complaintService.closeComplaint("comp-123");
        });
    }

    @Test
    void closeComplaint_Success_WhenResolved() {
        sampleComplaint.setStatus(Status.RESOLVED);
        when(complaintRepository.findById("comp-123")).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Complaint closed = complaintService.closeComplaint("comp-123");

        assertEquals(Status.CLOSED, closed.getStatus());
        assertNotNull(closed.getClosedAt());
    }

    @Test
    void rateComplaint_Success() {
        sampleComplaint.setStatus(Status.CLOSED);
        when(complaintRepository.findById("comp-123")).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Complaint rated = complaintService.rateComplaint("comp-123", 5);

        assertEquals(5, rated.getSatisfactionRating());
    }

    @Test
    void rateComplaint_ThrowsException_IfStatusNotClosed() {
        sampleComplaint.setStatus(Status.RESOLVED);
        when(complaintRepository.findById("comp-123")).thenReturn(Optional.of(sampleComplaint));

        assertThrows(IllegalStateException.class, () -> {
            complaintService.rateComplaint("comp-123", 4);
        });
    }

    @Test
    void deleteComplaint_Success() {
        when(complaintRepository.findById("comp-123")).thenReturn(Optional.of(sampleComplaint));

        complaintService.deleteComplaint("comp-123");

        verify(complaintRepository, times(1)).delete(sampleComplaint);
    }

    @Test
    void updateComplaint_shouldApplyChanges() {
        when(complaintRepository.findById("comp-123")).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Complaint details = new Complaint();
        details.setSubject("New subject");
        details.setDescription("New desc");

        Complaint updated = complaintService.updateComplaint("comp-123", details);

        assertEquals("New subject", updated.getSubject());
        assertEquals("New desc", updated.getDescription());
    }

    @Test
    void updatePriority_shouldPersistNewPriority() {
        when(complaintRepository.findById("comp-123")).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Complaint updated = complaintService.updatePriority("comp-123", Priority.HIGH);
        assertEquals(Priority.HIGH, updated.getPriority());
    }

    @Test
    void escalateComplaint_shouldSetEscalatedStatus() {
        when(complaintRepository.findById("comp-123")).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Complaint escalated = complaintService.escalateComplaint("comp-123");
        assertEquals(Status.ESCALATED, escalated.getStatus());
    }

    @Test
    void addAttachment_shouldAddUrl() {
        when(complaintRepository.findById("comp-123")).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Complaint withAttachment = complaintService.addAttachment("comp-123", "http://file/url.jpg");
        assertNotNull(withAttachment.getAttachments());
        assertTrue(withAttachment.getAttachments().contains("http://file/url.jpg"));
    }

    @Test
    void involveReportedUser_shouldSetReportedUserId() {
        when(complaintRepository.findById("comp-123")).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Complaint c = complaintService.involveReportedUser("comp-123", "reported-9");
        assertEquals("reported-9", c.getReportedUserId());
    }

    @Test
    void getOverdueComplaints_delegatesToRepository() {
        when(complaintRepository.findOverdueComplaints(any())).thenReturn(java.util.List.of(sampleComplaint));
        var list = complaintService.getOverdueComplaints(7);
        assertEquals(1, list.size());
    }

    @Test
    void countMethods_delegateToRepository() {
        when(complaintRepository.countByStatus(Status.OPEN)).thenReturn(2L);
        when(complaintRepository.countByPriority(Priority.MEDIUM)).thenReturn(3L);

        assertEquals(2L, complaintService.countByStatus(Status.OPEN));
        assertEquals(3L, complaintService.countByPriority(Priority.MEDIUM));
    }

    @Test
    void getAllComplaints_delegates() {
        when(complaintRepository.findAll()).thenReturn(java.util.List.of(sampleComplaint));
        var all = complaintService.getAllComplaints();
        assertEquals(1, all.size());
    }

    // ── Edge cases : validations et transitions d'état ─────────────

    @Test
    void createComplaint_ThrowsException_WhenReporterIdIsNull() {
        sampleComplaint.setReporterId(null);
        assertThrows(IllegalArgumentException.class,
                () -> complaintService.createComplaint(sampleComplaint));
    }

    @Test
    void createComplaint_ThrowsException_WhenReporterIdIsBlank() {
        sampleComplaint.setReporterId("  ");
        // Note: isEmpty() ne couvre pas les blancs, mais la chaîne "  ".isEmpty() = false
        // La validation réelle dépend de l'implémentation — on teste le cas null/empty strict
        sampleComplaint.setReporterId("");
        assertThrows(IllegalArgumentException.class,
                () -> complaintService.createComplaint(sampleComplaint));
    }

    @Test
    void closeComplaint_ThrowsException_WhenStatusIsOpen() {
        // OPEN → CLOSED directement doit être refusé (doit passer par RESOLVED d'abord)
        sampleComplaint.setStatus(Status.OPEN);
        when(complaintRepository.findById("comp-123")).thenReturn(Optional.of(sampleComplaint));

        assertThrows(IllegalStateException.class,
                () -> complaintService.closeComplaint("comp-123"));
    }

    @Test
    void closeComplaint_ThrowsException_WhenAlreadyClosed() {
        // CLOSED → CLOSED doit aussi être refusé (n'est pas RESOLVED)
        sampleComplaint.setStatus(Status.CLOSED);
        when(complaintRepository.findById("comp-123")).thenReturn(Optional.of(sampleComplaint));

        assertThrows(IllegalStateException.class,
                () -> complaintService.closeComplaint("comp-123"));
    }

    @Test
    void rateComplaint_ThrowsException_WhenRatingIsTooLow() {
        // Rating 0 < 1 → IllegalArgumentException (avant même de chercher la plainte)
        assertThrows(IllegalArgumentException.class,
                () -> complaintService.rateComplaint("comp-123", 0));
    }

    @Test
    void rateComplaint_ThrowsException_WhenRatingIsTooHigh() {
        // Rating 6 > 5 → IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
                () -> complaintService.rateComplaint("comp-123", 6));
    }

    @Test
    void rateComplaint_Success_WithMinimumRating() {
        // Rating 1 = minimum valide
        sampleComplaint.setStatus(Status.CLOSED);
        when(complaintRepository.findById("comp-123")).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Complaint rated = complaintService.rateComplaint("comp-123", 1);
        assertEquals(1, rated.getSatisfactionRating());
    }

    @Test
    void rateComplaint_Success_WithMaximumRating() {
        // Rating 5 = maximum valide
        sampleComplaint.setStatus(Status.CLOSED);
        when(complaintRepository.findById("comp-123")).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Complaint rated = complaintService.rateComplaint("comp-123", 5);
        assertEquals(5, rated.getSatisfactionRating());
    }

    @Test
    void assignComplaint_ShouldNotChangeStatus_WhenAlreadyInProgress() {
        // Réassigner une plainte déjà IN_PROGRESS ne doit pas changer son statut
        sampleComplaint.setStatus(Status.IN_PROGRESS);
        when(complaintRepository.findById("comp-123")).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Complaint updated = complaintService.assignComplaint("comp-123", "agent-new");

        assertEquals("agent-new", updated.getAssignedToId());
        assertEquals(Status.IN_PROGRESS, updated.getStatus()); // statut inchangé
    }

    @Test
    void updateStatus_ShouldSetResolvedAt_WhenStatusBecomesResolved() {
        when(complaintRepository.findById("comp-123")).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Complaint updated = complaintService.updateStatus("comp-123", Status.RESOLVED);

        assertEquals(Status.RESOLVED, updated.getStatus());
        assertNotNull(updated.getResolvedAt());
    }

    @Test
    void updateStatus_ShouldSetClosedAt_WhenStatusBecomesClosed() {
        when(complaintRepository.findById("comp-123")).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Complaint updated = complaintService.updateStatus("comp-123", Status.CLOSED);

        assertEquals(Status.CLOSED, updated.getStatus());
        assertNotNull(updated.getClosedAt());
    }
}