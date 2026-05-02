import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ComplaintService } from './complaint.service';
import { Complaint, ComplaintStatus, ComplaintPriority, ComplaintCategory, ResolutionType } from '../models/complaint.model';
import { environment } from '../../../environments/environment';

describe('ComplaintService', () => {
  let service: ComplaintService;
  let httpMock: HttpTestingController;

  const baseUrl = `${environment.complaintsApiUrl}/complaints`;
  const msgUrl = `${environment.complaintsApiUrl}/support-messages`;

  const mockComplaint: Complaint = {
    id: 'complaint-1',
    ticketNumber: 'TKT-001',
    complainantId: 'user-1',
    complainantEmail: 'user@test.com',
    subject: 'Payment Issue',
    description: 'Payment not received',
    category: ComplaintCategory.PAYMENT_ISSUE,
    status: ComplaintStatus.OPEN,
    priority: ComplaintPriority.HIGH,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  } as any;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ComplaintService]
    });

    service = TestBed.inject(ComplaintService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('createComplaint', () => {
    it('should send POST request to create a complaint', () => {
      const request = {
        complainantId: 'user-1',
        complainantEmail: 'user@test.com',
        subject: 'Payment Issue',
        description: 'Payment not received',
        category: ComplaintCategory.PAYMENT_ISSUE,
        priority: ComplaintPriority.HIGH
      };

      service.createComplaint(request as any).subscribe(result => {
        expect(result.id).toBe('complaint-1');
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      req.flush(mockComplaint);
    });
  });

  describe('getMyComplaints', () => {
    it('should GET user complaints', () => {
      service.getMyComplaints().subscribe(complaints => {
        expect(complaints.length).toBe(1);
      });

      const req = httpMock.expectOne(`${baseUrl}/my-complaints`);
      expect(req.request.method).toBe('GET');
      req.flush([mockComplaint]);
    });
  });

  describe('getComplaintById', () => {
    it('should GET complaint by ID', () => {
      service.getComplaintById('complaint-1').subscribe(complaint => {
        expect(complaint.id).toBe('complaint-1');
      });

      const req = httpMock.expectOne(`${baseUrl}/complaint-1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockComplaint);
    });
  });

  describe('rateComplaint', () => {
    it('should send PUT request with rating param', () => {
      service.rateComplaint('complaint-1', 5).subscribe();

      const req = httpMock.expectOne(r => r.url === `${baseUrl}/complaint-1/rate`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.params.get('rating')).toBe('5');
      req.flush(mockComplaint);
    });
  });

  describe('Agent methods', () => {
    it('getAgentAvailableQueue should GET available queue', () => {
      service.getAgentAvailableQueue().subscribe(complaints => {
        expect(complaints.length).toBe(1);
      });

      const req = httpMock.expectOne(`${baseUrl}/agent/available`);
      expect(req.request.method).toBe('GET');
      req.flush([mockComplaint]);
    });

    it('getMyAssignedComplaints should GET assigned complaints', () => {
      service.getMyAssignedComplaints().subscribe(complaints => {
        expect(complaints.length).toBe(1);
      });

      const req = httpMock.expectOne(`${baseUrl}/agent/my-assigned`);
      expect(req.request.method).toBe('GET');
      req.flush([mockComplaint]);
    });

    it('takeComplaint should send PUT request', () => {
      service.takeComplaint('complaint-1').subscribe();

      const req = httpMock.expectOne(`${baseUrl}/complaint-1/take`);
      expect(req.request.method).toBe('PUT');
      req.flush(mockComplaint);
    });

    it('assignComplaint should send PUT with agentId param', () => {
      service.assignComplaint('complaint-1', 'agent-1').subscribe();

      const req = httpMock.expectOne(r => r.url === `${baseUrl}/complaint-1/assign`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.params.get('agentId')).toBe('agent-1');
      req.flush(mockComplaint);
    });
  });

  describe('updateStatus', () => {
    it('should send PUT request to update complaint status', () => {
      service.updateStatus('complaint-1', ComplaintStatus.IN_PROGRESS).subscribe();

      const req = httpMock.expectOne(r => r.url === `${baseUrl}/complaint-1/status`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.params.get('status')).toBe('IN_PROGRESS');
      req.flush(mockComplaint);
    });
  });

  describe('updatePriority', () => {
    it('should send PUT request to update complaint priority', () => {
      service.updatePriority('complaint-1', ComplaintPriority.CRITICAL).subscribe();

      const req = httpMock.expectOne(r => r.url === `${baseUrl}/complaint-1/priority`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.params.get('priority')).toBe('CRITICAL');
      req.flush(mockComplaint);
    });
  });

  describe('resolveComplaint', () => {
    it('should send PUT request with resolution params', () => {
      service.resolveComplaint('complaint-1', {
        resolution: 'Issue has been resolved',
        resolutionType: ResolutionType.REFUND
      }).subscribe();

      const req = httpMock.expectOne(r => r.url === `${baseUrl}/complaint-1/resolve`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.params.get('resolution')).toBe('Issue has been resolved');
      expect(req.request.params.get('resolutionType')).toBe('REFUND');
      req.flush(mockComplaint);
    });
  });

  describe('Admin methods', () => {
    it('getAllComplaints should GET all complaints', () => {
      service.getAllComplaints().subscribe(complaints => {
        expect(complaints.length).toBe(2);
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('GET');
      req.flush([mockComplaint, { ...mockComplaint, id: 'complaint-2' }]);
    });

    it('getAdminQueue should GET admin queue', () => {
      service.getAdminQueue().subscribe();

      const req = httpMock.expectOne(`${baseUrl}/admin/queue`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });

    it('closeComplaint should send PUT request', () => {
      service.closeComplaint('complaint-1').subscribe();

      const req = httpMock.expectOne(`${baseUrl}/complaint-1/close`);
      expect(req.request.method).toBe('PUT');
      req.flush(mockComplaint);
    });

    it('deleteComplaint should send DELETE request', () => {
      service.deleteComplaint('complaint-1').subscribe();

      const req = httpMock.expectOne(`${baseUrl}/complaint-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('Statistics', () => {
    it('getStatsByStatus should GET stats', () => {
      const stats = { OPEN: 5, IN_PROGRESS: 3, RESOLVED: 10 };

      service.getStatsByStatus().subscribe(result => {
        expect(result['OPEN']).toBe(5);
      });

      const req = httpMock.expectOne(`${baseUrl}/statistics/by-status`);
      expect(req.request.method).toBe('GET');
      req.flush(stats);
    });

    it('getStatsByPriority should GET stats', () => {
      const stats = { LOW: 2, MEDIUM: 5, HIGH: 3 };

      service.getStatsByPriority().subscribe(result => {
        expect(result['HIGH']).toBe(3);
      });

      const req = httpMock.expectOne(`${baseUrl}/statistics/by-priority`);
      expect(req.request.method).toBe('GET');
      req.flush(stats);
    });
  });

  describe('getComplaintByTicketNumber', () => {
    it('should GET complaint by ticket number', () => {
      service.getComplaintByTicketNumber('TKT-001').subscribe(complaint => {
        expect(complaint.ticketNumber).toBe('TKT-001');
      });

      const req = httpMock.expectOne(`${baseUrl}/ticket/TKT-001`);
      expect(req.request.method).toBe('GET');
      req.flush(mockComplaint);
    });
  });

  describe('getOverdueComplaints', () => {
    it('should GET overdue complaints with threshold', () => {
      service.getOverdueComplaints(7).subscribe();

      const req = httpMock.expectOne(r => r.url === `${baseUrl}/overdue`);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('daysThreshold')).toBe('7');
      req.flush([]);
    });
  });

  describe('checkUserByEmail', () => {
    it('should GET user by email', () => {
      service.checkUserByEmail('user@test.com').subscribe(result => {
        expect(result.email).toBe('user@test.com');
      });

      const req = httpMock.expectOne(`${environment.userApiUrl}/users/email/user%40test.com`);
      expect(req.request.method).toBe('GET');
      req.flush({ id: 'u1', email: 'user@test.com', firstName: 'John', lastName: 'Doe', type: 'CLIENT' });
    });
  });
});
