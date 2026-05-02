package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.entity.Complaint;

public interface ReopenService {
    Complaint reopen(String complaintId, String userId, String reason);
}
