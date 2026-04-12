package tn.esprit.pi.service_projects.module_project_Milestone.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.Project;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.ProjectStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    
    List<Project> findByClientId(UUID clientId);
    
    List<Project> findByFreelanceId(UUID freelanceId);
    
    List<Project> findByStatus(ProjectStatus status);
    
    List<Project> findByJobOfferId(UUID jobOfferId);
    
    List<Project> findByClientIdAndStatus(UUID clientId, ProjectStatus status);
    
    List<Project> findByFreelanceIdAndStatus(UUID freelanceId, ProjectStatus status);
}
