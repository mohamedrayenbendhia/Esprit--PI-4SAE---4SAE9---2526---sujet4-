package tn.esprit.pi.service_projects.module_project_Milestone.services;

import tn.esprit.pi.service_projects.module_project_Milestone.entities.Project;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.ProjectStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectService {
    
    Project createProject(Project project);
    
    Project updateProject(UUID id, Project project);
    
    Optional<Project> getProjectById(UUID id);
    
    List<Project> getAllProjects();
    
    List<Project> getProjectsByClientId(UUID clientId);
    
    List<Project> getProjectsByFreelanceId(UUID freelanceId);
    
    List<Project> getProjectsByStatus(ProjectStatus status);
    
    List<Project> getProjectsByJobOfferId(UUID jobOfferId);
    
    Project updateProjectStatus(UUID id, ProjectStatus status);
    
    void deleteProject(UUID id);
    
    Project calculateAndUpdateProgress(UUID projectId);
}
