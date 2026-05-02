package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.InviteMemberRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.InvitationResponse;
import com.smartfreelance.microservice.organizationservice.entity.AuditLog;
import com.smartfreelance.microservice.organizationservice.entity.Invitation;
import com.smartfreelance.microservice.organizationservice.entity.OrganizationMember;
import com.smartfreelance.microservice.organizationservice.enums.InvitationStatus;
import com.smartfreelance.microservice.organizationservice.enums.MemberRole;
import com.smartfreelance.microservice.organizationservice.enums.MemberStatus;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.AuditLogRepository;
import com.smartfreelance.microservice.organizationservice.repository.InvitationRepository;
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
public class InvitationServiceImpl implements InvitationService {

    private final InvitationRepository          invitationRepo;
    private final OrganizationMemberRepository  memberRepo;
    private final AuditLogRepository            auditRepo;
    private final TrustScoreService             trustScoreService;

    @Override
    public InvitationResponse invite(String orgId, InviteMemberRequest request, String inviterId) {
        // Seul un OWNER ou MANAGER peut inviter de nouveaux membres
        OrganizationMember inviter = memberRepo.findByOrganizationIdAndUserId(orgId, inviterId)
                .orElseThrow(() -> new BusinessRuleException("You must be a member of this organization to invite others."));
        if (inviter.getRole() != MemberRole.OWNER && inviter.getRole() != MemberRole.MANAGER) {
            throw new BusinessRuleException("Only owners and managers can invite new members.");
        }
        if (request.getInviteeId() != null && request.getInviteeId().equals(inviterId)) {
            throw new BusinessRuleException("You cannot invite yourself.");
        }
        // Also check invitee is not already a member
        if (request.getInviteeId() != null && memberRepo.findByOrganizationIdAndUserId(orgId, request.getInviteeId()).isPresent()) {
            throw new BusinessRuleException("This user is already a member of the organization.");
        }
        if (request.getInviteeId() != null &&
                invitationRepo.existsByOrganizationIdAndInviteeIdAndStatus(orgId, request.getInviteeId(), InvitationStatus.PENDING)) {
            throw new BusinessRuleException("A pending invitation already exists for this user.");
        }
        String inviteeEmail = normalizeEmail(request.getInviteeEmail());
        if (inviteeEmail != null &&
                invitationRepo.existsByOrganizationIdAndInviteeEmailIgnoreCaseAndStatus(orgId, inviteeEmail, InvitationStatus.PENDING)) {
            throw new BusinessRuleException("A pending invitation already exists for this email.");
        }
        Invitation inv = Invitation.builder()
                .organizationId(orgId).inviterId(inviterId)
                .inviteeId(request.getInviteeId()).inviteeEmail(inviteeEmail)
                .role(request.getRole() != null ? request.getRole() : MemberRole.MEMBER)
                .message(request.getMessage()).build();
        inv = invitationRepo.save(inv);
        audit(orgId, inviterId, "INVITATION_SENT", "Invited user: " + request.getInviteeId());
        return toResponse(inv);
    }

    @Override
    public InvitationResponse respond(String invitationId, boolean accepted, String userId) {
        return respond(invitationId, accepted, userId, null);
    }

    @Override
    public InvitationResponse respond(String invitationId, boolean accepted, String userId, String userEmail) {
        Invitation inv = invitationRepo.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found: " + invitationId));
        if (!isRecipient(inv, userId, userEmail)) {
            throw new BusinessRuleException("You are not the recipient of this invitation.");
        }
        if (inv.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessRuleException("Invitation is no longer pending.");
        }
        if (inv.getExpiresAt().isBefore(LocalDateTime.now())) {
            inv.setStatus(InvitationStatus.EXPIRED);
            invitationRepo.save(inv);
            throw new BusinessRuleException("Invitation has expired.");
        }
        if (accepted) {
            inv.setStatus(InvitationStatus.ACCEPTED);
            OrganizationMember member = OrganizationMember.builder()
                    .organizationId(inv.getOrganizationId()).userId(userId)
                    .role(inv.getRole()).status(MemberStatus.ACTIVE).build();
            memberRepo.save(member);
            audit(inv.getOrganizationId(), userId, "INVITATION_ACCEPTED", "User joined the organization");
        } else {
            inv.setStatus(InvitationStatus.DECLINED);
            audit(inv.getOrganizationId(), userId, "INVITATION_DECLINED", "User declined the invitation");
        }
        inv.setRespondedAt(LocalDateTime.now());
        InvitationResponse result = toResponse(invitationRepo.save(inv));
        trustScoreService.recompute(inv.getOrganizationId()); // taux acceptation invitations modifié
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvitationResponse> getOrgInvitations(String orgId, Pageable pageable) {
        return invitationRepo.findByOrganizationId(orgId, pageable).map(this::toResponse);
    }

    @Override
    public void cancel(String invitationId, String requesterId) {
        Invitation inv = invitationRepo.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found: " + invitationId));
        if (inv.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessRuleException("Only pending invitations can be cancelled.");
        }
        inv.setStatus(InvitationStatus.CANCELLED);
        invitationRepo.save(inv);
        audit(inv.getOrganizationId(), requesterId, "INVITATION_CANCELLED", "Invitation cancelled");
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationResponse> getMyPendingInvitations(String userId) {
        return getMyPendingInvitations(userId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationResponse> getMyPendingInvitations(String userId, String userEmail) {
        return invitationRepo.findPendingForInvitee(userId, normalizeEmail(userEmail), InvitationStatus.PENDING)
                .stream()
                .filter(inv -> inv.getExpiresAt() == null || inv.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public InvitationResponse respondByToken(String token, boolean accepted, String userId) {
        return respondByToken(token, accepted, userId, null);
    }

    @Override
    public InvitationResponse respondByToken(String token, boolean accepted, String userId, String userEmail) {
        Invitation inv = invitationRepo.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found for token: " + token));
        return respond(inv.getId(), accepted, userId, userEmail);
    }

    private void audit(String orgId, String userId, String action, String details) {
        auditRepo.save(AuditLog.builder()
                .organizationId(orgId).performedByUserId(userId)
                .action(action).details(details).build());
    }

    private InvitationResponse toResponse(Invitation inv) {
        return InvitationResponse.builder()
                .id(inv.getId()).organizationId(inv.getOrganizationId())
                .inviterId(inv.getInviterId()).inviteeId(inv.getInviteeId())
                .inviteeEmail(inv.getInviteeEmail()).role(inv.getRole()).status(inv.getStatus())
                .message(inv.getMessage()).expiresAt(inv.getExpiresAt())
                .createdAt(inv.getCreatedAt()).respondedAt(inv.getRespondedAt()).build();
    }

    private boolean isRecipient(Invitation inv, String userId, String userEmail) {
        if (inv.getInviteeId() != null && inv.getInviteeId().equals(userId)) {
            return true;
        }
        String invitationEmail = normalizeEmail(inv.getInviteeEmail());
        String currentEmail = normalizeEmail(userEmail);
        return invitationEmail != null && invitationEmail.equals(currentEmail);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}
