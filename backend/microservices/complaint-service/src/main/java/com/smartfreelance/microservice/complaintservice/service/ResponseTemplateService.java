package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.dto.advanced.CreateTemplateRequest;
import com.smartfreelance.microservice.complaintservice.dto.advanced.ResponseTemplateResponse;
import com.smartfreelance.microservice.complaintservice.entity.Complaint;

import java.util.List;

public interface ResponseTemplateService {
    ResponseTemplateResponse create(CreateTemplateRequest req, String adminId);
    ResponseTemplateResponse update(String id, CreateTemplateRequest req);
    void delete(String id);
    List<ResponseTemplateResponse> getAll();
    List<ResponseTemplateResponse> getByCategory(Complaint.ComplaintCategory category);
    ResponseTemplateResponse recordUsage(String id);
}
