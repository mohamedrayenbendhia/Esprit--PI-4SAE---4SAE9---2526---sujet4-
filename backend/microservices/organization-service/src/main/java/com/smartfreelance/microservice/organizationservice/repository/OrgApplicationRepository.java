package com.smartfreelance.microservice.organizationservice.repository;

import com.smartfreelance.microservice.organizationservice.entity.OrgApplication;
import com.smartfreelance.microservice.organizationservice.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrgApplicationRepository extends JpaRepository<OrgApplication, String> {

    Page<OrgApplication> findByOrganizationId(String organizationId, Pageable pageable);

    List<OrgApplication> findByApplicantId(String applicantId);

    boolean existsByOrganizationIdAndApplicantIdAndStatus(String organizationId, String applicantId, ApplicationStatus status);

    Page<OrgApplication> findByOrganizationIdAndStatus(String organizationId, ApplicationStatus status, Pageable pageable);

    long countByOrganizationId(String organizationId);

    long countByOrganizationIdAndStatus(String organizationId, ApplicationStatus status);
}
