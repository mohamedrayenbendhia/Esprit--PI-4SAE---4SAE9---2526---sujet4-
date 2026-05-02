package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.dto.advanced.NpsResponseRequest;
import com.smartfreelance.microservice.complaintservice.dto.advanced.NpsStatsResponse;
import com.smartfreelance.microservice.complaintservice.entity.NpsSurvey;
import com.smartfreelance.microservice.complaintservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.complaintservice.notification.ComplaintNotificationService;
import com.smartfreelance.microservice.complaintservice.repository.ComplaintRepository;
import com.smartfreelance.microservice.complaintservice.repository.NpsSurveyRepository;
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
public class NpsServiceImplTest {

    @Mock private NpsSurveyRepository          npsRepo;
    @Mock private ComplaintRepository          complaintRepo;
    @Mock private ComplaintNotificationService notificationService;

    @InjectMocks private NpsServiceImpl npsService;

    private NpsSurvey sampleSurvey;

    @BeforeEach
    void setUp() {
        sampleSurvey = NpsSurvey.builder()
                .complaintId("comp-1")
                .respondentId("user-123")
                .build();
    }

    @Test
    void createSurvey_Success_New() {
        when(npsRepo.existsByComplaintId("comp-1")).thenReturn(false);
        when(npsRepo.save(any(NpsSurvey.class))).thenAnswer(i -> i.getArgument(0));

        NpsSurvey result = npsService.createSurvey("comp-1", "user-123");

        assertNotNull(result);
        assertEquals("comp-1", result.getComplaintId());
        verify(npsRepo).save(any());
    }

    @Test
    void respond_Success() {
        NpsResponseRequest req = new NpsResponseRequest();
        req.setScore(9); // Promoteur
        req.setComment("Excellent service");

        when(npsRepo.findByComplaintId("comp-1")).thenReturn(Optional.of(sampleSurvey));
        when(npsRepo.save(any(NpsSurvey.class))).thenAnswer(i -> i.getArgument(0));

        NpsSurvey result = npsService.respond("comp-1", req, "user-123");

        assertEquals(9, result.getScore());
        assertNotNull(result.getRespondedAt());
        verify(npsRepo).save(any());
    }

    @Test
    void respond_ThrowsException_WhenAlreadyResponded() {
        sampleSurvey.setRespondedAt(java.time.LocalDateTime.now());
        when(npsRepo.findByComplaintId("comp-1")).thenReturn(Optional.of(sampleSurvey));

        NpsResponseRequest req = new NpsResponseRequest();
        req.setScore(5);

        assertThrows(IllegalStateException.class,
                () -> npsService.respond("comp-1", req, "user-123"));
    }

    @Test
    void respond_ThrowsException_WhenWrongUser() {
        when(npsRepo.findByComplaintId("comp-1")).thenReturn(Optional.of(sampleSurvey));
        NpsResponseRequest req = new NpsResponseRequest();

        assertThrows(IllegalArgumentException.class,
                () -> npsService.respond("comp-1", req, "intruder-id"));
    }

    @Test
    void getStats_CalculatesCorrectly() {
        // Mock des counts pour le calcul du NPS
        when(npsRepo.count()).thenReturn(10L);          // Total envoyés
        when(npsRepo.countResponded()).thenReturn(5L);  // Total répondus
        when(npsRepo.countPromoters()).thenReturn(3L);  // 3 Promoteurs
        when(npsRepo.countDetractors()).thenReturn(1L); // 1 Détracteur
        when(npsRepo.computeAverageScore()).thenReturn(8.0);

        NpsStatsResponse stats = npsService.getStats();

        // NPS = ((Promoteurs - Détracteurs) / TotalRéponses) * 100
        // NPS = ((3 - 1) / 5) * 100 = 40.0
        assertEquals(40.0, stats.getNpsScore());
        assertEquals(50.0, stats.getResponseRate()); // (5/10) * 100
        assertEquals(1L, stats.getPassives());       // 5 total - 3 prom - 1 det = 1 passive
        assertEquals(8.0, stats.getAverageScore());
    }

    @Test
    void respond_NotFound_ThrowsException() {
        when(npsRepo.findByComplaintId("any")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> npsService.respond("any", new NpsResponseRequest(), "u1"));
    }

    // ── Edge cases ─────────────────────────────────────────────────

    @Test
    void createSurvey_ShouldReturnExisting_WhenAlreadyExists() {
        // Si une enquête existe déjà, createSurvey doit retourner l'existante sans en créer une nouvelle
        when(npsRepo.existsByComplaintId("comp-1")).thenReturn(true);
        when(npsRepo.findByComplaintId("comp-1")).thenReturn(Optional.of(sampleSurvey));

        NpsSurvey result = npsService.createSurvey("comp-1", "user-123");

        assertEquals("comp-1", result.getComplaintId());
        verify(npsRepo, never()).save(any()); // aucune nouvelle sauvegarde
    }

    @Test
    void respond_WithNullComment_ShouldSucceed() {
        // Un commentaire null est autorisé (optionnel)
        NpsResponseRequest req = new NpsResponseRequest();
        req.setScore(7);
        req.setComment(null);

        when(npsRepo.findByComplaintId("comp-1")).thenReturn(Optional.of(sampleSurvey));
        when(npsRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        NpsSurvey result = npsService.respond("comp-1", req, "user-123");

        assertEquals(7, result.getScore());
        assertNull(result.getComment());
        assertNotNull(result.getRespondedAt());
    }

    @Test
    void getStats_WithZeroRespondents_NpsScoreShouldBeZero() {
        // Aucune réponse → NPS = 0 (pas de division par zéro)
        when(npsRepo.count()).thenReturn(5L);
        when(npsRepo.countResponded()).thenReturn(0L);
        when(npsRepo.countPromoters()).thenReturn(0L);
        when(npsRepo.countDetractors()).thenReturn(0L);
        when(npsRepo.computeAverageScore()).thenReturn(null);

        NpsStatsResponse stats = npsService.getStats();

        assertEquals(0.0, stats.getNpsScore());
        assertEquals(0.0, stats.getAverageScore());
        assertEquals(0.0, stats.getResponseRate()); // 0 réponses / 5 envoyés ≠ 0 → 0%
        // Note: responseRate = 0/5*100 = 0
    }

    @Test
    void getStats_WithZeroSurveysSent_ResponseRateShouldBeZero() {
        // Aucun sondage envoyé → taux de réponse = 0 (pas de division par zéro)
        when(npsRepo.count()).thenReturn(0L);
        when(npsRepo.countResponded()).thenReturn(0L);
        when(npsRepo.countPromoters()).thenReturn(0L);
        when(npsRepo.countDetractors()).thenReturn(0L);
        when(npsRepo.computeAverageScore()).thenReturn(null);

        NpsStatsResponse stats = npsService.getStats();

        assertEquals(0.0, stats.getResponseRate());
        assertEquals(0.0, stats.getNpsScore());
    }

    @Test
    void getStats_AllPromoters_NpsScoreShouldBe100() {
        // Tous des promoteurs (9-10), 0 détracteurs → NPS = 100
        when(npsRepo.count()).thenReturn(10L);
        when(npsRepo.countResponded()).thenReturn(10L);
        when(npsRepo.countPromoters()).thenReturn(10L);
        when(npsRepo.countDetractors()).thenReturn(0L);
        when(npsRepo.computeAverageScore()).thenReturn(9.5);

        NpsStatsResponse stats = npsService.getStats();

        assertEquals(100.0, stats.getNpsScore());
        assertEquals(0L, stats.getPassives());
    }

    @Test
    void getStats_AllDetractors_NpsScoreShouldBeMinus100() {
        // Tous des détracteurs (0-6) → NPS = -100
        when(npsRepo.count()).thenReturn(10L);
        when(npsRepo.countResponded()).thenReturn(10L);
        when(npsRepo.countPromoters()).thenReturn(0L);
        when(npsRepo.countDetractors()).thenReturn(10L);
        when(npsRepo.computeAverageScore()).thenReturn(2.0);

        NpsStatsResponse stats = npsService.getStats();

        assertEquals(-100.0, stats.getNpsScore());
        // passives = max(10 - 0 - 10, 0) = 0
        assertEquals(0L, stats.getPassives());
    }
}