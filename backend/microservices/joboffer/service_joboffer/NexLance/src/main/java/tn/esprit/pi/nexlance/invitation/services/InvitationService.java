package tn.esprit.pi.nexlance.invitation.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.nexlance.audit.AuditLogService;
import tn.esprit.pi.nexlance.invitation.dto.CreateInvitationDto;
import tn.esprit.pi.nexlance.invitation.dto.UpdateInvitationDto;
import tn.esprit.pi.nexlance.invitation.entities.Invitation;
import tn.esprit.pi.nexlance.invitation.enums.InvitationStatus;
import tn.esprit.pi.nexlance.invitation.repositories.InvitationRepository;
import tn.esprit.pi.nexlance.module_job_offers.entities.JobOffer;
import tn.esprit.pi.nexlance.module_job_offers.repositories.JobOfferRepository;
import tn.esprit.pi.nexlance.notification.NotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvitationService {
    
    private final InvitationRepository invitationRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final JobOfferRepository jobOfferRepository;
    
    @Transactional
    public Invitation createInvitation(CreateInvitationDto dto) {
        // Vérifier si une invitation existe déjà
        if (invitationRepository.existsByClientIdAndFreelanceIdAndJobOfferId(
                dto.getClientId(), dto.getFreelanceId(), dto.getJobOfferId())) {
            throw new RuntimeException("Une invitation existe déjà pour ce freelance et cette offre");
        }
        
        Invitation invitation = Invitation.builder()
                .clientId(dto.getClientId())
                .freelanceId(dto.getFreelanceId())
                .jobOfferId(dto.getJobOfferId())
                .message(dto.getMessage())
                .proposedBudget(dto.getProposedBudget())
                .deadlineResponse(dto.getDeadlineResponse())
                .status(InvitationStatus.PENDING)
                .build();
        
        Invitation saved = invitationRepository.save(invitation);
        auditLogService.logAction("CREATE", "INVITATION", saved.getId().toString(),
                dto.getClientId(), "CLIENT",
                "Sent invitation to freelance " + dto.getFreelanceId() + " for job " + dto.getJobOfferId(),
                null, null);
        
        // Send real-time notification to the freelancer
        try {
            String jobTitle = "a job offer";
            try {
                JobOffer jobOffer = jobOfferRepository.findById(UUID.fromString(dto.getJobOfferId())).orElse(null);
                if (jobOffer != null) {
                    jobTitle = jobOffer.getTitle();
                }
            } catch (Exception e) {
                // Ignore if job offer not found
            }
            notificationService.notifyNewInvitation(
                    dto.getFreelanceId(),
                    "A client",
                    jobTitle,
                    saved.getId().toString()
            );
        } catch (Exception e) {
            System.err.println("Warning: Failed to send invitation notification: " + e.getMessage());
        }
        
        return saved;
    }
    
    public Invitation getInvitationById(Long id) {
        return invitationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invitation non trouvée avec l'ID: " + id));
    }
    
    public List<Invitation> getAllInvitations() {
        return invitationRepository.findAll();
    }
    
    public List<Invitation> getInvitationsByClientId(String clientId) {
        return invitationRepository.findByClientId(clientId);
    }
    
    public List<Invitation> getInvitationsByFreelanceId(String freelanceId) {
        return invitationRepository.findByFreelanceId(freelanceId);
    }
    
    public List<Invitation> getInvitationsByJobOfferId(String jobOfferId) {
        return invitationRepository.findByJobOfferId(jobOfferId);
    }
    
    public List<Invitation> getInvitationsByStatus(InvitationStatus status) {
        return invitationRepository.findByStatus(status);
    }
    
    @Transactional
    public Invitation updateInvitationStatus(Long id, UpdateInvitationDto dto) {
        Invitation invitation = getInvitationById(id);
        String oldStatus = invitation.getStatus() != null ? invitation.getStatus().name() : "null";
        
        invitation.setStatus(dto.getStatus());
        if (dto.getStatus() == InvitationStatus.ACCEPTED || dto.getStatus() == InvitationStatus.DECLINED) {
            invitation.setRespondedAt(LocalDateTime.now());
        }
        
        if (dto.getMessage() != null) {
            invitation.setMessage(dto.getMessage());
        }
        
        Invitation updated = invitationRepository.save(invitation);
        auditLogService.logAction("STATUS_CHANGE", "INVITATION", updated.getId().toString(),
                invitation.getFreelanceId(), "FREELANCE",
                "Invitation status changed: " + oldStatus + " \u2192 " + dto.getStatus().name(),
                oldStatus, dto.getStatus().name());
        return updated;
    }
    
    @Transactional
    public void deleteInvitation(Long id) {
        Invitation invitation = getInvitationById(id);
        auditLogService.logAction("DELETE", "INVITATION", id.toString(),
                invitation.getClientId(), "CLIENT",
                "Deleted invitation for freelance " + invitation.getFreelanceId(),
                null, null);
        invitationRepository.delete(invitation);
    }
    
    // Vérifier et expirer les invitations dépassées
    @Transactional
    public void expireOldInvitations() {
        List<Invitation> pendingInvitations = invitationRepository.findByStatus(InvitationStatus.PENDING);
        LocalDateTime now = LocalDateTime.now();
        
        for (Invitation invitation : pendingInvitations) {
            if (invitation.getDeadlineResponse() != null && 
                invitation.getDeadlineResponse().isBefore(now)) {
                invitation.setStatus(InvitationStatus.EXPIRED);
                invitationRepository.save(invitation);
            }
        }
    }
}
