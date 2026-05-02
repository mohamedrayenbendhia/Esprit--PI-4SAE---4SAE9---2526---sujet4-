package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.dto.advanced.CreateTemplateRequest;
import com.smartfreelance.microservice.complaintservice.dto.advanced.ResponseTemplateResponse;
import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.entity.ResponseTemplate;
import com.smartfreelance.microservice.complaintservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.complaintservice.repository.ResponseTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ResponseTemplateServiceImpl implements ResponseTemplateService {

    private final ResponseTemplateRepository templateRepo;

    @Override
    public ResponseTemplateResponse create(CreateTemplateRequest req, String adminId) {
        ResponseTemplate t = ResponseTemplate.builder()
                .title(req.getTitle())
                .content(req.getContent())
                .category(req.getCategory())
                .createdByAdminId(adminId)
                .build();
        return toResponse(templateRepo.save(t));
    }

    @Override
    public ResponseTemplateResponse update(String id, CreateTemplateRequest req) {
        ResponseTemplate t = templateRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
        t.setTitle(req.getTitle());
        t.setContent(req.getContent());
        t.setCategory(req.getCategory());
        return toResponse(templateRepo.save(t));
    }

    @Override
    public void delete(String id) {
        ResponseTemplate t = templateRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
        t.setActive(false);
        templateRepo.save(t);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseTemplateResponse> getAll() {
        return templateRepo.findByActiveTrueOrderByUsageCountDesc()
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseTemplateResponse> getByCategory(Complaint.ComplaintCategory category) {
        List<ResponseTemplate> results = new ArrayList<>(
                templateRepo.findByCategoryAndActiveTrue(category));
        results.addAll(templateRepo.findByCategoryIsNullAndActiveTrue());
        return results.stream().map(this::toResponse).toList();
    }

    @Override
    public ResponseTemplateResponse recordUsage(String id) {
        ResponseTemplate t = templateRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
        t.setUsageCount(t.getUsageCount() + 1);
        return toResponse(templateRepo.save(t));
    }

    private ResponseTemplateResponse toResponse(ResponseTemplate t) {
        return ResponseTemplateResponse.builder()
                .id(t.getId())
                .title(t.getTitle())
                .content(t.getContent())
                .category(t.getCategory())
                .usageCount(t.getUsageCount())
                .active(t.isActive())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
