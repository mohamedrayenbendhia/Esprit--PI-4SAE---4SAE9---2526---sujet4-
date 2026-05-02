package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.dto.advanced.CreateSlaRuleRequest;
import com.smartfreelance.microservice.complaintservice.dto.advanced.SlaRuleResponse;
import com.smartfreelance.microservice.complaintservice.dto.advanced.SlaTrackingResponse;
import com.smartfreelance.microservice.complaintservice.entity.Complaint;

import java.util.List;

public interface SlaService {
    SlaRuleResponse createRule(CreateSlaRuleRequest req);
    SlaRuleResponse updateRule(String ruleId, CreateSlaRuleRequest req);
    List<SlaRuleResponse> getAllRules();
    void deleteRule(String ruleId);
    void initTracking(String complaintId, Complaint.Priority priority);
    void recordFirstResponse(String complaintId);
    void recordResolution(String complaintId);
    SlaTrackingResponse getTracking(String complaintId);
    void processBreaches();
}
