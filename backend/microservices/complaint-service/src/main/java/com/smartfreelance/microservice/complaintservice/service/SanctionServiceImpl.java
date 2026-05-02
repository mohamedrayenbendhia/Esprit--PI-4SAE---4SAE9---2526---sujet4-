package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.dto.advanced.UserSanctionResponse;
import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.entity.UserSanction;
import com.smartfreelance.microservice.complaintservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.complaintservice.notification.ComplaintNotificationEvent;
import com.smartfreelance.microservice.complaintservice.notification.ComplaintNotificationService;
import com.smartfreelance.microservice.complaintservice.repository.ComplaintRepository;
import com.smartfreelance.microservice.complaintservice.repository.UserSanctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SanctionServiceImpl implements SanctionService {

    private final UserSanctionRepository       sanctionRepo;
    private final ComplaintRepository          complaintRepo;
    private final ComplaintNotificationService notificationService;

    @Override
    public UserSanctionResponse applyAutomatic(String userId, String triggerComplaintId) {
        long count = sanctionRepo.countByUserId(userId);
        UserSanction.SanctionType type;
        LocalDateTime expiresAt = null;
        String reason;

        if (count == 0) {
            type   = UserSanction.SanctionType.WARNING;
            reason = "Avertissement automatique — comportement signalé (réclamation #1).";
        } else if (count == 1) {
            type      = UserSanction.SanctionType.TEMP_SUSPENSION;
            expiresAt = LocalDateTime.now().plusDays(7);
            reason    = "Suspension temporaire 7 jours — récidive confirmée (réclamation #2).";
        } else {
            type   = UserSanction.SanctionType.PERMANENT_SUSPENSION;
            reason = "Suspension permanente — récidive répétée (réclamation #" + (count + 1) + ").";
        }

        UserSanction sanction = UserSanction.builder()
                .userId(userId)
                .type(type)
                .reason(reason)
                .triggerComplaintId(triggerComplaintId)
                .appliedBySystem(true)
                .expiresAt(expiresAt)
                .build();

        UserSanction saved = sanctionRepo.save(sanction);
        log.info("Auto-sanction applied to user {}: {}", userId, type);

        // GAP #1e — notifier l'utilisateur sanctionné
        fireSanctionNotification(userId, triggerComplaintId, type.name());

        return toResponse(saved);
    }

    @Override
    public UserSanctionResponse applyManual(String userId, String reason,
                                             UserSanction.SanctionType type, String adminId) {
        LocalDateTime expiresAt = type == UserSanction.SanctionType.TEMP_SUSPENSION
                ? LocalDateTime.now().plusDays(7) : null;

        UserSanction sanction = UserSanction.builder()
                .userId(userId)
                .type(type)
                .reason(reason)
                .appliedBySystem(false)
                .appliedByAdminId(adminId)
                .expiresAt(expiresAt)
                .build();

        UserSanction saved = sanctionRepo.save(sanction);

        // GAP #1e — notifier l'utilisateur sanctionné (pas de complaintId direct ici)
        fireSanctionNotification(userId, null, type.name());

        return toResponse(saved);
    }

    @Override
    public UserSanctionResponse liftSanction(String sanctionId, String adminId) {
        UserSanction s = sanctionRepo.findById(sanctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sanction not found: " + sanctionId));
        s.setActive(false);
        s.setLiftedAt(LocalDateTime.now());
        s.setLiftedByAdminId(adminId);
        return toResponse(sanctionRepo.save(s));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSanctionResponse> getForUser(String userId) {
        return sanctionRepo.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    @Override
    public int expireOldSanctions() {
        int count = sanctionRepo.expireOldSanctions(LocalDateTime.now());
        if (count > 0) log.info("Expired {} temporary sanctions.", count);
        return count;
    }

    /**
     * GAP #1e — construit et envoie l'événement SANCTION_APPLIED.
     * Utilise complaintId pour enrichir le contexte si disponible.
     */
    private void fireSanctionNotification(String targetUserId, String complaintId, String sanctionType) {
        ComplaintNotificationEvent.ComplaintNotificationEventBuilder builder =
                ComplaintNotificationEvent.builder()
                        .eventType(ComplaintNotificationEvent.EventType.SANCTION_APPLIED)
                        .reportedUserId(targetUserId)
                        .extraContext(sanctionType);

        if (complaintId != null) {
            complaintRepo.findById(complaintId).ifPresent(c -> {
                builder.complaintId(c.getId())
                       .ticketNumber(c.getTicketNumber())
                       .complaintSubject(c.getSubject())
                       .reporterId(c.getReporterId());
            });
        }

        notificationService.handle(builder.build());
    }

    private UserSanctionResponse toResponse(UserSanction s) {
        return UserSanctionResponse.builder()
                .id(s.getId())
                .userId(s.getUserId())
                .type(s.getType())
                .reason(s.getReason())
                .triggerComplaintId(s.getTriggerComplaintId())
                .active(s.isActive())
                .expiresAt(s.getExpiresAt())
                .appliedAt(s.getAppliedAt())
                .appliedBySystem(s.isAppliedBySystem())
                .build();
    }
}
