package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.CreateApplicationRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.RespondApplicationRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.ApplicationResponse;
import com.smartfreelance.microservice.organizationservice.entity.AuditLog;
import com.smartfreelance.microservice.organizationservice.entity.OrgApplication;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.entity.OrganizationMember;
import com.smartfreelance.microservice.organizationservice.enums.ApplicationStatus;
import com.smartfreelance.microservice.organizationservice.enums.MemberRole;
import com.smartfreelance.microservice.organizationservice.enums.MemberStatus;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationStatus;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.AuditLogRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrgApplicationRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationMemberRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationRepository;
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
public class ApplicationServiceImpl implements ApplicationService {

    private final OrgApplicationRepository appRepo;
    private final OrganizationMemberRepository memberRepo;
    private final OrganizationRepository orgRepo;
    private final AuditLogRepository auditRepo;

    @Override
    public ApplicationResponse apply(String orgId, CreateApplicationRequest request, String applicantId) {
        Organization org = orgRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        if (org.getStatus() != OrganizationStatus.ACTIVE) {
            throw new BusinessRuleException("Cannot apply to an organization that is not active.");
        }
        boolean alreadyMember = memberRepo.findByOrganizationIdAndUserId(orgId, applicantId).isPresent();
        if (alreadyMember) {
            throw new BusinessRuleException("You are already a member of this organization.");
        }
        if (appRepo.existsByOrganizationIdAndApplicantIdAndStatus(orgId, applicantId, ApplicationStatus.PENDING)) {
            throw new BusinessRuleException("You already have a pending application for this organization.");
        }
        OrgApplication app = OrgApplication.builder()
                .organizationId(orgId).applicantId(applicantId)
                .message(request.getMessage()).cvUrl(request.getCvUrl()).build();
        app = appRepo.save(app);
        audit(orgId, applicantId, "APPLICATION_SUBMITTED", "Application submitted by: " + applicantId);
        return toResponse(app);
    }

    @Override
    public ApplicationResponse respond(String applicationId, RespondApplicationRequest request, String responderId) {
        OrgApplication app = appRepo.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
        if (app.getStatus() != ApplicationStatus.PENDING) {
            throw new BusinessRuleException("Application is no longer pending.");
        }
        if (request.getStatus() != ApplicationStatus.ACCEPTED && request.getStatus() != ApplicationStatus.REJECTED) {
            throw new BusinessRuleException("Response status must be ACCEPTED or REJECTED.");
        }
        String orgId = app.getOrganizationId();
        Organization orgForAuth = orgRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        boolean isOwner = orgForAuth.getOwnerId().equals(responderId);
        boolean isManager = memberRepo.findByOrganizationIdAndUserId(orgId, responderId)
                .map(m -> m.getRole() == MemberRole.MANAGER || m.getRole() == MemberRole.OWNER)
                .orElse(false);
        if (!isOwner && !isManager) {
            throw new BusinessRuleException("Only the organization owner or a manager can respond to applications.");
        }
        app.setStatus(request.getStatus());
        app.setRejectionReason(request.getRejectionReason());
        app.setRespondedAt(LocalDateTime.now());
        if (request.getStatus() == ApplicationStatus.ACCEPTED) {
            OrganizationMember member = OrganizationMember.builder()
                    .organizationId(app.getOrganizationId()).userId(app.getApplicantId())
                    .role(MemberRole.MEMBER).status(MemberStatus.ACTIVE).build();
            memberRepo.save(member);
            audit(app.getOrganizationId(), responderId, "APPLICATION_ACCEPTED", "Applicant " + app.getApplicantId() + " accepted");
        } else {
            audit(app.getOrganizationId(), responderId, "APPLICATION_REJECTED", "Applicant " + app.getApplicantId() + " rejected");
        }
        return toResponse(appRepo.save(app));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getOrgApplications(String orgId, Pageable pageable) {
        return appRepo.findByOrganizationId(orgId, pageable).map(this::toResponse);
    }

    @Override
    public void withdraw(String applicationId, String applicantId) {
        OrgApplication app = appRepo.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
        if (!app.getApplicantId().equals(applicantId)) {
            throw new BusinessRuleException("You can only withdraw your own application.");
        }
        if (app.getStatus() != ApplicationStatus.PENDING) {
            throw new BusinessRuleException("Only pending applications can be withdrawn.");
        }
        app.setStatus(ApplicationStatus.WITHDRAWN);
        appRepo.save(app);
        audit(app.getOrganizationId(), applicantId, "APPLICATION_WITHDRAWN", "Application withdrawn by applicant");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplications(String applicantId) {
        return appRepo.findByApplicantId(applicantId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private void audit(String orgId, String userId, String action, String details) {
        auditRepo.save(AuditLog.builder()
                .organizationId(orgId).performedByUserId(userId)
                .action(action).details(details).build());
    }

    private ApplicationResponse toResponse(OrgApplication app) {
        return ApplicationResponse.builder()
                .id(app.getId()).organizationId(app.getOrganizationId())
                .applicantId(app.getApplicantId()).message(app.getMessage()).cvUrl(app.getCvUrl())
                .status(app.getStatus()).rejectionReason(app.getRejectionReason())
                .createdAt(app.getCreatedAt()).respondedAt(app.getRespondedAt()).build();
    }
}
