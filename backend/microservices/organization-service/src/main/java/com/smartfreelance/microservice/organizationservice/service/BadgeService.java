package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.response.BadgeResponse;
import com.smartfreelance.microservice.organizationservice.enums.TrustBadge;

public interface BadgeService {
    BadgeResponse getBadges(String orgId);
    BadgeResponse addBadge(String orgId, TrustBadge badge, String adminId);
    BadgeResponse removeBadge(String orgId, TrustBadge badge, String adminId);
}
