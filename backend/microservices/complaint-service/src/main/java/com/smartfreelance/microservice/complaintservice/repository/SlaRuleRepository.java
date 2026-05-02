package com.smartfreelance.microservice.complaintservice.repository;

import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.entity.SlaRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SlaRuleRepository extends JpaRepository<SlaRule, String> {
    Optional<SlaRule> findByPriority(Complaint.Priority priority);
    boolean existsByPriority(Complaint.Priority priority);
}
