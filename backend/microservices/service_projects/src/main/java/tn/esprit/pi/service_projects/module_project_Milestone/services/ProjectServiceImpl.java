package tn.esprit.pi.service_projects.module_project_Milestone.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.Project;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.ProjectStatus;
import tn.esprit.pi.service_projects.module_project_Milestone.repositories.ProjectRepository;
import tn.esprit.pi.service_projects.chat.NotificationClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final NotificationClient notificationClient;

    @Autowired
    public ProjectServiceImpl(ProjectRepository projectRepository, NotificationClient notificationClient) {
        this.projectRepository = projectRepository;
        this.notificationClient = notificationClient;
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
        
        Project saved = projectRepository.save(project);
        notifyProjectCreated(saved);
        return saved;
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
                    Project saved = projectRepository.save(project);
                    notifyProjectStatusChanged(saved);
                    return saved;
                })
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
    }

    @Override
    public void deleteProject(UUID id) {
        log.info("Deleting project with id: {}", id);
        projectRepository.deleteById(id);
    }

    @Override
    public Project calculateAndUpdateProgress(UUID projectId) {
        log.info("Calculating progress for project: {}", projectId);
        return projectRepository.findById(projectId)
                .map(project -> {
                    project.calculateProgress();
                    return projectRepository.save(project);
                })
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
    }

    private void notifyProjectCreated(Project project) {
        String title = project.getTitle() != null ? project.getTitle() : "Projet";
        String projectId = project.getId() != null ? project.getId().toString() : null;
        if (project.getClientId() != null) {
            notificationClient.sendNotification(
                    project.getClientId().toString(),
                    "PROJECT",
                    "Projet cree",
                    "Votre projet \"" + title + "\" a ete cree",
                    projectId,
                    "PROJECT"
            );
        }
        if (project.getFreelanceId() != null) {
            notificationClient.sendNotification(
                    project.getFreelanceId().toString(),
                    "PROJECT",
                    "Nouveau projet",
                    "Vous avez ete associe au projet \"" + title + "\"",
                    projectId,
                    "PROJECT"
            );
        }
    }

    private void notifyProjectStatusChanged(Project project) {
        String title = project.getTitle() != null ? project.getTitle() : "Projet";
        String projectId = project.getId() != null ? project.getId().toString() : null;
        String message = "Le projet \"" + title + "\" est maintenant " + project.getStatus();
        if (project.getClientId() != null) {
            notificationClient.sendNotification(project.getClientId().toString(), "PROJECT", "Statut projet", message, projectId, "PROJECT");
        }
        if (project.getFreelanceId() != null) {
            notificationClient.sendNotification(project.getFreelanceId().toString(), "PROJECT", "Statut projet", message, projectId, "PROJECT");
        }
    }
}
