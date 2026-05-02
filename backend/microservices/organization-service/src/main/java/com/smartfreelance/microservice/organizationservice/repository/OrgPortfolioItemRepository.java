package com.smartfreelance.microservice.organizationservice.repository;

import com.smartfreelance.microservice.organizationservice.entity.OrgPortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrgPortfolioItemRepository extends JpaRepository<OrgPortfolioItem, String> {

    List<OrgPortfolioItem> findByOrganizationIdOrderByCreatedAtDesc(String organizationId);

    long countByOrganizationId(String organizationId);
}
