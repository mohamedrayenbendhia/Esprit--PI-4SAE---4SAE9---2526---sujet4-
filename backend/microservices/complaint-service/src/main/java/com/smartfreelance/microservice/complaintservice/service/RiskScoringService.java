package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.dto.advanced.UserRiskProfileResponse;

import java.util.List;

public interface RiskScoringService {
    UserRiskProfileResponse computeAndSave(String userId);
    UserRiskProfileResponse getProfile(String userId);
    List<UserRiskProfileResponse> getHighRiskUsers();
    void recalculateAll();
}
