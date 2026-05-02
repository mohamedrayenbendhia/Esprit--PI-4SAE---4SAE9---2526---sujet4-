package tn.esprit.pi.service_projects.module_project_Milestone.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.MilestoneStatus;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.ProjectMilestone;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestone, UUID> {
    
    List<ProjectMilestone> findByProject_IdOrderByOrderIndexAsc(UUID projectId);
    
    List<ProjectMilestone> findByProject_IdAndStatus(UUID projectId, MilestoneStatus status);
    
    List<ProjectMilestone> findByStatus(MilestoneStatus status);
    
    @Query("SELECT pm FROM ProjectMilestone pm WHERE pm.dueDate <= :date AND pm.status NOT IN ('APPROVED', 'REJECTED')")
    List<ProjectMilestone> findOverdueMilestones(LocalDateTime date);
    
    @Query("SELECT pm FROM ProjectMilestone pm WHERE pm.dueDate BETWEEN :start AND :end AND pm.status NOT IN ('APPROVED', 'REJECTED')")
    List<ProjectMilestone> findMilestonesDueBetween(LocalDateTime start, LocalDateTime end);
    
    Long countByProject_IdAndStatus(UUID projectId, MilestoneStatus status);
}
