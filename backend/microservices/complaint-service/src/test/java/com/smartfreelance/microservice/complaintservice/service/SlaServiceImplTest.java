package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.dto.advanced.CreateSlaRuleRequest;
import com.smartfreelance.microservice.complaintservice.dto.advanced.SlaRuleResponse;
import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.entity.SlaRule;
import com.smartfreelance.microservice.complaintservice.entity.SlaTracking;
import com.smartfreelance.microservice.complaintservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.complaintservice.notification.ComplaintNotificationService;
import com.smartfreelance.microservice.complaintservice.repository.ComplaintRepository;
import com.smartfreelance.microservice.complaintservice.repository.SlaRuleRepository;
import com.smartfreelance.microservice.complaintservice.repository.SlaTrackingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlaServiceImplTest {

    @Mock private SlaRuleRepository            slaRuleRepo;
    @Mock private SlaTrackingRepository        slaTrackingRepo;
    @Mock private ComplaintRepository          complaintRepo;
    @Mock private ComplaintNotificationService notificationService;

    @InjectMocks private SlaServiceImpl slaService;

    // --- Tests des Règles SLA ---

    @Test
    void createRule_Success() {
        CreateSlaRuleRequest req = CreateSlaRuleRequest.builder()
                .priority(Complaint.Priority.HIGH)
                .maxFirstResponseHours(2)
                .maxResolutionHours(24)
                .warningThresholdHours(1)
                .build();

        when(slaRuleRepo.existsByPriority(any())).thenReturn(false);
        when(slaRuleRepo.save(any(SlaRule.class))).thenAnswer(i -> i.getArgument(0));

        SlaRuleResponse res = slaService.createRule(req);

        assertNotNull(res);
        assertEquals(Complaint.Priority.HIGH, res.getPriority());
        verify(slaRuleRepo).save(any());
    }

    @Test
    void createRule_ThrowsException_IfPriorityExists() {
        CreateSlaRuleRequest req = CreateSlaRuleRequest.builder()
                .priority(Complaint.Priority.HIGH)
                .build();

        when(slaRuleRepo.existsByPriority(Complaint.Priority.HIGH)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> slaService.createRule(req));
    }

    // --- Tests du Tracking ---

    @Test
    void initTracking_Success() {
        SlaRule rule = SlaRule.builder()
                .maxFirstResponseHours(2)
                .maxResolutionHours(24)
                .build();

        when(slaTrackingRepo.findByComplaintId("C1")).thenReturn(Optional.empty());
        when(slaRuleRepo.findByPriority(Complaint.Priority.HIGH)).thenReturn(Optional.of(rule));

        slaService.initTracking("C1", Complaint.Priority.HIGH);

        verify(slaTrackingRepo).save(any(SlaTracking.class));
    }

    @Test
    void recordFirstResponse_Success() {
        SlaTracking tracking = new SlaTracking();
        when(slaTrackingRepo.findByComplaintId("C1")).thenReturn(Optional.of(tracking));

        slaService.recordFirstResponse("C1");

        assertNotNull(tracking.getFirstResponseAt());
        verify(slaTrackingRepo).save(tracking);
    }

    // --- Tests du Scheduler (Breaches) ---

    @Test
    void processBreaches_ShouldEscalateToCritical_WhenFirstResponseBreached() {
        SlaTracking tracking = SlaTracking.builder().complaintId("C1").build();
        Complaint complaint = new Complaint();
        complaint.setId("C1");
        complaint.setPriority(Complaint.Priority.LOW);

        when(slaTrackingRepo.findFirstResponseBreaches(any())).thenReturn(List.of(tracking));
        when(complaintRepo.findById("C1")).thenReturn(Optional.of(complaint));

        slaService.processBreaches();

        assertTrue(tracking.isFirstResponseBreached());
        assertEquals(Complaint.Priority.CRITICAL, complaint.getPriority());
        verify(complaintRepo).save(complaint);
    }

    @Test
    void processBreaches_ShouldEscalateStatus_WhenResolutionBreached() {
        SlaTracking tracking = SlaTracking.builder().complaintId("C1").build();
        Complaint complaint = new Complaint();
        complaint.setId("C1");
        complaint.setStatus(Complaint.Status.OPEN);

        when(slaTrackingRepo.findResolutionBreaches(any())).thenReturn(List.of(tracking));
        when(complaintRepo.findById("C1")).thenReturn(Optional.of(complaint));

        slaService.processBreaches();

        assertTrue(tracking.isResolutionBreached());
        assertEquals(Complaint.Status.ESCALATED, complaint.getStatus());
        verify(complaintRepo).save(complaint);
    }

    // --- Tests Utilitaires ---

    @Test
    void getTracking_NotFound_ThrowsException() {
        when(slaTrackingRepo.findByComplaintId("C1")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> slaService.getTracking("C1"));
    }

    @Test
    void deleteRule_Success() {
        slaService.deleteRule("rule1");
        verify(slaRuleRepo).deleteById("rule1");
    }

    // ── Edge cases ─────────────────────────────────────────────────

    @Test
    void initTracking_ShouldBeIdempotent_WhenAlreadyInitialized() {
        // Si un tracking existe déjà, initTracking ne doit pas en créer un nouveau
        SlaTracking existing = new SlaTracking();
        when(slaTrackingRepo.findByComplaintId("C1")).thenReturn(Optional.of(existing));

        slaService.initTracking("C1", Complaint.Priority.HIGH);

        verify(slaTrackingRepo, never()).save(any()); // aucune sauvegarde
        verify(slaRuleRepo, never()).findByPriority(any()); // règle non consultée
    }

    @Test
    void initTracking_ShouldSkip_WhenNoSlaRuleFoundForPriority() {
        // Pas de règle SLA pour la priorité donnée → rien n'est sauvegardé
        when(slaTrackingRepo.findByComplaintId("C1")).thenReturn(Optional.empty());
        when(slaRuleRepo.findByPriority(Complaint.Priority.LOW)).thenReturn(Optional.empty());

        slaService.initTracking("C1", Complaint.Priority.LOW);

        verify(slaTrackingRepo, never()).save(any());
    }

    @Test
    void recordFirstResponse_ShouldBeIdempotent_WhenAlreadyRecorded() {
        // Si firstResponseAt est déjà renseigné, il ne doit pas être écrasé
        SlaTracking tracking = new SlaTracking();
        java.time.LocalDateTime originalTime = java.time.LocalDateTime.now().minusHours(1);
        tracking.setFirstResponseAt(originalTime);

        when(slaTrackingRepo.findByComplaintId("C1")).thenReturn(Optional.of(tracking));

        slaService.recordFirstResponse("C1");

        assertEquals(originalTime, tracking.getFirstResponseAt()); // inchangé
        verify(slaTrackingRepo, never()).save(any()); // pas de sauvegarde
    }

    @Test
    void processBreaches_ShouldNotEscalatePriority_WhenAlreadyCritical() {
        // Si la priorité est déjà CRITICAL, ne pas déclencher de nouvelle sauvegarde
        SlaTracking tracking = SlaTracking.builder().complaintId("C1").build();
        Complaint complaint = new Complaint();
        complaint.setId("C1");
        complaint.setPriority(Complaint.Priority.CRITICAL); // déjà au maximum

        when(slaTrackingRepo.findFirstResponseBreaches(any())).thenReturn(List.of(tracking));
        when(complaintRepo.findById("C1")).thenReturn(Optional.of(complaint));

        slaService.processBreaches();

        assertTrue(tracking.isFirstResponseBreached()); // le flag est quand même mis
        verify(complaintRepo, never()).save(complaint);  // mais pas de sauvegarde de la plainte
    }

    @Test
    void processBreaches_ShouldNotEscalateStatus_WhenAlreadyResolved() {
        // Breach de résolution sur une plainte déjà RESOLVED → ne pas changer le statut
        SlaTracking tracking = SlaTracking.builder().complaintId("C1").build();
        Complaint complaint = new Complaint();
        complaint.setId("C1");
        complaint.setStatus(Complaint.Status.RESOLVED);

        when(slaTrackingRepo.findResolutionBreaches(any())).thenReturn(List.of(tracking));
        when(complaintRepo.findById("C1")).thenReturn(Optional.of(complaint));

        slaService.processBreaches();

        assertTrue(tracking.isResolutionBreached()); // flag positionné
        assertEquals(Complaint.Status.RESOLVED, complaint.getStatus()); // statut inchangé
        verify(complaintRepo, never()).save(complaint);
    }

    @Test
    void processBreaches_ShouldNotEscalateStatus_WhenAlreadyClosed() {
        // Breach sur plainte CLOSED → statut protégé
        SlaTracking tracking = SlaTracking.builder().complaintId("C1").build();
        Complaint complaint = new Complaint();
        complaint.setId("C1");
        complaint.setStatus(Complaint.Status.CLOSED);

        when(slaTrackingRepo.findResolutionBreaches(any())).thenReturn(List.of(tracking));
        when(complaintRepo.findById("C1")).thenReturn(Optional.of(complaint));

        slaService.processBreaches();

        assertEquals(Complaint.Status.CLOSED, complaint.getStatus());
        verify(complaintRepo, never()).save(complaint);
    }

    @Test
    void processBreaches_ShouldDoNothing_WhenNoBreachesDetected() {
        // Aucun breach → aucune interaction avec complaintRepo
        when(slaTrackingRepo.findFirstResponseBreaches(any())).thenReturn(List.of());
        when(slaTrackingRepo.findResolutionBreaches(any())).thenReturn(List.of());

        slaService.processBreaches();

        verifyNoInteractions(complaintRepo);
    }

    @Test
    void processBreaches_ShouldNotEscalateStatus_WhenAlreadyEscalated() {
        // Breach sur plainte déjà ESCALATED → pas de double escalade
        SlaTracking tracking = SlaTracking.builder().complaintId("C1").build();
        Complaint complaint = new Complaint();
        complaint.setId("C1");
        complaint.setStatus(Complaint.Status.ESCALATED);

        when(slaTrackingRepo.findResolutionBreaches(any())).thenReturn(List.of(tracking));
        when(complaintRepo.findById("C1")).thenReturn(Optional.of(complaint));

        slaService.processBreaches();

        assertEquals(Complaint.Status.ESCALATED, complaint.getStatus());
        verify(complaintRepo, never()).save(complaint);
    }
}