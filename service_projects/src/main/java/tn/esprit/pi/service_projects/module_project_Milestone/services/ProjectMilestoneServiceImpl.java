package tn.esprit.pi.service_projects.module_project_Milestone.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.MilestoneStatus;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.ProjectMilestone;
import tn.esprit.pi.service_projects.module_project_Milestone.repositories.ProjectMilestoneRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class ProjectMilestoneServiceImpl implements ProjectMilestoneService {

    private final ProjectMilestoneRepository milestoneRepository;
    private final ProjectService projectService;

    @Autowired
    public ProjectMilestoneServiceImpl(ProjectMilestoneRepository milestoneRepository, ProjectService projectService) {
        this.milestoneRepository = milestoneRepository;
        this.projectService = projectService;
    }

    @Override
    public ProjectMilestone createMilestone(ProjectMilestone milestone) {
        log.info("Creating new milestone: {}", milestone.getTitle());
        // Resolve the project reference from DB to avoid transient entity error
        if (milestone.getProject() != null && milestone.getProject().getId() != null) {
            var project = projectService.getProjectById(milestone.getProject().getId())
                    .orElseThrow(() -> new RuntimeException("Project not found: " + milestone.getProject().getId()));
            milestone.setProject(project);
        }
        return milestoneRepository.save(milestone);
    }

    @Override
    public ProjectMilestone updateMilestone(UUID id, ProjectMilestone milestone) {
        log.info("Updating milestone with id: {}", id);
        return milestoneRepository.findById(id)
                .map(existingMilestone -> {
                    existingMilestone.setTitle(milestone.getTitle());
                    existingMilestone.setDescription(milestone.getDescription());
                    existingMilestone.setOrderIndex(milestone.getOrderIndex());
                    existingMilestone.setDueDate(milestone.getDueDate());
                    existingMilestone.setDeliverables(milestone.getDeliverables());
                    existingMilestone.setAcceptanceCriteria(milestone.getAcceptanceCriteria());
                    return milestoneRepository.save(existingMilestone);
                })
                .orElseThrow(() -> new RuntimeException("Milestone not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProjectMilestone> getMilestoneById(UUID id) {
        log.info("Fetching milestone with id: {}", id);
        return milestoneRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMilestone> getAllMilestones() {
        log.info("Fetching all milestones");
        return milestoneRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMilestone> getMilestonesByProjectId(UUID projectId) {
        log.info("Fetching milestones for project: {}", projectId);
        return milestoneRepository.findByProjectIdOrderByOrderIndexAsc(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMilestone> getMilestonesByProjectIdAndStatus(UUID projectId, MilestoneStatus status) {
        log.info("Fetching milestones for project: {} with status: {}", projectId, status);
        return milestoneRepository.findByProjectIdAndStatus(projectId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMilestone> getMilestonesByStatus(MilestoneStatus status) {
        log.info("Fetching milestones with status: {}", status);
        return milestoneRepository.findByStatus(status);
    }

    @Override
    public ProjectMilestone submitMilestone(UUID id, String attachments, String comment) {
        log.info("Submitting milestone with id: {}", id);
        return milestoneRepository.findById(id)
                .map(milestone -> {
                    milestone.setStatus(MilestoneStatus.SUBMITTED);
                    milestone.setSubmittedAt(LocalDateTime.now());
                    if (attachments != null) {
                        milestone.setAttachments(attachments);
                    }
                    ProjectMilestone savedMilestone = milestoneRepository.save(milestone);
                    
                    // Update project progress
                    if (milestone.getProject() != null) {
                        projectService.calculateAndUpdateProgress(milestone.getProject().getId());
                    }
                    
                    return savedMilestone;
                })
                .orElseThrow(() -> new RuntimeException("Milestone not found with id: " + id));
    }

    @Override
    public ProjectMilestone approveMilestone(UUID id) {
        log.info("Approving milestone with id: {}", id);
        return milestoneRepository.findById(id)
                .map(milestone -> {
                    milestone.setStatus(MilestoneStatus.APPROVED);
                    milestone.setApprovedAt(LocalDateTime.now());
                    milestone.setRejectionReason(null);
                    ProjectMilestone savedMilestone = milestoneRepository.save(milestone);
                    
                    // Update project progress
                    if (milestone.getProject() != null) {
                        projectService.calculateAndUpdateProgress(milestone.getProject().getId());
                    }
                    
                    return savedMilestone;
                })
                .orElseThrow(() -> new RuntimeException("Milestone not found with id: " + id));
    }

    @Override
    public ProjectMilestone rejectMilestone(UUID id, String rejectionReason) {
        log.info("Rejecting milestone with id: {}", id);
        return milestoneRepository.findById(id)
                .map(milestone -> {
                    milestone.setStatus(MilestoneStatus.REJECTED);
                    milestone.setRejectionReason(rejectionReason);
                    milestone.setApprovedAt(null);
                    ProjectMilestone savedMilestone = milestoneRepository.save(milestone);
                    
                    // Update project progress
                    if (milestone.getProject() != null) {
                        projectService.calculateAndUpdateProgress(milestone.getProject().getId());
                    }
                    
                    return savedMilestone;
                })
                .orElseThrow(() -> new RuntimeException("Milestone not found with id: " + id));
    }

    @Override
    public ProjectMilestone updateMilestoneStatus(UUID id, MilestoneStatus status) {
        log.info("Updating milestone {} status to: {}", id, status);
        return milestoneRepository.findById(id)
                .map(milestone -> {
                    milestone.setStatus(status);
                    return milestoneRepository.save(milestone);
                })
                .orElseThrow(() -> new RuntimeException("Milestone not found with id: " + id));
    }

    @Override
    public void deleteMilestone(UUID id) {
        log.info("Deleting milestone with id: {}", id);
        milestoneRepository.findById(id).ifPresent(milestone -> {
            UUID projectId = milestone.getProject() != null ? milestone.getProject().getId() : null;
            milestoneRepository.deleteById(id);
            
            // Update project progress after deletion
            if (projectId != null) {
                projectService.calculateAndUpdateProgress(projectId);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMilestone> getOverdueMilestones() {
        log.info("Fetching overdue milestones");
        return milestoneRepository.findOverdueMilestones(LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMilestone> getMilestonesDueSoon(int days) {
        log.info("Fetching milestones due in {} days", days);
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(days);
        return milestoneRepository.findMilestonesDueBetween(start, end);
    }

    @Override
    public ProjectMilestone adminApproveMilestone(UUID id, String adminNotes) {
        log.info("Admin approving milestone with id: {} (notes: {})", id, adminNotes);
        return milestoneRepository.findById(id)
                .map(milestone -> {
                    milestone.setStatus(MilestoneStatus.APPROVED);
                    milestone.setApprovedAt(LocalDateTime.now());
                    milestone.setRejectionReason(null); // Clear rejection reason on approval
                    ProjectMilestone savedMilestone = milestoneRepository.save(milestone);
                    
                    // Update project progress
                    if (milestone.getProject() != null) {
                        projectService.calculateAndUpdateProgress(milestone.getProject().getId());
                    }
                    
                    log.info("Milestone {} approved by admin", id);
                    return savedMilestone;
                })
                .orElseThrow(() -> new RuntimeException("Milestone not found with id: " + id));
    }

    @Override
    public ProjectMilestone adminRequestRevisions(UUID id, String adminNotes) {
        log.info("Admin requesting revisions for milestone with id: {} (notes: {})", id, adminNotes);
        return milestoneRepository.findById(id)
                .map(milestone -> {
                    milestone.setStatus(MilestoneStatus.REJECTED);
                    milestone.setRejectionReason("Admin: " + (adminNotes != null ? adminNotes : "Révisions requises"));
                    milestone.setApprovedAt(null);
                    ProjectMilestone savedMilestone = milestoneRepository.save(milestone);
                    
                    // Update project progress
                    if (milestone.getProject() != null) {
                        projectService.calculateAndUpdateProgress(milestone.getProject().getId());
                    }
                    
                    log.info("Revisions requested by admin for milestone {}", id);
                    return savedMilestone;
                })
                .orElseThrow(() -> new RuntimeException("Milestone not found with id: " + id));
    }
}
