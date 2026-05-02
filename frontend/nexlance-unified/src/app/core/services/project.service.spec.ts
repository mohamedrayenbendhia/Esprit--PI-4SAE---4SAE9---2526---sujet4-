import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ProjectService } from './project.service';
import { Project, ProjectStatus, ProjectMilestone, MilestoneStatus } from '../models/project.model';
import { environment } from '../../../environments/environment';

describe('ProjectService', () => {
  let service: ProjectService;
  let httpMock: HttpTestingController;

  const projectsUrl = `${environment.projectsApiUrl}/projects`;
  const milestonesUrl = `${environment.projectsApiUrl}/milestones`;

  const mockProject: Project = {
    id: 'proj-1',
    jobOfferId: 'job-1',
    clientId: 'client-1',
    freelanceId: 'freelance-1',
    title: 'Web App Project',
    description: 'Building a web app',
    status: ProjectStatus.ACTIVE,
    budget: 5000,
    progress: 50,
    startDate: new Date().toISOString(),
    endDate: new Date().toISOString(),
    milestones: [],
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  } as any;

  const mockMilestone: ProjectMilestone = {
    id: 'ms-1',
    projectId: 'proj-1',
    title: 'Phase 1',
    description: 'Initial setup',
    status: MilestoneStatus.IN_PROGRESS,
    amount: 1000,
    dueDate: new Date().toISOString()
  } as any;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ProjectService]
    });

    service = TestBed.inject(ProjectService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('Project CRUD', () => {
    it('createProject should send POST request', () => {
      const createReq = { title: 'Web App', description: 'test', clientId: 'c1', freelanceId: 'f1', jobOfferId: 'j1', budget: 5000 };

      service.createProject(createReq as any).subscribe(result => {
        expect(result.id).toBe('proj-1');
      });

      const req = httpMock.expectOne(projectsUrl);
      expect(req.request.method).toBe('POST');
      req.flush(mockProject);
    });

    it('getAllProjects should GET all projects', () => {
      service.getAllProjects().subscribe(projects => {
        expect(projects.length).toBe(1);
      });

      const req = httpMock.expectOne(projectsUrl);
      expect(req.request.method).toBe('GET');
      req.flush([mockProject]);
    });

    it('getProjectById should GET project by ID', () => {
      service.getProjectById('proj-1').subscribe(project => {
        expect(project.id).toBe('proj-1');
      });

      const req = httpMock.expectOne(`${projectsUrl}/proj-1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockProject);
    });

    it('getProjectsByClientId should GET projects for client', () => {
      service.getProjectsByClientId('client-1').subscribe(projects => {
        expect(projects.length).toBe(1);
      });

      const req = httpMock.expectOne(`${projectsUrl}/client/client-1`);
      expect(req.request.method).toBe('GET');
      req.flush([mockProject]);
    });

    it('getProjectsByFreelanceId should GET projects for freelance', () => {
      service.getProjectsByFreelanceId('freelance-1').subscribe(projects => {
        expect(projects.length).toBe(1);
      });

      const req = httpMock.expectOne(`${projectsUrl}/freelance/freelance-1`);
      expect(req.request.method).toBe('GET');
      req.flush([mockProject]);
    });

    it('getProjectsByJobOfferId should GET projects by job offer', () => {
      service.getProjectsByJobOfferId('job-1').subscribe(projects => {
        expect(projects.length).toBe(1);
      });

      const req = httpMock.expectOne(`${projectsUrl}/job-offer/job-1`);
      expect(req.request.method).toBe('GET');
      req.flush([mockProject]);
    });

    it('getProjectsByStatus should GET projects by status', () => {
      service.getProjectsByStatus(ProjectStatus.ACTIVE).subscribe(projects => {
        expect(projects.length).toBe(1);
      });

      const req = httpMock.expectOne(`${projectsUrl}/status/ACTIVE`);
      expect(req.request.method).toBe('GET');
      req.flush([mockProject]);
    });

    it('updateProject should send PUT request', () => {
      service.updateProject('proj-1', { title: 'Updated' }).subscribe(project => {
        expect(project.title).toBe('Updated');
      });

      const req = httpMock.expectOne(`${projectsUrl}/proj-1`);
      expect(req.request.method).toBe('PUT');
      req.flush({ ...mockProject, title: 'Updated' });
    });

    it('updateProjectStatus should send PATCH request', () => {
      service.updateProjectStatus('proj-1', ProjectStatus.COMPLETED).subscribe();

      const req = httpMock.expectOne(r => r.url === `${projectsUrl}/proj-1/status`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.params.get('status')).toBe('COMPLETED');
      req.flush(mockProject);
    });

    it('deleteProject should send DELETE request', () => {
      service.deleteProject('proj-1').subscribe();

      const req = httpMock.expectOne(`${projectsUrl}/proj-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('Milestone CRUD', () => {
    it('createMilestone should send POST request with projectId', () => {
      const msData = { title: 'Phase 1', description: 'Init', amount: 1000, dueDate: new Date() };

      service.createMilestone('proj-1', msData as any).subscribe(result => {
        expect(result.id).toBe('ms-1');
      });

      const req = httpMock.expectOne(milestonesUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body.projectId).toBe('proj-1');
      req.flush(mockMilestone);
    });

    it('getAllMilestones should GET all milestones', () => {
      service.getAllMilestones().subscribe(milestones => {
        expect(milestones.length).toBe(1);
      });

      const req = httpMock.expectOne(milestonesUrl);
      expect(req.request.method).toBe('GET');
      req.flush([mockMilestone]);
    });

    it('getMilestoneById should GET milestone by ID', () => {
      service.getMilestoneById('ms-1').subscribe(ms => {
        expect(ms.id).toBe('ms-1');
      });

      const req = httpMock.expectOne(`${milestonesUrl}/ms-1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockMilestone);
    });

    it('getMilestonesByProjectId should GET milestones for project', () => {
      service.getMilestonesByProjectId('proj-1').subscribe(milestones => {
        expect(milestones.length).toBe(1);
      });

      const req = httpMock.expectOne(`${milestonesUrl}/project/proj-1`);
      expect(req.request.method).toBe('GET');
      req.flush([mockMilestone]);
    });

    it('getMilestonesByStatus should GET milestones by status', () => {
      service.getMilestonesByStatus(MilestoneStatus.IN_PROGRESS).subscribe(milestones => {
        expect(milestones.length).toBe(1);
      });

      const req = httpMock.expectOne(`${milestonesUrl}/status/IN_PROGRESS`);
      expect(req.request.method).toBe('GET');
      req.flush([mockMilestone]);
    });
  });
});
