package tn.esprit.pi.nexlance.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.esprit.pi.nexlance.invitation.entities.Invitation;
import tn.esprit.pi.nexlance.invitation.enums.InvitationStatus;
import tn.esprit.pi.nexlance.invitation.repositories.InvitationRepository;
import tn.esprit.pi.nexlance.module_job_offers.entities.JobOffer;
import tn.esprit.pi.nexlance.module_job_offers.enums.JobOfferStatus;
import tn.esprit.pi.nexlance.module_job_offers.repositories.JobOfferRepository;
import tn.esprit.pi.nexlance.notification.NotificationService;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final JobOfferRepository jobOfferRepository;
    private final InvitationRepository invitationRepository;
    private final NotificationService notificationService;

    /**
     * Check for job offer deadlines approaching (runs every day at 8 AM)
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void checkJobOfferDeadlines() {
        log.info("Running job offer deadline check...");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysFromNow = now.plusDays(3);

        List<JobOffer> expiringOffers = jobOfferRepository.findAll().stream()
                .filter(offer -> offer.getDeadline() != null
                        && offer.getDeadline().isAfter(now)
                        && offer.getDeadline().isBefore(threeDaysFromNow)
                        && "OPEN".equals(offer.getStatus().name()))
                .toList();

        for (JobOffer offer : expiringOffers) {
            int daysRemaining = (int) java.time.Duration.between(now, offer.getDeadline()).toDays();
            notificationService.notifyDeadlineApproaching(
                    offer.getClientId().toString(),
                    "Job Offer: " + offer.getTitle(),
                    daysRemaining + 1,
                    offer.getId().toString(),
                    "JOB_OFFER"
            );
            log.info("Deadline alert sent for job offer: {}", offer.getTitle());
        }

        log.info("Job offer deadline check complete. {} alerts sent.", expiringOffers.size());
    }

    /**
     * Expire old invitations (runs every day at midnight)
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void expireOldInvitations() {
        log.info("Running invitation expiration check...");
        LocalDateTime now = LocalDateTime.now();

        List<Invitation> pendingInvitations = invitationRepository.findAll().stream()
                .filter(inv -> "PENDING".equals(inv.getStatus().name())
                        && inv.getDeadlineResponse() != null
                        && inv.getDeadlineResponse().isBefore(now))
                .toList();

        for (Invitation invitation : pendingInvitations) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);

            // Notify both parties
            notificationService.sendNotification(
                    invitation.getClientId(),
                    "INVITATION",
                    "Invitation Expired",
                    "Your invitation for job offer has expired without response",
                    invitation.getId().toString(),
                    "INVITATION"
            );
            notificationService.sendNotification(
                    invitation.getFreelanceId(),
                    "INVITATION",
                    "Invitation Expired",
                    "An invitation you received has expired",
                    invitation.getId().toString(),
                    "INVITATION"
            );
            log.info("Expired invitation: {}", invitation.getId());
        }

        log.info("Invitation expiration check complete. {} invitations expired.", pendingInvitations.size());
    }

    /**
     * Auto-close expired job offers (runs every day at 1 AM)
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void autoCloseExpiredJobOffers() {
        log.info("Running auto-close for expired job offers...");
        LocalDateTime now = LocalDateTime.now();

        List<JobOffer> expiredOffers = jobOfferRepository.findAll().stream()
                .filter(offer -> offer.getDeadline() != null
                        && offer.getDeadline().isBefore(now)
                        && "OPEN".equals(offer.getStatus().name()))
                .toList();

        for (JobOffer offer : expiredOffers) {
            offer.setStatus(JobOfferStatus.ARCHIVED);
            jobOfferRepository.save(offer);

            notificationService.sendNotification(
                    offer.getClientId().toString(),
                    "JOB_OFFER",
                    "Job Offer Closed",
                    "Your job offer \"" + offer.getTitle() + "\" has been automatically closed (deadline passed)",
                    offer.getId().toString(),
                    "JOB_OFFER"
            );
            log.info("Auto-closed job offer: {}", offer.getTitle());
        }

        log.info("Auto-close complete. {} offers closed.", expiredOffers.size());
    }
}
