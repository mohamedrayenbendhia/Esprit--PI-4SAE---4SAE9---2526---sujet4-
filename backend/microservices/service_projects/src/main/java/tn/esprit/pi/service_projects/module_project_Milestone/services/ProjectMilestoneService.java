package tn.esprit.pi.service_projects.module_project_Milestone.services;

import tn.esprit.pi.service_projects.module_project_Milestone.entities.MilestoneStatus;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.ProjectMilestone;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMilestoneService {
    
    ProjectMilestone createMilestone(ProjectMilestone milestone);
    
    ProjectMilestone updateMilestone(UUID id, ProjectMilestone milestone);
    
    Optional<ProjectMilestone> getMilestoneById(UUID id);
    
    List<ProjectMilestone> getAllMilestones();
    
    List<ProjectMilestone> getMilestonesByProjectId(UUID projectId);
    
    List<ProjectMilestone> getMilestonesByProjectIdAndStatus(UUID projectId, MilestoneStatus status);
    
    List<ProjectMilestone> getMilestonesByStatus(MilestoneStatus status);
    
    ProjectMilestone submitMilestone(UUID id, String attachments, String comment);
    
    ProjectMilestone approveMilestone(UUID id);
    
    ProjectMilestone rejectMilestone(UUID id, String rejectionReason);
    
    ProjectMilestone updateMilestoneStatus(UUID id, MilestoneStatus status);
    
    void deleteMilestone(UUID id);
    
    List<ProjectMilestone> getOverdueMilestones();
    
    List<ProjectMilestone> getMilestonesDueSoon(int days);
    
    // Admin actions
    ProjectMilestone adminApproveMilestone(UUID id, String adminNotes);
    
    ProjectMilestone adminRequestRevisions(UUID id, String adminNotes);
}
