package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.response.MemberResponse;
import com.smartfreelance.microservice.organizationservice.entity.AuditLog;
import com.smartfreelance.microservice.organizationservice.entity.OrganizationMember;
import com.smartfreelance.microservice.organizationservice.enums.MemberRole;
import com.smartfreelance.microservice.organizationservice.enums.MemberStatus;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.AuditLogRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberServiceImpl implements MemberService {

    private final OrganizationMemberRepository memberRepo;
    private final AuditLogRepository auditRepo;

    @Override
    @Transactional(readOnly = true)
    public Page<MemberResponse> getMembers(String orgId, Pageable pageable) {
        return memberRepo.findByOrganizationId(orgId, pageable).map(this::toResponse);
    }

    @Override
    public MemberResponse updateRole(String orgId, String memberId, MemberRole newRole, String requesterId) {
        OrganizationMember requester = memberRepo.findByOrganizationIdAndUserId(orgId, requesterId)
                .orElseThrow(() -> new BusinessRuleException("Requester is not a member of this organization."));
        if (requester.getRole() != MemberRole.OWNER && requester.getRole() != MemberRole.MANAGER) {
            throw new BusinessRuleException("Only owners and managers can change member roles.");
        }
        if (newRole == MemberRole.OWNER) {
            throw new BusinessRuleException("Use the transfer ownership endpoint to assign OWNER role.");
        }
        OrganizationMember member = memberRepo.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + memberId));
        member.setRole(newRole);
        audit(orgId, requesterId, "MEMBER_ROLE_UPDATED", "Member " + memberId + " role changed to " + newRole);
        return toResponse(memberRepo.save(member));
    }

    @Override
    public void removeMember(String orgId, String memberId, String requesterId) {
        OrganizationMember requester = memberRepo.findByOrganizationIdAndUserId(orgId, requesterId)
                .orElseThrow(() -> new BusinessRuleException("Requester is not a member of this organization."));
        if (requester.getRole() != MemberRole.OWNER && requester.getRole() != MemberRole.MANAGER) {
            throw new BusinessRuleException("Only owners and managers can remove members.");
        }
        OrganizationMember member = memberRepo.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + memberId));
        if (member.getRole() == MemberRole.OWNER) {
            throw new BusinessRuleException("Cannot remove the organization owner.");
        }
        member.setStatus(MemberStatus.INACTIVE);
        member.setLeftAt(LocalDateTime.now());
        memberRepo.save(member);
        audit(orgId, requesterId, "MEMBER_REMOVED", "Removed member: " + memberId);
    }

    @Override
    public void leave(String orgId, String userId) {
        OrganizationMember member = memberRepo.findByOrganizationIdAndUserId(orgId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("You are not a member of this organization."));
        if (member.getRole() == MemberRole.OWNER) {
            throw new BusinessRuleException("Owner cannot leave. Transfer ownership first.");
        }
        member.setStatus(MemberStatus.INACTIVE);
        member.setLeftAt(LocalDateTime.now());
        memberRepo.save(member);
        audit(orgId, userId, "MEMBER_LEFT", "Member voluntarily left the organization");
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> getMyMemberships(String userId) {
        // Uniquement les memberships actifs — exclut les membres qui ont quitté (INACTIVE)
        return memberRepo.findByUserIdAndStatus(userId, MemberStatus.ACTIVE)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public MemberResponse toggleProfileDisplay(String orgId, String userId, boolean display) {
        OrganizationMember member = memberRepo.findByOrganizationIdAndUserId(orgId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in organization: " + orgId));
        member.setDisplayOnProfile(display);
        return toResponse(memberRepo.save(member));
    }

    private void audit(String orgId, String userId, String action, String details) {
        auditRepo.save(AuditLog.builder()
                .organizationId(orgId).performedByUserId(userId)
                .action(action).details(details).build());
    }

    private MemberResponse toResponse(OrganizationMember m) {
        return MemberResponse.builder()
                .id(m.getId()).organizationId(m.getOrganizationId()).userId(m.getUserId())
                .role(m.getRole()).status(m.getStatus()).displayOnProfile(m.isDisplayOnProfile())
                .joinedAt(m.getJoinedAt()).leftAt(m.getLeftAt()).build();
    }
}
