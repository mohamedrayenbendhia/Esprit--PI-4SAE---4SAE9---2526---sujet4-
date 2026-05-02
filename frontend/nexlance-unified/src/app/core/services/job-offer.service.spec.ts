import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { JobOfferService } from './job-offer.service';
import { AuthService } from './auth.service';
import { JobOffer, JobCategory, BudgetType, JobOfferStatus, ExperienceLevel, CreateJobOfferDto } from '../models/job-offer.model';
import { environment } from '../../../environments/environment';

describe('JobOfferService', () => {
  let service: JobOfferService;
  let httpMock: HttpTestingController;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  const mockJobOffer: JobOffer = {
    id: 'job-1',
    clientId: 'client-1',
    title: 'Angular Developer Needed',
    description: 'Build a web app',
    category: JobCategory.DEVELOPMENT,
    budget: 5000,
    budgetType: BudgetType.FIXED,
    estimatedDuration: 30,
    deadline: new Date('2026-06-01'),
    status: JobOfferStatus.OPEN,
    requiredSkills: ['Angular', 'TypeScript'],
    experienceLevel: ExperienceLevel.INTERMEDIATE,
    location: 'Remote',
    isRemote: true,
    attachments: [],
    viewCount: 10,
    applicantCount: 3,
    createdAt: new Date(),
    updatedAt: new Date()
  };

  const apiUrl = `${environment.jobOffersApiUrl}/job-offers`;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['getCurrentUser']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        JobOfferService,
        { provide: AuthService, useValue: authServiceSpy }
      ]
    });

    service = TestBed.inject(JobOfferService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('createJobOffer', () => {
    it('should send POST request to create a job offer', () => {
      const createDto: CreateJobOfferDto = {
        clientId: 'client-1',
        title: 'Angular Developer Needed',
        description: 'Build a web app',
        category: JobCategory.DEVELOPMENT,
        budget: 5000,
        budgetType: BudgetType.FIXED,
        estimatedDuration: 30,
        deadline: new Date('2026-06-01'),
        requiredSkills: ['Angular', 'TypeScript'],
        experienceLevel: ExperienceLevel.INTERMEDIATE,
        location: 'Remote',
        isRemote: true,
        status: JobOfferStatus.DRAFT
      };

      service.createJobOffer(createDto).subscribe(result => {
        expect(result).toEqual(mockJobOffer);
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('POST');
      req.flush(mockJobOffer);
    });
  });

  describe('getAllJobOffers', () => {
    it('should GET all job offers without filters', () => {
      service.getAllJobOffers().subscribe(offers => {
        expect(offers.length).toBe(2);
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('GET');
      req.flush([mockJobOffer, { ...mockJobOffer, id: 'job-2' }]);
    });

    it('should GET job offers by status when filter is provided', () => {
      service.getAllJobOffers({ status: JobOfferStatus.OPEN }).subscribe(offers => {
        expect(offers.length).toBe(1);
      });

      const req = httpMock.expectOne(`${apiUrl}/status/OPEN`);
      expect(req.request.method).toBe('GET');
      req.flush([mockJobOffer]);
    });
  });

  describe('getActiveJobOffers', () => {
    it('should GET active job offers', () => {
      service.getActiveJobOffers().subscribe(offers => {
        expect(offers.length).toBe(1);
      });

      const req = httpMock.expectOne(`${apiUrl}/active`);
      expect(req.request.method).toBe('GET');
      req.flush([mockJobOffer]);
    });
  });

  describe('getJobOffersByCategory', () => {
    it('should GET job offers by category', () => {
      service.getJobOffersByCategory('DEVELOPMENT').subscribe(offers => {
        expect(offers.length).toBe(1);
      });

      const req = httpMock.expectOne(`${apiUrl}/category/DEVELOPMENT`);
      expect(req.request.method).toBe('GET');
      req.flush([mockJobOffer]);
    });
  });

  describe('getRemoteJobOffers', () => {
    it('should GET remote job offers', () => {
      service.getRemoteJobOffers().subscribe(offers => {
        expect(offers.length).toBe(1);
      });

      const req = httpMock.expectOne(`${apiUrl}/remote`);
      expect(req.request.method).toBe('GET');
      req.flush([mockJobOffer]);
    });
  });

  describe('getJobOfferById', () => {
    it('should GET a single job offer by ID', () => {
      service.getJobOfferById('job-1').subscribe(offer => {
        expect(offer.id).toBe('job-1');
        expect(offer.title).toBe('Angular Developer Needed');
      });

      const req = httpMock.expectOne(`${apiUrl}/job-1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockJobOffer);
    });
  });

  describe('updateJobOffer', () => {
    it('should send PUT request to update a job offer', () => {
      const updateDto = { title: 'Updated Title' };

      service.updateJobOffer('job-1', updateDto).subscribe(offer => {
        expect(offer.title).toBe('Updated Title');
      });

      const req = httpMock.expectOne(`${apiUrl}/job-1`);
      expect(req.request.method).toBe('PUT');
      req.flush({ ...mockJobOffer, title: 'Updated Title' });
    });
  });

  describe('deleteJobOffer', () => {
    it('should send DELETE request', () => {
      service.deleteJobOffer('job-1').subscribe();

      const req = httpMock.expectOne(`${apiUrl}/job-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('changeStatus', () => {
    it('should send PATCH request to change status', () => {
      service.changeStatus('job-1', 'OPEN').subscribe(offer => {
        expect(offer.status).toBe(JobOfferStatus.OPEN);
      });

      const req = httpMock.expectOne(`${apiUrl}/job-1/status?status=OPEN`);
      expect(req.request.method).toBe('PATCH');
      req.flush({ ...mockJobOffer, status: JobOfferStatus.OPEN });
    });
  });

  describe('publishJobOffer', () => {
    it('should change status to OPEN', () => {
      service.publishJobOffer('job-1').subscribe();

      const req = httpMock.expectOne(`${apiUrl}/job-1/status?status=OPEN`);
      expect(req.request.method).toBe('PATCH');
      req.flush(mockJobOffer);
    });
  });

  describe('archiveJobOffer', () => {
    it('should send PATCH request to archive', () => {
      service.archiveJobOffer('job-1').subscribe();

      const req = httpMock.expectOne(`${apiUrl}/job-1/archive`);
      expect(req.request.method).toBe('PATCH');
      req.flush({ ...mockJobOffer, status: JobOfferStatus.ARCHIVED });
    });
  });

  describe('getClientJobOffers', () => {
    it('should GET job offers for a specific client', () => {
      service.getClientJobOffers('client-1').subscribe(offers => {
        expect(offers.length).toBe(1);
      });

      const req = httpMock.expectOne(`${apiUrl}/client/client-1`);
      expect(req.request.method).toBe('GET');
      req.flush([mockJobOffer]);
    });
  });

  describe('getMyJobOffers', () => {
    it('should GET current user job offers when user is authenticated', () => {
      authServiceSpy.getCurrentUser.and.returnValue({
        id: 'client-1',
        email: 'test@test.com',
        firstName: 'Test',
        lastName: 'User',
        role: 'CLIENT' as any,
        status: 'ACTIVE' as any,
        emailVerified: true
      });

      service.getMyJobOffers().subscribe(offers => {
        expect(offers.length).toBe(1);
      });

      const req = httpMock.expectOne(`${apiUrl}/client/client-1`);
      expect(req.request.method).toBe('GET');
      req.flush([mockJobOffer]);
    });
  });

  describe('getJobOffersByExperienceLevel', () => {
    it('should GET job offers by experience level', () => {
      service.getJobOffersByExperienceLevel('INTERMEDIATE').subscribe(offers => {
        expect(offers.length).toBe(1);
      });

      const req = httpMock.expectOne(`${apiUrl}/experience-level/INTERMEDIATE`);
      expect(req.request.method).toBe('GET');
      req.flush([mockJobOffer]);
    });
  });
});
