package tn.esprit.pi.service_projects.module_project_Milestone.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.Project;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.ProjectStatus;
import tn.esprit.pi.service_projects.module_project_Milestone.services.ProjectService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*")
public class ProjectController {

    private final ProjectService projectService;

    @Autowired
    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * Create a new project
     * POST /api/projects
     */
    @PostMapping
    public ResponseEntity<Project> createProject(@RequestBody Project project) {
        Project createdProject = projectService.createProject(project);
        return new ResponseEntity<>(createdProject, HttpStatus.CREATED);
    }

    /**
     * Get all projects
     * GET /api/projects
     */
    @GetMapping
    public ResponseEntity<List<Project>> getAllProjects() {
        List<Project> projects = projectService.getAllProjects();
        return ResponseEntity.ok(projects);
    }

    /**
     * Get project by ID
     * GET /api/projects/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectById(@PathVariable UUID id) {
        return projectService.getProjectById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get projects by client ID
     * GET /api/projects/client/{clientId}
     */
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<Project>> getProjectsByClientId(@PathVariable UUID clientId) {
        List<Project> projects = projectService.getProjectsByClientId(clientId);
        return ResponseEntity.ok(projects);
    }

    /**
     * Get projects by freelance ID
     * GET /api/projects/freelance/{freelanceId}
     */
    @GetMapping("/freelance/{freelanceId}")
    public ResponseEntity<List<Project>> getProjectsByFreelanceId(@PathVariable UUID freelanceId) {
        List<Project> projects = projectService.getProjectsByFreelanceId(freelanceId);
        return ResponseEntity.ok(projects);
    }

    /**
     * Get projects by job offer ID
     * GET /api/projects/job-offer/{jobOfferId}
     */
    @GetMapping("/job-offer/{jobOfferId}")
    public ResponseEntity<List<Project>> getProjectsByJobOfferId(@PathVariable UUID jobOfferId) {
        List<Project> projects = projectService.getProjectsByJobOfferId(jobOfferId);
        return ResponseEntity.ok(projects);
    }

    /**
     * Get projects by status
     * GET /api/projects/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Project>> getProjectsByStatus(@PathVariable ProjectStatus status) {
        List<Project> projects = projectService.getProjectsByStatus(status);
        return ResponseEntity.ok(projects);
    }

    /**
     * Update a project
     * PUT /api/projects/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(@PathVariable UUID id, @RequestBody Project project) {
        try {
            Project updatedProject = projectService.updateProject(id, project);
            return ResponseEntity.ok(updatedProject);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update project status
     * PATCH /api/projects/{id}/status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Project> updateProjectStatus(
            @PathVariable UUID id,
            @RequestParam ProjectStatus status) {
        try {
            Project updatedProject = projectService.updateProjectStatus(id, status);
            return ResponseEntity.ok(updatedProject);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Calculate and update project progress
     * POST /api/projects/{id}/calculate-progress
     */
    @PostMapping("/{id}/calculate-progress")
    public ResponseEntity<Void> calculateProgress(@PathVariable UUID id) {
        projectService.calculateAndUpdateProgress(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Delete a project
     * DELETE /api/projects/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
