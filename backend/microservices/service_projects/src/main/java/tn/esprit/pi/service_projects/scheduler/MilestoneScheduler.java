package tn.esprit.pi.service_projects.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.Project;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.ProjectMilestone;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.MilestoneStatus;
import tn.esprit.pi.service_projects.module_project_Milestone.repositories.ProjectMilestoneRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class MilestoneScheduler {

    private final ProjectMilestoneRepository milestoneRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Check for milestone deadlines approaching (runs every day at 9 AM)
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void checkMilestoneDeadlines() {
        log.info("Running milestone deadline check...");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysFromNow = now.plusDays(3);

        List<ProjectMilestone> dueSoonMilestones = milestoneRepository.findAll().stream()
                .filter(m -> m.getDueDate() != null
                        && m.getDueDate().isAfter(now)
                        && m.getDueDate().isBefore(threeDaysFromNow)
                        && m.getStatus() != MilestoneStatus.APPROVED)
                .toList();

        for (ProjectMilestone milestone : dueSoonMilestones) {
            int daysRemaining = (int) Duration.between(now, milestone.getDueDate()).toDays() + 1;
            Project project = milestone.getProject();

            if (project != null) {
                // Notify freelancer
                Map<String, Object> notification = buildNotification(
                        "DEADLINE",
                        "Milestone Deadline Approaching",
                        "Milestone \"" + milestone.getTitle() + "\" in project \"" + project.getTitle() + "\" is due in " + daysRemaining + " day(s)",
                        milestone.getId().toString(),
                        "MILESTONE"
                );

                messagingTemplate.convertAndSend(
                        "/topic/notifications/" + project.getFreelanceId(),
                        (Object) notification
                );

                // Also notify client
                messagingTemplate.convertAndSend(
                        "/topic/notifications/" + project.getClientId(),
                        (Object) notification
                );

                log.info("Deadline alert sent for milestone: {} ({} days remaining)", milestone.getTitle(), daysRemaining);
            }
        }

        log.info("Milestone deadline check complete. {} alerts sent.", dueSoonMilestones.size());
    }

    /**
     * Mark overdue milestones (runs every day at midnight)
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void checkOverdueMilestones() {
        log.info("Checking for overdue milestones...");
        LocalDateTime now = LocalDateTime.now();

        List<ProjectMilestone> overdueMilestones = milestoneRepository.findAll().stream()
                .filter(m -> m.getDueDate() != null
                        && m.getDueDate().isBefore(now)
                        && m.getStatus() != MilestoneStatus.APPROVED
                        && m.getStatus() != MilestoneStatus.REJECTED)
                .toList();

        for (ProjectMilestone milestone : overdueMilestones) {
            Project project = milestone.getProject();
            if (project != null) {
                int daysOverdue = (int) Duration.between(milestone.getDueDate(), now).toDays();

                Map<String, Object> notification = buildNotification(
                        "OVERDUE",
                        "Milestone Overdue",
                        "Milestone \"" + milestone.getTitle() + "\" is overdue by " + daysOverdue + " day(s)",
                        milestone.getId().toString(),
                        "MILESTONE"
                );

                messagingTemplate.convertAndSend(
                        "/topic/notifications/" + project.getFreelanceId(),
                        (Object) notification
                );
                messagingTemplate.convertAndSend(
                        "/topic/notifications/" + project.getClientId(),
                        (Object) notification
                );
            }
        }

        log.info("Overdue check complete. {} overdue milestones found.", overdueMilestones.size());
    }

    private Map<String, Object> buildNotification(String type, String title, String message, String referenceId, String referenceType) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", type);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("referenceId", referenceId);
        notification.put("referenceType", referenceType);
        notification.put("timestamp", LocalDateTime.now().toString());
        notification.put("read", false);
        return notification;
    }
}
