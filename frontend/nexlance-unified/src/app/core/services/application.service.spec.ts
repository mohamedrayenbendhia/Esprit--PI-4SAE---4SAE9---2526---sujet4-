import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ApplicationService } from './application.service';
import { Application, ApplicationStatus, CreateApplicationDto } from '../models/application.model';
import { environment } from '../../../environments/environment';

describe('ApplicationService', () => {
  let service: ApplicationService;
  let httpMock: HttpTestingController;

  const apiUrl = `${environment.jobOffersApiUrl}/applications`;

  const mockApplication: Application = {
    id: 'app-1',
    jobOfferId: 'job-1',
    freelanceId: 'freelance-1',
    coverLetter: 'I am experienced in Angular...',
    proposedRate: 50,
    estimatedDelivery: new Date('2026-07-01'),
    status: ApplicationStatus.PENDING,
    portfolioItems: [],
    availableFrom: new Date('2026-05-01'),
    isRead: false,
    submittedAt: new Date(),
    createdAt: new Date()
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ApplicationService]
    });

    service = TestBed.inject(ApplicationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('createApplication', () => {
    it('should send POST request to create an application', () => {
      const createDto: CreateApplicationDto = {
        jobOfferId: 'job-1',
        freelanceId: 'freelance-1',
        coverLetter: 'I am experienced in Angular...',
        proposedRate: 50,
        estimatedDelivery: new Date('2026-07-01'),
        portfolioItems: [],
        availableFrom: new Date('2026-05-01')
      };

      service.createApplication(createDto).subscribe(result => {
        expect(result.id).toBe('app-1');
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('POST');
      req.flush(mockApplication);
    });
  });

  describe('getAllApplications', () => {
    it('should GET all applications without filters', () => {
      service.getAllApplications().subscribe(apps => {
        expect(apps.length).toBe(1);
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('GET');
      req.flush([mockApplication]);
    });

    it('should GET applications by status when filter is provided', () => {
      service.getAllApplications({ status: ApplicationStatus.PENDING }).subscribe(apps => {
        expect(apps.length).toBe(1);
      });

      const req = httpMock.expectOne(`${apiUrl}/status/PENDING`);
      expect(req.request.method).toBe('GET');
      req.flush([mockApplication]);
    });
  });

  describe('getApplicationById', () => {
    it('should GET a single application by ID', () => {
      service.getApplicationById('app-1').subscribe(app => {
        expect(app.id).toBe('app-1');
        expect(app.freelanceId).toBe('freelance-1');
      });

      const req = httpMock.expectOne(`${apiUrl}/app-1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockApplication);
    });
  });

  describe('updateApplication', () => {
    it('should send PUT request', () => {
      const updateDto = { coverLetter: 'Updated letter' };

      service.updateApplication('app-1', updateDto).subscribe(app => {
        expect(app.coverLetter).toBe('Updated letter');
      });

      const req = httpMock.expectOne(`${apiUrl}/app-1`);
      expect(req.request.method).toBe('PUT');
      req.flush({ ...mockApplication, coverLetter: 'Updated letter' });
    });
  });

  describe('deleteApplication', () => {
    it('should send DELETE request', () => {
      service.deleteApplication('app-1').subscribe();

      const req = httpMock.expectOne(`${apiUrl}/app-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('changeStatus', () => {
    it('should send PATCH request to change application status', () => {
      service.changeStatus('app-1', ApplicationStatus.SHORTLISTED).subscribe(app => {
        expect(app.status).toBe(ApplicationStatus.SHORTLISTED);
      });

      const req = httpMock.expectOne(`${apiUrl}/app-1/status?status=SHORTLISTED`);
      expect(req.request.method).toBe('PATCH');
      req.flush({ ...mockApplication, status: ApplicationStatus.SHORTLISTED });
    });
  });

  describe('markAsRead', () => {
    it('should send PATCH request to mark application as read', () => {
      service.markAsRead('app-1').subscribe(app => {
        expect(app.isRead).toBeTrue();
      });

      const req = httpMock.expectOne(`${apiUrl}/app-1/read`);
      expect(req.request.method).toBe('PATCH');
      req.flush({ ...mockApplication, isRead: true });
    });
  });

  describe('getApplicationsByJobOffer', () => {
    it('should GET applications for a specific job offer', () => {
      service.getApplicationsByJobOffer('job-1').subscribe(apps => {
        expect(apps.length).toBe(1);
      });

      const req = httpMock.expectOne(`${apiUrl}/job-offer/job-1`);
      expect(req.request.method).toBe('GET');
      req.flush([mockApplication]);
    });
  });

  describe('getUnreadApplicationsByJobOffer', () => {
    it('should GET unread applications', () => {
      service.getUnreadApplicationsByJobOffer('job-1').subscribe(apps => {
        expect(apps.length).toBe(1);
      });

      const req = httpMock.expectOne(`${apiUrl}/job-offer/job-1/unread`);
      expect(req.request.method).toBe('GET');
      req.flush([mockApplication]);
    });
  });

  describe('getApplicationCount', () => {
    it('should GET the count of applications for a job offer', () => {
      service.getApplicationCount('job-1').subscribe(count => {
        expect(count).toBe(5);
      });

      const req = httpMock.expectOne(`${apiUrl}/job-offer/job-1/count`);
      expect(req.request.method).toBe('GET');
      req.flush(5);
    });
  });

  describe('shortlistApplication', () => {
    it('should change status to SHORTLISTED', () => {
      service.shortlistApplication('app-1').subscribe();

      const req = httpMock.expectOne(`${apiUrl}/app-1/status?status=SHORTLISTED`);
      expect(req.request.method).toBe('PATCH');
      req.flush({ ...mockApplication, status: ApplicationStatus.SHORTLISTED });
    });
  });

  describe('acceptApplication', () => {
    it('should change status to ACCEPTED', () => {
      service.acceptApplication('app-1').subscribe();

      const req = httpMock.expectOne(`${apiUrl}/app-1/status?status=ACCEPTED`);
      expect(req.request.method).toBe('PATCH');
      req.flush({ ...mockApplication, status: ApplicationStatus.ACCEPTED });
    });
  });

  describe('rejectApplication', () => {
    it('should change status to REJECTED', () => {
      service.rejectApplication('app-1').subscribe();

      const req = httpMock.expectOne(`${apiUrl}/app-1/status?status=REJECTED`);
      expect(req.request.method).toBe('PATCH');
      req.flush({ ...mockApplication, status: ApplicationStatus.REJECTED });
    });
  });

  describe('withdrawApplication', () => {
    it('should send PATCH request to withdraw', () => {
      service.withdrawApplication('app-1').subscribe();

      const req = httpMock.expectOne(`${apiUrl}/app-1/withdraw`);
      expect(req.request.method).toBe('PATCH');
      req.flush({ ...mockApplication, status: ApplicationStatus.WITHDRAWN });
    });
  });
});
