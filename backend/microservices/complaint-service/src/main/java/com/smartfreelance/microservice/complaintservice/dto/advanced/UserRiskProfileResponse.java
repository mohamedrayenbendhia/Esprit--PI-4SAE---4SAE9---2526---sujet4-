package com.smartfreelance.microservice.complaintservice.dto.advanced;

import com.smartfreelance.microservice.complaintservice.entity.UserRiskProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserRiskProfileResponse {
    private String userId;
    private int riskScore;
    private UserRiskProfile.RiskLevel riskLevel;
    private int totalComplaintsAgainst;
    private int resolvedAgainst;
    private int scamCount;
    private int harassmentCount;
    private int paymentIssueCount;
    private LocalDateTime lastCalculatedAt;
    private long activeSanctions;
}
