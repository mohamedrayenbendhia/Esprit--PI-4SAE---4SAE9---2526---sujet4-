package tn.esprit.pi.nexlance.reputation;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReputationScore {
    private String freelancerId;
    private double overallScore; // 0-100
    private int completedProjects;
    private int totalRecommendations;
    private int acceptedRecommendations;
    private double responseRate; // % of invitations responded to
    private double onTimeDeliveryRate; // % of milestones delivered on time
    private int totalApplications;
    private int acceptedApplications;
    private String tier; // BRONZE, SILVER, GOLD, PLATINUM
    private List<String> badges;
}
