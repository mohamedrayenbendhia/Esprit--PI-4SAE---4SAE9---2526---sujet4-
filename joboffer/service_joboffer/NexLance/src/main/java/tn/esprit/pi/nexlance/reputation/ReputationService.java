package tn.esprit.pi.nexlance.reputation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.pi.nexlance.invitation.entities.Invitation;
import tn.esprit.pi.nexlance.invitation.repositories.InvitationRepository;
import tn.esprit.pi.nexlance.module_job_offers.entities.Application;
import tn.esprit.pi.nexlance.module_job_offers.repositories.ApplicationRepository;
import tn.esprit.pi.nexlance.recommendation.entities.Recommendation;
import tn.esprit.pi.nexlance.recommendation.repositories.RecommendationRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReputationService {

    private final ApplicationRepository applicationRepository;
    private final RecommendationRepository recommendationRepository;
    private final InvitationRepository invitationRepository;

    /**
     * Calculate reputation score for a freelancer
     */
    public ReputationScore calculateReputation(String freelancerId) {
        // Get all applications by this freelancer
        List<Application> applications = applicationRepository.findAll().stream()
                .filter(app -> freelancerId.equals(app.getFreelanceId().toString()))
                .toList();

        int totalApplications = applications.size();
        int acceptedApplications = (int) applications.stream()
                .filter(app -> "ACCEPTED".equals(app.getStatus().name()))
                .count();

        // Get recommendations for this freelancer
        List<Recommendation> recommendations = recommendationRepository.findByFreelanceId(freelancerId);
        int totalRecommendations = recommendations.size();
        int acceptedRecommendations = (int) recommendations.stream()
                .filter(rec -> "ACCEPTED".equals(rec.getStatus().name()))
                .count();

        // Get invitations for this freelancer
        List<Invitation> invitations = invitationRepository.findAll().stream()
                .filter(inv -> freelancerId.equals(inv.getFreelanceId()))
                .toList();

        int totalInvitations = invitations.size();
        int respondedInvitations = (int) invitations.stream()
                .filter(inv -> !"PENDING".equals(inv.getStatus().name()))
                .count();

        double responseRate = totalInvitations > 0
                ? (double) respondedInvitations / totalInvitations * 100 : 100;

        // Calculate overall score (weighted formula)
        double applicationScore = totalApplications > 0
                ? (double) acceptedApplications / totalApplications * 30 : 15;
        double recommendationScore = totalRecommendations > 0
                ? (double) acceptedRecommendations / totalRecommendations * 25 : 12.5;
        double responseScore = responseRate * 0.2;
        double volumeScore = Math.min(acceptedApplications * 2.5, 25); // Max 25 points for volume

        double overallScore = Math.min(applicationScore + recommendationScore + responseScore + volumeScore, 100);
        overallScore = Math.round(overallScore * 10.0) / 10.0;

        // Determine tier
        String tier = determineTier(overallScore);

        // Calculate badges
        List<String> badges = calculateBadges(totalApplications, acceptedApplications,
                totalRecommendations, acceptedRecommendations, responseRate);

        return ReputationScore.builder()
                .freelancerId(freelancerId)
                .overallScore(overallScore)
                .completedProjects(acceptedApplications)
                .totalRecommendations(totalRecommendations)
                .acceptedRecommendations(acceptedRecommendations)
                .responseRate(Math.round(responseRate * 10.0) / 10.0)
                .onTimeDeliveryRate(0) // Would need milestone data from projects service
                .totalApplications(totalApplications)
                .acceptedApplications(acceptedApplications)
                .tier(tier)
                .badges(badges)
                .build();
    }

    private String determineTier(double score) {
        if (score >= 80) return "PLATINUM";
        if (score >= 60) return "GOLD";
        if (score >= 40) return "SILVER";
        return "BRONZE";
    }

    private List<String> calculateBadges(int totalApps, int acceptedApps,
                                          int totalRecs, int acceptedRecs, double responseRate) {
        List<String> badges = new ArrayList<>();

        if (acceptedApps >= 10) badges.add("TOP_PERFORMER");
        if (acceptedApps >= 5) badges.add("EXPERIENCED");
        if (acceptedApps >= 1) badges.add("VERIFIED_FREELANCER");
        if (totalRecs >= 5) badges.add("HIGHLY_RECOMMENDED");
        if (acceptedRecs >= 3) badges.add("TRUSTED_EXPERT");
        if (responseRate >= 90) badges.add("FAST_RESPONDER");
        if (totalApps >= 20) badges.add("ACTIVE_CONTRIBUTOR");

        return badges;
    }
}
