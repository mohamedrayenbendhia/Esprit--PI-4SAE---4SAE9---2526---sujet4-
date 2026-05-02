package com.smartfreelance.microservice.organizationservice.repository;

import com.smartfreelance.microservice.organizationservice.entity.OrganizationMember;
import com.smartfreelance.microservice.organizationservice.enums.MemberRole;
import com.smartfreelance.microservice.organizationservice.enums.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, String> {

    Optional<OrganizationMember> findByOrganizationIdAndUserId(String organizationId, String userId);

    List<OrganizationMember> findByOrganizationIdAndStatus(String organizationId, MemberStatus status);

    Page<OrganizationMember> findByOrganizationId(String organizationId, Pageable pageable);

    List<OrganizationMember> findByUserId(String userId);

    List<OrganizationMember> findByUserIdAndStatus(String userId, MemberStatus status);

    boolean existsByOrganizationIdAndUserIdAndStatus(String organizationId, String userId, MemberStatus status);

    long countByOrganizationIdAndStatus(String organizationId, MemberStatus status);

    List<OrganizationMember> findByOrganizationIdAndRole(String organizationId, MemberRole role);
}
