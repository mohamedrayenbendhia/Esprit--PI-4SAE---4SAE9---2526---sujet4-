package com.smartfreelance.microservice.organizationservice.repository;

import com.smartfreelance.microservice.organizationservice.entity.OrgRfq;
import com.smartfreelance.microservice.organizationservice.enums.RfqStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrgRfqRepository extends JpaRepository<OrgRfq, String> {

    Page<OrgRfq> findByOrganizationId(String organizationId, Pageable pageable);

    List<OrgRfq> findByRequesterId(String requesterId);

    Page<OrgRfq> findByOrganizationIdAndStatus(String organizationId, RfqStatus status, Pageable pageable);

    long countByOrganizationId(String organizationId);

    long countByOrganizationIdAndStatus(String organizationId, RfqStatus status);
}
