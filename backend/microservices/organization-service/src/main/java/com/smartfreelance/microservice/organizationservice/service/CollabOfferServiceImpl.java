package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.ApplyCollabOfferRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.CreateCollabOfferRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.RespondCollabApplicationRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.CollabApplicationResponse;
import com.smartfreelance.microservice.organizationservice.dto.response.CollabOfferResponse;
import com.smartfreelance.microservice.organizationservice.entity.AuditLog;
import com.smartfreelance.microservice.organizationservice.entity.CollabApplication;
import com.smartfreelance.microservice.organizationservice.entity.CollabOffer;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.enums.CollabApplicationStatus;
import com.smartfreelance.microservice.organizationservice.enums.CollabOfferStatus;
import com.smartfreelance.microservice.organizationservice.enums.MemberRole;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationStatus;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.AuditLogRepository;
import com.smartfreelance.microservice.organizationservice.repository.CollabApplicationRepository;
import com.smartfreelance.microservice.organizationservice.repository.CollabOfferRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationMemberRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CollabOfferServiceImpl implements CollabOfferService {

    private final CollabOfferRepository offerRepo;
    private final CollabApplicationRepository applicationRepo;
    private final OrganizationRepository orgRepo;
    private final OrganizationMemberRepository memberRepo;
    private final AuditLogRepository auditRepo;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Organization requireActiveOrg(String orgId) {
        Organization org = orgRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation introuvable : " + orgId));
        if (org.getStatus() != OrganizationStatus.ACTIVE) {
            throw new BusinessRuleException("L'organisation n'est pas active.");
        }
        return org;
    }

    private void requireOwnerOrManager(String orgId, String userId) {
        Organization org = orgRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation introuvable : " + orgId));
        boolean isOwner = org.getOwnerId().equals(userId);
        boolean isManager = memberRepo.findByOrganizationIdAndUserId(orgId, userId)
                .map(m -> m.getRole() == MemberRole.MANAGER || m.getRole() == MemberRole.OWNER)
                .orElse(false);
        if (!isOwner && !isManager) {
            throw new BusinessRuleException("Seul un propriétaire ou un gestionnaire peut effectuer cette action.");
        }
    }

    private boolean isOwnerOrManager(String orgId, String userId) {
        Organization org = orgRepo.findById(orgId).orElse(null);
        if (org == null) return false;
        if (org.getOwnerId().equals(userId)) return true;
        return memberRepo.findByOrganizationIdAndUserId(orgId, userId)
                .map(m -> m.getRole() == MemberRole.MANAGER || m.getRole() == MemberRole.OWNER)
                .orElse(false);
    }

    private void audit(String orgId, String userId, String action, String details) {
        auditRepo.save(AuditLog.builder()
                .organizationId(orgId).performedByUserId(userId)
                .action(action).details(details).build());
    }

    // ── Offres ────────────────────────────────────────────────────────────────

    @Override
    public CollabOfferResponse createOffer(String orgId, CreateCollabOfferRequest request, String creatorId) {
        requireActiveOrg(orgId);
        requireOwnerOrManager(orgId, creatorId);

        CollabOffer offer = CollabOffer.builder()
                .organizationId(orgId)
                .createdBy(creatorId)
                .title(request.getTitle())
                .description(request.getDescription())
                .requiredSkills(request.getRequiredSkills())
                .durationLabel(request.getDurationLabel())
                .budgetEstimate(request.getBudgetEstimate())
                .maxApplicants(request.getMaxApplicants())
                .deadlineDate(request.getDeadlineDate())
                .status(CollabOfferStatus.OPEN)
                .build();

        offer = offerRepo.save(offer);
        audit(orgId, creatorId, "COLLAB_OFFER_CREATED", "Offre créée : " + offer.getTitle());
        return toOfferResponse(offer);
    }

    @Override
    @Transactional(readOnly = true)
    public CollabOfferResponse getOffer(String offerId) {
        CollabOffer offer = offerRepo.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offre introuvable : " + offerId));
        return toOfferResponse(offer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CollabOfferResponse> getOrgOffers(String orgId, Pageable pageable, String requesterId) {
        // Managers voient toutes les offres ; les autres voient seulement les offres OPEN
        if (isOwnerOrManager(orgId, requesterId)) {
            return offerRepo.findByOrganizationId(orgId, pageable).map(this::toOfferResponse);
        }
        return offerRepo.findByOrganizationIdAndStatus(orgId, CollabOfferStatus.OPEN, pageable).map(this::toOfferResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CollabOfferResponse> getPublicOffers(String orgId, Pageable pageable) {
        return offerRepo.findByOrganizationIdAndStatus(orgId, CollabOfferStatus.OPEN, pageable)
                .map(this::toOfferResponse);
    }

    @Override
    public CollabOfferResponse closeOffer(String offerId, String actorId) {
        CollabOffer offer = offerRepo.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offre introuvable : " + offerId));
        requireOwnerOrManager(offer.getOrganizationId(), actorId);
        if (offer.getStatus() != CollabOfferStatus.OPEN) {
            throw new BusinessRuleException("Seule une offre ouverte peut être clôturée.");
        }
        offer.setStatus(CollabOfferStatus.CLOSED);
        offer = offerRepo.save(offer);
        audit(offer.getOrganizationId(), actorId, "COLLAB_OFFER_CLOSED", "Offre clôturée : " + offer.getTitle());
        return toOfferResponse(offer);
    }

    @Override
    public CollabOfferResponse cancelOffer(String offerId, String actorId) {
        CollabOffer offer = offerRepo.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offre introuvable : " + offerId));
        requireOwnerOrManager(offer.getOrganizationId(), actorId);
        if (offer.getStatus() == CollabOfferStatus.CANCELLED) {
            throw new BusinessRuleException("L'offre est déjà annulée.");
        }
        offer.setStatus(CollabOfferStatus.CANCELLED);
        offer = offerRepo.save(offer);
        audit(offer.getOrganizationId(), actorId, "COLLAB_OFFER_CANCELLED", "Offre annulée : " + offer.getTitle());
        return toOfferResponse(offer);
    }

    // ── Candidatures ──────────────────────────────────────────────────────────

    @Override
    public CollabApplicationResponse apply(String offerId, ApplyCollabOfferRequest request, String applicantId) {
        CollabOffer offer = offerRepo.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offre introuvable : " + offerId));

        if (offer.getStatus() != CollabOfferStatus.OPEN) {
            throw new BusinessRuleException("Cette offre n'est plus ouverte aux candidatures.");
        }

        // Vérification date limite
        if (offer.getDeadlineDate() != null && offer.getDeadlineDate().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("La date limite de candidature est dépassée.");
        }

        // Vérification quota
        if (offer.getMaxApplicants() != null) {
            long accepted = applicationRepo.countByOfferIdAndStatus(offerId, CollabApplicationStatus.ACCEPTED);
            if (accepted >= offer.getMaxApplicants()) {
                throw new BusinessRuleException("Le quota de collaborateurs a été atteint pour cette offre.");
            }
        }

        // Candidature en double
        if (applicationRepo.existsByOfferIdAndApplicantIdAndStatus(offerId, applicantId, CollabApplicationStatus.PENDING)) {
            throw new BusinessRuleException("Vous avez déjà une candidature en attente pour cette offre.");
        }

        // L'auteur de l'offre ne peut pas postuler à sa propre offre
        if (offer.getCreatedBy().equals(applicantId)) {
            throw new BusinessRuleException("Vous ne pouvez pas postuler à votre propre offre.");
        }

        CollabApplication app = CollabApplication.builder()
                .offerId(offerId)
                .organizationId(offer.getOrganizationId())
                .applicantId(applicantId)
                .message(request.getMessage())
                .portfolioUrl(request.getPortfolioUrl())
                .build();

        app = applicationRepo.save(app);
        audit(offer.getOrganizationId(), applicantId, "COLLAB_APPLICATION_SUBMITTED",
                "Candidature pour l'offre : " + offer.getTitle());
        return toApplicationResponse(app, offer.getTitle());
    }

    @Override
    public CollabApplicationResponse respond(String applicationId, RespondCollabApplicationRequest request, String responderId) {
        CollabApplication app = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable : " + applicationId));

        if (app.getStatus() != CollabApplicationStatus.PENDING) {
            throw new BusinessRuleException("Cette candidature n'est plus en attente.");
        }

        CollabApplicationStatus newStatus = request.getStatus();
        if (newStatus != CollabApplicationStatus.ACCEPTED && newStatus != CollabApplicationStatus.REJECTED) {
            throw new BusinessRuleException("Le statut doit être ACCEPTED ou REJECTED.");
        }

        requireOwnerOrManager(app.getOrganizationId(), responderId);

        // Si on accepte, vérifier quota avant
        if (newStatus == CollabApplicationStatus.ACCEPTED) {
            CollabOffer offer = offerRepo.findById(app.getOfferId())
                    .orElseThrow(() -> new ResourceNotFoundException("Offre introuvable"));
            if (offer.getMaxApplicants() != null) {
                long accepted = applicationRepo.countByOfferIdAndStatus(app.getOfferId(), CollabApplicationStatus.ACCEPTED);
                if (accepted >= offer.getMaxApplicants()) {
                    throw new BusinessRuleException("Le quota de collaborateurs a déjà été atteint.");
                }
                // Clôture automatique de l'offre si quota atteint après cet accept
                if (accepted + 1 >= offer.getMaxApplicants()) {
                    offer.setStatus(CollabOfferStatus.CLOSED);
                    offerRepo.save(offer);
                }
            }
        }

        app.setStatus(newStatus);
        app.setRejectionReason(request.getRejectionReason());
        app.setRespondedAt(LocalDateTime.now());
        app = applicationRepo.save(app);

        String action = newStatus == CollabApplicationStatus.ACCEPTED
                ? "COLLAB_APPLICATION_ACCEPTED" : "COLLAB_APPLICATION_REJECTED";
        audit(app.getOrganizationId(), responderId, action,
                "Candidature " + applicationId + " → " + newStatus);

        // Récupérer le titre de l'offre pour la réponse
        String offerTitle = offerRepo.findById(app.getOfferId())
                .map(CollabOffer::getTitle).orElse("");
        return toApplicationResponse(app, offerTitle);
    }

    @Override
    public void withdraw(String applicationId, String applicantId) {
        CollabApplication app = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable : " + applicationId));

        if (!app.getApplicantId().equals(applicantId)) {
            throw new BusinessRuleException("Vous ne pouvez retirer que votre propre candidature.");
        }
        if (app.getStatus() != CollabApplicationStatus.PENDING) {
            throw new BusinessRuleException("Seules les candidatures en attente peuvent être retirées.");
        }

        app.setStatus(CollabApplicationStatus.WITHDRAWN);
        applicationRepo.save(app);
        audit(app.getOrganizationId(), applicantId, "COLLAB_APPLICATION_WITHDRAWN", "Candidature retirée");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CollabApplicationResponse> getApplicationsForOffer(String offerId, Pageable pageable, String requesterId) {
        CollabOffer offer = offerRepo.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offre introuvable : " + offerId));
        requireOwnerOrManager(offer.getOrganizationId(), requesterId);
        return applicationRepo.findByOfferId(offerId, pageable)
                .map(app -> toApplicationResponse(app, offer.getTitle()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CollabApplicationResponse> getMyApplications(String applicantId) {
        return applicationRepo.findByApplicantId(applicantId).stream()
                .map(app -> {
                    String title = offerRepo.findById(app.getOfferId())
                            .map(CollabOffer::getTitle).orElse("");
                    return toApplicationResponse(app, title);
                })
                .collect(Collectors.toList());
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private CollabOfferResponse toOfferResponse(CollabOffer offer) {
        // Toutes les candidatures, y compris retirées (WITHDRAWN)
        long total    = applicationRepo.countByOfferIdAndStatus(offer.getId(), CollabApplicationStatus.PENDING)
                      + applicationRepo.countByOfferIdAndStatus(offer.getId(), CollabApplicationStatus.ACCEPTED)
                      + applicationRepo.countByOfferIdAndStatus(offer.getId(), CollabApplicationStatus.REJECTED)
                      + applicationRepo.countByOfferIdAndStatus(offer.getId(), CollabApplicationStatus.WITHDRAWN);
        long accepted = applicationRepo.countByOfferIdAndStatus(offer.getId(), CollabApplicationStatus.ACCEPTED);

        return CollabOfferResponse.builder()
                .id(offer.getId())
                .organizationId(offer.getOrganizationId())
                .createdBy(offer.getCreatedBy())
                .title(offer.getTitle())
                .description(offer.getDescription())
                .requiredSkills(offer.getRequiredSkills())
                .durationLabel(offer.getDurationLabel())
                .budgetEstimate(offer.getBudgetEstimate())
                .maxApplicants(offer.getMaxApplicants())
                .deadlineDate(offer.getDeadlineDate())
                .status(offer.getStatus())
                .applicationCount(total)
                .acceptedCount(accepted)
                .createdAt(offer.getCreatedAt())
                .updatedAt(offer.getUpdatedAt())
                .build();
    }

    private CollabApplicationResponse toApplicationResponse(CollabApplication app, String offerTitle) {
        return CollabApplicationResponse.builder()
                .id(app.getId())
                .offerId(app.getOfferId())
                .organizationId(app.getOrganizationId())
                .applicantId(app.getApplicantId())
                .message(app.getMessage())
                .portfolioUrl(app.getPortfolioUrl())
                .status(app.getStatus())
                .rejectionReason(app.getRejectionReason())
                .createdAt(app.getCreatedAt())
                .respondedAt(app.getRespondedAt())
                .offerTitle(offerTitle)
                .build();
    }
}
