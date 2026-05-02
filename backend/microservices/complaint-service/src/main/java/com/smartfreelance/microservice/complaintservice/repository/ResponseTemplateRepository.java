package com.smartfreelance.microservice.complaintservice.repository;

import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.entity.ResponseTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResponseTemplateRepository extends JpaRepository<ResponseTemplate, String> {
    List<ResponseTemplate> findByActiveTrue();
    List<ResponseTemplate> findByCategoryAndActiveTrue(Complaint.ComplaintCategory category);
    List<ResponseTemplate> findByCategoryIsNullAndActiveTrue();
    List<ResponseTemplate> findByActiveTrueOrderByUsageCountDesc();
}
