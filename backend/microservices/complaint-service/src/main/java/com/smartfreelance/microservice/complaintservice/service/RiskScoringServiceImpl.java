package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.dto.advanced.UserRiskProfileResponse;
import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.entity.UserRiskProfile;
import com.smartfreelance.microservice.complaintservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.complaintservice.repository.ComplaintRepository;
import com.smartfreelance.microservice.complaintservice.repository.UserRiskProfileRepository;
import com.smartfreelance.microservice.complaintservice.repository.UserSanctionRepository;
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
public class RiskScoringServiceImpl implements RiskScoringService {

    private final UserRiskProfileRepository riskRepo;
    private final ComplaintRepository       complaintRepo;
    private final UserSanctionRepository    sanctionRepo;

    @Override
    public UserRiskProfileResponse computeAndSave(String userId) {
        List<Complaint> against = complaintRepo.findByReportedUserId(userId);

        int total         = against.size();
        int resolved      = (int) against.stream()
                .filter(c -> c.getStatus() == Complaint.Status.CLOSED
                          || c.getStatus() == Complaint.Status.ESCALATED).count();
        int scam          = (int) against.stream()
                .filter(c -> c.getCategory() == Complaint.ComplaintCategory.SCAM).count();
        int harassment    = (int) against.stream()
                .filter(c -> c.getCategory() == Complaint.ComplaintCategory.HARASSMENT).count();
        int payment       = (int) against.stream()
                .filter(c -> c.getCategory() == Complaint.ComplaintCategory.PAYMENT_ISSUE).count();

        // Algorithme de score (max 100)
        int score = 0;
        score += Math.min(total     * 5,  25);
        score += Math.min(resolved  * 8,  32);
        score += Math.min(scam      * 15, 45);
        score += Math.min(harassment* 10, 30);
        score += Math.min(payment   * 5,  15);
        score  = Math.min(score, 100);

        UserRiskProfile.RiskLevel level = computeLevel(score);

        UserRiskProfile profile = riskRepo.findByUserId(userId).orElse(
                UserRiskProfile.builder().userId(userId).build());

        profile.setRiskScore(score);
        profile.setRiskLevel(level);
        profile.setTotalComplaintsAgainst(total);
        profile.setResolvedAgainst(resolved);
        profile.setScamCount(scam);
        profile.setHarassmentCount(harassment);
        profile.setPaymentIssueCount(payment);
        profile.setLastCalculatedAt(LocalDateTime.now());

        UserRiskProfile saved = riskRepo.save(profile);
        log.debug("Risk score for user {}: {} ({})", userId, score, level);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserRiskProfileResponse getProfile(String userId) {
        UserRiskProfile p = riskRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No risk profile for user: " + userId));
        return toResponse(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRiskProfileResponse> getHighRiskUsers() {
        return riskRepo.findHighRiskUsers(50).stream().map(this::toResponse).toList();
    }

    @Override
    public void recalculateAll() {
        List<String> reportedIds = complaintRepo.findAll().stream()
                .map(Complaint::getReportedUserId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        reportedIds.forEach(this::computeAndSave);
        log.info("Risk scores recalculated for {} users.", reportedIds.size());
    }

    private UserRiskProfile.RiskLevel computeLevel(int score) {
        if (score >= 75) return UserRiskProfile.RiskLevel.CRITICAL;
        if (score >= 50) return UserRiskProfile.RiskLevel.HIGH;
        if (score >= 25) return UserRiskProfile.RiskLevel.MODERATE;
        return UserRiskProfile.RiskLevel.LOW;
    }

    private UserRiskProfileResponse toResponse(UserRiskProfile p) {
        long activeSanctions = sanctionRepo.countByUserIdAndActiveTrue(p.getUserId());
        return UserRiskProfileResponse.builder()
                .userId(p.getUserId())
                .riskScore(p.getRiskScore())
                .riskLevel(p.getRiskLevel())
                .totalComplaintsAgainst(p.getTotalComplaintsAgainst())
                .resolvedAgainst(p.getResolvedAgainst())
                .scamCount(p.getScamCount())
                .harassmentCount(p.getHarassmentCount())
                .paymentIssueCount(p.getPaymentIssueCount())
                .lastCalculatedAt(p.getLastCalculatedAt())
                .activeSanctions(activeSanctions)
                .build();
    }
}
