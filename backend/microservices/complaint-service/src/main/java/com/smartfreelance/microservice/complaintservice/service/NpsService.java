package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.dto.advanced.NpsResponseRequest;
import com.smartfreelance.microservice.complaintservice.dto.advanced.NpsStatsResponse;
import com.smartfreelance.microservice.complaintservice.entity.NpsSurvey;

import java.util.List;

public interface NpsService {
    NpsSurvey createSurvey(String complaintId, String respondentId);
    NpsSurvey respond(String complaintId, NpsResponseRequest req, String userId);
    NpsStatsResponse getStats();
    List<NpsSurvey> getPendingSurveys();
}
