package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.CreateRfqRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.RfqResponseRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.RfqResponse;
import com.smartfreelance.microservice.organizationservice.entity.OrgRfq;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.enums.MemberRole;
import com.smartfreelance.microservice.organizationservice.enums.RfqStatus;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.OrgRfqRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationMemberRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RfqServiceImpl implements RfqService {

    private final OrgRfqRepository             rfqRepo;
    private final OrganizationRepository        orgRepo;
    private final OrganizationMemberRepository  memberRepo;
    private final TrustScoreService             trustScoreService;

    @Override
    public RfqResponse create(String orgId, CreateRfqRequest request, String requesterId) {
        OrgRfq rfq = OrgRfq.builder()
                .organizationId(orgId).requesterId(requesterId).title(request.getTitle())
                .description(request.getDescription()).budgetMin(request.getBudgetMin())
                .budgetMax(request.getBudgetMax()).deadline(request.getDeadline())
                .skillsNeeded(request.getSkillsNeeded() != null ? request.getSkillsNeeded() : new ArrayList<>())
                .build();
        return toResponse(rfqRepo.save(rfq));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RfqResponse> getByOrg(String orgId, Pageable pageable) {
        return rfqRepo.findByOrganizationId(orgId, pageable).map(this::toResponse);
    }

    @Override
    public RfqResponse respond(String rfqId, RfqResponseRequest request, String responderId) {
        OrgRfq rfq = rfqRepo.findById(rfqId)
                .orElseThrow(() -> new ResourceNotFoundException("RFQ not found: " + rfqId));
        if (rfq.getStatus() != RfqStatus.PENDING) {
            throw new BusinessRuleException("RFQ is no longer pending.");
        }
        // Seul un membre (owner ou manager) de l'organisation peut répondre
        Organization org = orgRepo.findById(rfq.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        boolean isOwner = org.getOwnerId().equals(responderId);
        boolean isMember = memberRepo.findByOrganizationIdAndUserId(rfq.getOrganizationId(), responderId)
                .map(m -> m.getRole() == MemberRole.MANAGER || m.getRole() == MemberRole.OWNER)
                .orElse(false);
        if (!isOwner && !isMember) {
            throw new BusinessRuleException("Only an owner or manager of this organization can respond to an RFQ.");
        }
        rfq.setStatus(RfqStatus.RESPONDED);
        rfq.setResponseMessage(request.getResponseMessage());
        rfq.setRespondedById(responderId);
        rfq.setRespondedAt(LocalDateTime.now());
        RfqResponse result = toResponse(rfqRepo.save(rfq));
        trustScoreService.recompute(rfq.getOrganizationId()); // taux de réponse RFQ modifié
        return result;
    }

    @Override
    public void close(String rfqId, String userId) {
        OrgRfq rfq = rfqRepo.findById(rfqId)
                .orElseThrow(() -> new ResourceNotFoundException("RFQ not found: " + rfqId));
        Organization org = orgRepo.findById(rfq.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        boolean isOwner = org.getOwnerId().equals(userId);
        boolean isManager = memberRepo.findByOrganizationIdAndUserId(rfq.getOrganizationId(), userId)
                .map(m -> m.getRole() == MemberRole.MANAGER || m.getRole() == MemberRole.OWNER)
                .orElse(false);
        if (!isOwner && !isManager) {
            throw new BusinessRuleException("Only the organization owner or a manager can close an RFQ.");
        }
        rfq.setStatus(RfqStatus.CLOSED);
        rfqRepo.save(rfq);
        trustScoreService.recompute(rfq.getOrganizationId()); // clôture = RFQ traitée
    }

    @Override
    @Transactional(readOnly = true)
    public List<RfqResponse> getMyRfqs(String requesterId) {
        return rfqRepo.findByRequesterId(requesterId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private RfqResponse toResponse(OrgRfq rfq) {
        return RfqResponse.builder()
                .id(rfq.getId()).organizationId(rfq.getOrganizationId()).requesterId(rfq.getRequesterId())
                .title(rfq.getTitle()).description(rfq.getDescription()).budgetMin(rfq.getBudgetMin())
                .budgetMax(rfq.getBudgetMax()).deadline(rfq.getDeadline()).skillsNeeded(rfq.getSkillsNeeded())
                .status(rfq.getStatus()).responseMessage(rfq.getResponseMessage())
                .respondedById(rfq.getRespondedById()).createdAt(rfq.getCreatedAt())
                .respondedAt(rfq.getRespondedAt()).build();
    }
}
