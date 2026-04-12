package tn.esprit.pi.service_projects.module_project_Milestone.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.MilestoneStatus;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.Project;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.ProjectMilestone;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.ProjectStatus;
import tn.esprit.pi.service_projects.module_project_Milestone.repositories.ProjectRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private Project project;
    private UUID projectId;
    private UUID clientId;
    private UUID freelanceId;
    private UUID jobOfferId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        freelanceId = UUID.randomUUID();
        jobOfferId = UUID.randomUUID();

        project = Project.builder()
                .id(projectId)
                .title("Web Application")
                .clientId(clientId)
                .freelanceId(freelanceId)
                .jobOfferId(jobOfferId)
                .status(ProjectStatus.ACTIVE)
                .progress(0)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusMonths(3))
                .requirements("Build a web app")
                .milestones(new ArrayList<>())
                .build();
    }

    @Test
    void createProject_Success() {
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        Project result = projectService.createProject(project);

        assertNotNull(result);
        assertEquals("Web Application", result.getTitle());
        assertEquals(ProjectStatus.ACTIVE, result.getStatus());
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void createProject_WithMilestones_SetsBidirectionalRelation() {
        ProjectMilestone milestone = new ProjectMilestone();
        milestone.setTitle("Phase 1");
        project.setMilestones(List.of(milestone));

        when(projectRepository.save(any(Project.class))).thenReturn(project);

        projectService.createProject(project);

        verify(projectRepository).save(argThat(p ->
                p.getMilestones().get(0).getProject() == p));
    }

    @Test
    void getProjectById_Success() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        Optional<Project> result = projectService.getProjectById(projectId);

        assertTrue(result.isPresent());
        assertEquals(projectId, result.get().getId());
    }

    @Test
    void getProjectById_NotFound_ReturnsEmpty() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        Optional<Project> result = projectService.getProjectById(projectId);

        assertTrue(result.isEmpty());
    }

    @Test
    void getAllProjects_ReturnsList() {
        when(projectRepository.findAll()).thenReturn(List.of(project));

        List<Project> result = projectService.getAllProjects();

        assertEquals(1, result.size());
        assertEquals("Web Application", result.get(0).getTitle());
    }

    @Test
    void getProjectsByClientId_ReturnsList() {
        when(projectRepository.findByClientId(clientId)).thenReturn(List.of(project));

        List<Project> result = projectService.getProjectsByClientId(clientId);

        assertEquals(1, result.size());
        assertEquals(clientId, result.get(0).getClientId());
    }

    @Test
    void getProjectsByFreelanceId_ReturnsList() {
        when(projectRepository.findByFreelanceId(freelanceId)).thenReturn(List.of(project));

        List<Project> result = projectService.getProjectsByFreelanceId(freelanceId);

        assertEquals(1, result.size());
        assertEquals(freelanceId, result.get(0).getFreelanceId());
    }

    @Test
    void getProjectsByStatus_ReturnsList() {
        when(projectRepository.findByStatus(ProjectStatus.ACTIVE)).thenReturn(List.of(project));

        List<Project> result = projectService.getProjectsByStatus(ProjectStatus.ACTIVE);

        assertEquals(1, result.size());
        assertEquals(ProjectStatus.ACTIVE, result.get(0).getStatus());
    }

    @Test
    void getProjectsByJobOfferId_ReturnsList() {
        when(projectRepository.findByJobOfferId(jobOfferId)).thenReturn(List.of(project));

        List<Project> result = projectService.getProjectsByJobOfferId(jobOfferId);

        assertEquals(1, result.size());
        assertEquals(jobOfferId, result.get(0).getJobOfferId());
    }

    @Test
    void updateProject_Success() {
        Project updatedData = Project.builder()
                .title("Updated App")
                .clientId(clientId)
                .freelanceId(freelanceId)
                .jobOfferId(jobOfferId)
                .status(ProjectStatus.ON_HOLD)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusMonths(6))
                .requirements("Updated requirements")
                .deliverables("Updated deliverables")
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        Project result = projectService.updateProject(projectId, updatedData);

        assertNotNull(result);
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void updateProject_NotFound_ThrowsException() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> projectService.updateProject(projectId, project));
    }

    @Test
    void updateProjectStatus_Success() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        Project result = projectService.updateProjectStatus(projectId, ProjectStatus.COMPLETED);

        assertNotNull(result);
        verify(projectRepository).save(argThat(p -> p.getStatus() == ProjectStatus.COMPLETED));
    }

    @Test
    void updateProjectStatus_NotFound_ThrowsException() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> projectService.updateProjectStatus(projectId, ProjectStatus.COMPLETED));
    }

    @Test
    void deleteProject_Success() {
        projectService.deleteProject(projectId);

        verify(projectRepository).deleteById(projectId);
    }

    @Test
    void calculateAndUpdateProgress_WithApprovedMilestones() {
        ProjectMilestone m1 = new ProjectMilestone();
        m1.setStatus(MilestoneStatus.APPROVED);
        ProjectMilestone m2 = new ProjectMilestone();
        m2.setStatus(MilestoneStatus.PENDING);
        project.setMilestones(List.of(m1, m2));

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        projectService.calculateAndUpdateProgress(projectId);

        verify(projectRepository).save(argThat(p -> p.getProgress() == 50));
    }

    @Test
    void calculateAndUpdateProgress_NoMilestones_ProgressIsZero() {
        project.setMilestones(new ArrayList<>());

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        projectService.calculateAndUpdateProgress(projectId);

        verify(projectRepository).save(argThat(p -> p.getProgress() == 0));
    }

    @Test
    void calculateAndUpdateProgress_ProjectNotFound_NoAction() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        projectService.calculateAndUpdateProgress(projectId);

        verify(projectRepository, never()).save(any(Project.class));
    }
}
