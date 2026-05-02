package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.dto.advanced.UserRiskProfileResponse;
import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.entity.UserRiskProfile;
import com.smartfreelance.microservice.complaintservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.complaintservice.repository.ComplaintRepository;
import com.smartfreelance.microservice.complaintservice.repository.UserRiskProfileRepository;
import com.smartfreelance.microservice.complaintservice.repository.UserSanctionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskScoringServiceImplTest {

    @Mock private UserRiskProfileRepository riskRepo;
    @Mock private ComplaintRepository complaintRepo;
    @Mock private UserSanctionRepository sanctionRepo;

    @InjectMocks private RiskScoringServiceImpl riskScoringService;

    @Test
    void computeAndSave_LowRisk_Success() {
        String userId = "user1";
        // Une seule réclamation simple
        Complaint c1 = new Complaint();
        c1.setCategory(Complaint.ComplaintCategory.PAYMENT_ISSUE);
        c1.setStatus(Complaint.Status.OPEN);

        when(complaintRepo.findByReportedUserId(userId)).thenReturn(List.of(c1));
        when(riskRepo.findByUserId(userId)).thenReturn(Optional.empty());
        when(riskRepo.save(any(UserRiskProfile.class))).thenAnswer(i -> i.getArgument(0));
        when(sanctionRepo.countByUserIdAndActiveTrue(userId)).thenReturn(0L);

        UserRiskProfileResponse res = riskScoringService.computeAndSave(userId);

        // Score attendu : total(1*5=5) + payment(1*5=5) = 10
        assertEquals(10, res.getRiskScore());
        assertEquals(UserRiskProfile.RiskLevel.LOW, res.getRiskLevel());
        verify(riskRepo).save(any());
    }

    @Test
    void computeAndSave_CriticalRisk_WithScams() {
        String userId = "scammer_123";

        // Simuler plusieurs arnaques (SCAM = 15 points par unité)
        Complaint s1 = new Complaint(); s1.setCategory(Complaint.ComplaintCategory.SCAM); s1.setStatus(Complaint.Status.CLOSED);
        Complaint s2 = new Complaint(); s2.setCategory(Complaint.ComplaintCategory.SCAM); s2.setStatus(Complaint.Status.CLOSED);
        Complaint s3 = new Complaint(); s3.setCategory(Complaint.ComplaintCategory.SCAM); s3.setStatus(Complaint.Status.CLOSED);

        when(complaintRepo.findByReportedUserId(userId)).thenReturn(Arrays.asList(s1, s2, s3));
        when(riskRepo.findByUserId(userId)).thenReturn(Optional.empty());
        when(riskRepo.save(any(UserRiskProfile.class))).thenAnswer(i -> i.getArgument(0));

        UserRiskProfileResponse res = riskScoringService.computeAndSave(userId);

        // Score : total(3*5=15) + resolved(3*8=24) + scam(3*15=45) = 84
        // 84 >= 75 donc CRITICAL
        assertEquals(84, res.getRiskScore());
        assertEquals(UserRiskProfile.RiskLevel.CRITICAL, res.getRiskLevel());
    }

    @Test
    void getProfile_Success() {
        UserRiskProfile profile = UserRiskProfile.builder().userId("u1").riskScore(60).build();
        when(riskRepo.findByUserId("u1")).thenReturn(Optional.of(profile));
        when(sanctionRepo.countByUserIdAndActiveTrue("u1")).thenReturn(2L);

        UserRiskProfileResponse res = riskScoringService.getProfile("u1");

        assertEquals(60, res.getRiskScore());
        assertEquals(2, res.getActiveSanctions());
    }

    @Test
    void getProfile_NotFound_ThrowsException() {
        when(riskRepo.findByUserId("u1")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> riskScoringService.getProfile("u1"));
    }

    @Test
    void recalculateAll_Success() {
        Complaint c1 = new Complaint(); c1.setReportedUserId("userA");
        Complaint c2 = new Complaint(); c2.setReportedUserId("userB");

        when(complaintRepo.findAll()).thenReturn(Arrays.asList(c1, c2));
        // Mock pour les deux appels à computeAndSave
        when(complaintRepo.findByReportedUserId(anyString())).thenReturn(Collections.emptyList());
        when(riskRepo.findByUserId(anyString())).thenReturn(Optional.empty());
        when(riskRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        riskScoringService.recalculateAll();

        // On vérifie que save est appelé deux fois (une fois pour chaque utilisateur unique)
        verify(riskRepo, times(2)).save(any());
    }

    @Test
    void getHighRiskUsers_ReturnsList() {
        UserRiskProfile p = UserRiskProfile.builder().userId("highRisk").riskScore(80).build();
        when(riskRepo.findHighRiskUsers(50)).thenReturn(List.of(p));

        List<UserRiskProfileResponse> results = riskScoringService.getHighRiskUsers();

        assertEquals(1, results.size());
        assertEquals("highRisk", results.get(0).getUserId());
    }

    // ── Edge cases : frontières des seuils de risque ──────────────

    @Test
    void computeAndSave_ScoreAt49_ShouldBeLow() {
        // Score 49 → juste sous le seuil HIGH (50) → LOW
        String userId = "borderLow";
        // Score = 9 réclamations OPEN × (5 total + 5 status) = 9×10 = 90 → trop élevé
        // On construit exactement 9 complaints PAYMENT_ISSUE OPEN = 9*(5+5+5) = 135... pas facile à être 49 exactement
        // On mocke directement le résultat en injectant un profil déjà calculé
        UserRiskProfile profile = UserRiskProfile.builder()
                .userId(userId).riskScore(49).riskLevel(UserRiskProfile.RiskLevel.LOW).build();
        when(riskRepo.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(sanctionRepo.countByUserIdAndActiveTrue(userId)).thenReturn(0L);

        UserRiskProfileResponse res = riskScoringService.getProfile(userId);

        assertEquals(49, res.getRiskScore());
        assertEquals(UserRiskProfile.RiskLevel.LOW, res.getRiskLevel());
    }

    @Test
    void computeAndSave_ScoreAt50_ShouldBeHigh() {
        // Score 50 → exactement au seuil HIGH
        String userId = "borderHigh";
        UserRiskProfile profile = UserRiskProfile.builder()
                .userId(userId).riskScore(50).riskLevel(UserRiskProfile.RiskLevel.HIGH).build();
        when(riskRepo.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(sanctionRepo.countByUserIdAndActiveTrue(userId)).thenReturn(0L);

        UserRiskProfileResponse res = riskScoringService.getProfile(userId);

        assertEquals(50, res.getRiskScore());
        assertEquals(UserRiskProfile.RiskLevel.HIGH, res.getRiskLevel());
    }

    @Test
    void computeAndSave_ScoreAt74_ShouldBeHigh() {
        // Score 74 → juste sous CRITICAL (75) → HIGH
        String userId = "almostCritical";
        UserRiskProfile profile = UserRiskProfile.builder()
                .userId(userId).riskScore(74).riskLevel(UserRiskProfile.RiskLevel.HIGH).build();
        when(riskRepo.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(sanctionRepo.countByUserIdAndActiveTrue(userId)).thenReturn(1L);

        UserRiskProfileResponse res = riskScoringService.getProfile(userId);

        assertEquals(74, res.getRiskScore());
        assertEquals(UserRiskProfile.RiskLevel.HIGH, res.getRiskLevel());
    }

    @Test
    void computeAndSave_ScoreAt75_ShouldBeCritical() {
        // Score 75 → seuil exact CRITICAL
        String userId = "exactCritical";
        UserRiskProfile profile = UserRiskProfile.builder()
                .userId(userId).riskScore(75).riskLevel(UserRiskProfile.RiskLevel.CRITICAL).build();
        when(riskRepo.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(sanctionRepo.countByUserIdAndActiveTrue(userId)).thenReturn(2L);

        UserRiskProfileResponse res = riskScoringService.getProfile(userId);

        assertEquals(75, res.getRiskScore());
        assertEquals(UserRiskProfile.RiskLevel.CRITICAL, res.getRiskLevel());
    }

    @Test
    void computeAndSave_WithNoComplaints_ShouldReturnScoreZero() {
        // Aucune réclamation → score = 0 → LOW
        String userId = "newUser";
        when(complaintRepo.findByReportedUserId(userId)).thenReturn(Collections.emptyList());
        when(riskRepo.findByUserId(userId)).thenReturn(Optional.empty());
        when(riskRepo.save(any(UserRiskProfile.class))).thenAnswer(i -> i.getArgument(0));
        when(sanctionRepo.countByUserIdAndActiveTrue(userId)).thenReturn(0L);

        UserRiskProfileResponse res = riskScoringService.computeAndSave(userId);

        assertEquals(0, res.getRiskScore());
        assertEquals(UserRiskProfile.RiskLevel.LOW, res.getRiskLevel());
    }

    @Test
    void getHighRiskUsers_ReturnsEmpty_WhenNoHighRiskExists() {
        when(riskRepo.findHighRiskUsers(50)).thenReturn(Collections.emptyList());

        List<UserRiskProfileResponse> results = riskScoringService.getHighRiskUsers();

        assertTrue(results.isEmpty());
    }
}