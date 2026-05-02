package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.dto.advanced.UserSanctionResponse;
import com.smartfreelance.microservice.complaintservice.entity.UserSanction;

import java.util.List;

public interface SanctionService {
    UserSanctionResponse applyAutomatic(String userId, String triggerComplaintId);
    UserSanctionResponse applyManual(String userId, String reason, UserSanction.SanctionType type, String adminId);
    UserSanctionResponse liftSanction(String sanctionId, String adminId);
    List<UserSanctionResponse> getForUser(String userId);
    int expireOldSanctions();
}
