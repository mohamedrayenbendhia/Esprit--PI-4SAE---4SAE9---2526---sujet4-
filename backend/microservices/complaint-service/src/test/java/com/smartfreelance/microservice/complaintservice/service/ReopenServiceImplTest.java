package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.complaintservice.repository.ComplaintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReopenServiceImplTest {

    @Mock private ComplaintRepository complaintRepo;
    @Mock private com.smartfreelance.microservice.complaintservice.notification.ComplaintNotificationService notificationService;

    @InjectMocks private ReopenServiceImpl reopenService;

    private Complaint sampleComplaint;

    @BeforeEach
    void setUp() {
        sampleComplaint = new Complaint();
        sampleComplaint.setId("comp-1");
        sampleComplaint.setReporterId("user-123");
        sampleComplaint.setStatus(Complaint.Status.RESOLVED);
        sampleComplaint.setReopenCount(0);
        sampleComplaint.setResolvedAt(LocalDateTime.now().minusDays(2)); // Dans la fenêtre de 7 jours
    }

    @Test
    void reopen_Success() {
        when(complaintRepo.findById("comp-1")).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepo.save(any(Complaint.class))).thenAnswer(i -> i.getArgument(0));

        Complaint result = reopenService.reopen("comp-1", "user-123", "Problème non résolu");

        assertEquals(Complaint.Status.OPEN, result.getStatus());
        assertEquals(1, result.getReopenCount());
        assertNull(result.getAssignedToId());
        assertNotNull(result.getLastReopenedAt());
        verify(complaintRepo).save(any());
    }

    @Test
    void reopen_ThrowsException_WhenUserIsNotReporter() {
        when(complaintRepo.findById("comp-1")).thenReturn(Optional.of(sampleComplaint));

        assertThrows(IllegalArgumentException.class,
                () -> reopenService.reopen("comp-1", "wrong-user", "Reason"));
    }

    @Test
    void reopen_ThrowsException_WhenStatusInvalid() {
        sampleComplaint.setStatus(Complaint.Status.OPEN); // Déjà ouvert
        when(complaintRepo.findById("comp-1")).thenReturn(Optional.of(sampleComplaint));

        assertThrows(IllegalStateException.class,
                () -> reopenService.reopen("comp-1", "user-123", "Reason"));
    }

    @Test
    void reopen_ThrowsException_WhenMaxReopensReached() {
        sampleComplaint.setReopenCount(2); // MAX_REOPENS = 2
        when(complaintRepo.findById("comp-1")).thenReturn(Optional.of(sampleComplaint));

        assertThrows(IllegalStateException.class,
                () -> reopenService.reopen("comp-1", "user-123", "Reason"));
    }

    @Test
    void reopen_ThrowsException_WhenWindowExpired() {
        // Date de résolution il y a 10 jours (fenêtre de 7 jours dépassée)
        sampleComplaint.setResolvedAt(LocalDateTime.now().minusDays(10));
        when(complaintRepo.findById("comp-1")).thenReturn(Optional.of(sampleComplaint));

        assertThrows(IllegalStateException.class,
                () -> reopenService.reopen("comp-1", "user-123", "Reason"));
    }

    @Test
    void reopen_NotFound_ThrowsException() {
        when(complaintRepo.findById("unknown")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> reopenService.reopen("unknown", "user-1", "Reason"));
    }
}