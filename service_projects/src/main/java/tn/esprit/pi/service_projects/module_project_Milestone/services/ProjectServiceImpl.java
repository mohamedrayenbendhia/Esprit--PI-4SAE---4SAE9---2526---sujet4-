package tn.esprit.pi.service_projects.module_project_Milestone.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.Project;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.ProjectStatus;
import tn.esprit.pi.service_projects.module_project_Milestone.repositories.ProjectRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;

    @Autowired
    public ProjectServiceImpl(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    public Project createProject(Project project) {
        log.info("Creating new project for client: {}", project.getClientId());
        
        // Establish bidirectional relationship between project and milestones
        if (project.getMilestones() != null && !project.getMilestones().isEmpty()) {
            project.getMilestones().forEach(milestone -> {
                milestone.setProject(project);
            });
        }
        
        return projectRepository.save(project);
    }

    @Override
    public Project updateProject(UUID id, Project project) {
        log.info("Updating project with id: {}", id);
        return projectRepository.findById(id)
                .map(existingProject -> {
                    existingProject.setJobOfferId(project.getJobOfferId());
                    existingProject.setTitle(project.getTitle());
                    existingProject.setFreelanceId(project.getFreelanceId());
                    existingProject.setClientId(project.getClientId());
                    existingProject.setStartDate(project.getStartDate());
                    existingProject.setEndDate(project.getEndDate());
                    existingProject.setStatus(project.getStatus());
                    existingProject.setRequirements(project.getRequirements());
                    existingProject.setDeliverables(project.getDeliverables());
                    return projectRepository.save(existingProject);
                })
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> getProjectById(UUID id) {
        log.info("Fetching project with id: {}", id);
        return projectRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> getAllProjects() {
        log.info("Fetching all projects");
        return projectRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> getProjectsByClientId(UUID clientId) {
        log.info("Fetching projects for client: {}", clientId);
        return projectRepository.findByClientId(clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> getProjectsByFreelanceId(UUID freelanceId) {
        log.info("Fetching projects for freelance: {}", freelanceId);
        return projectRepository.findByFreelanceId(freelanceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> getProjectsByStatus(ProjectStatus status) {
        log.info("Fetching projects with status: {}", status);
        return projectRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> getProjectsByJobOfferId(UUID jobOfferId) {
        log.info("Fetching projects for job offer: {}", jobOfferId);
        return projectRepository.findByJobOfferId(jobOfferId);
    }

    @Override
    public Project updateProjectStatus(UUID id, ProjectStatus status) {
        log.info("Updating project {} status to: {}", id, status);
        return projectRepository.findById(id)
                .map(project -> {
                    project.setStatus(status);
                    return projectRepository.save(project);
                })
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
    }

    @Override
    public void deleteProject(UUID id) {
        log.info("Deleting project with id: {}", id);
        projectRepository.deleteById(id);
    }

    @Override
    public void calculateAndUpdateProgress(UUID projectId) {
        log.info("Calculating progress for project: {}", projectId);
        projectRepository.findById(projectId).ifPresent(project -> {
            project.calculateProgress();
            projectRepository.save(project);
        });
    }
}
