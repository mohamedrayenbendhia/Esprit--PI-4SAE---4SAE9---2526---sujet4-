package tn.esprit.pi.service_projects.module_project_Milestone.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.Project;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.ProjectStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @Override
    @EntityGraph(attributePaths = "milestones")
    Optional<Project> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = "milestones")
    List<Project> findAll();
    
    @EntityGraph(attributePaths = "milestones")
    List<Project> findByClientId(UUID clientId);
    
    @EntityGraph(attributePaths = "milestones")
    List<Project> findByFreelanceId(UUID freelanceId);
    
    @EntityGraph(attributePaths = "milestones")
    List<Project> findByStatus(ProjectStatus status);
    
    @EntityGraph(attributePaths = "milestones")
    List<Project> findByJobOfferId(UUID jobOfferId);
    
    @EntityGraph(attributePaths = "milestones")
    List<Project> findByClientIdAndStatus(UUID clientId, ProjectStatus status);
    
    @EntityGraph(attributePaths = "milestones")
    List<Project> findByFreelanceIdAndStatus(UUID freelanceId, ProjectStatus status);
}
