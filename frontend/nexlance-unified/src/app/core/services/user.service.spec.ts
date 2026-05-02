import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { UserService } from './user.service';
import { User, UserRole, UserType, UserStatus, SubscriptionType } from '../../shared/models/user.model';
import { environment } from '../../../environments/environment';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;

  const apiUrl = `${environment.apiUrl}/users`;

  const mockUser: User = {
    id: 'user-1',
    email: 'test@example.com',
    firstName: 'John',
    lastName: 'Doe',
    role: UserRole.CLIENT,
    status: UserStatus.ACTIVE,
    emailVerified: true,
    subscriptionType: SubscriptionType.FREE
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [UserService]
    });

    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getUserById', () => {
    it('should GET user by ID', () => {
      service.getUserById('user-1').subscribe(user => {
        expect(user.id).toBe('user-1');
        expect(user.email).toBe('test@example.com');
      });

      const req = httpMock.expectOne(`${apiUrl}/user-1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockUser);
    });
  });

  describe('getUserByEmail', () => {
    it('should GET user by email', () => {
      service.getUserByEmail('test@example.com').subscribe(user => {
        expect(user.email).toBe('test@example.com');
      });

      const req = httpMock.expectOne(`${apiUrl}/email/test@example.com`);
      expect(req.request.method).toBe('GET');
      req.flush(mockUser);
    });
  });

  describe('getCurrentUser', () => {
    it('should GET current authenticated user', () => {
      service.getCurrentUser().subscribe(user => {
        expect(user.id).toBe('user-1');
      });

      const req = httpMock.expectOne(`${apiUrl}/me`);
      expect(req.request.method).toBe('GET');
      req.flush(mockUser);
    });
  });

  describe('updateUser', () => {
    it('should send PUT request to update user', () => {
      service.updateUser('user-1', { firstName: 'Jane' }).subscribe(user => {
        expect(user.firstName).toBe('Jane');
      });

      const req = httpMock.expectOne(`${apiUrl}/user-1`);
      expect(req.request.method).toBe('PUT');
      req.flush({ ...mockUser, firstName: 'Jane' });
    });
  });

  describe('updateCurrentUser', () => {
    it('should send PUT request to update current user', () => {
      service.updateCurrentUser({ firstName: 'Updated' }).subscribe(user => {
        expect(user.firstName).toBe('Updated');
      });

      const req = httpMock.expectOne(`${apiUrl}/me`);
      expect(req.request.method).toBe('PUT');
      req.flush({ ...mockUser, firstName: 'Updated' });
    });
  });

  describe('uploadAvatar', () => {
    it('should send POST request with FormData', () => {
      const file = new File(['test'], 'avatar.png', { type: 'image/png' });

      service.uploadAvatar(file).subscribe();

      const req = httpMock.expectOne(`${apiUrl}/me/avatar`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body instanceof FormData).toBeTrue();
      req.flush(mockUser);
    });
  });

  describe('deleteUser', () => {
    it('should send DELETE request', () => {
      service.deleteUser('user-1').subscribe();

      const req = httpMock.expectOne(`${apiUrl}/user-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('deleteCurrentAccount', () => {
    it('should send DELETE request to /me', () => {
      service.deleteCurrentAccount().subscribe();

      const req = httpMock.expectOne(`${apiUrl}/me`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('getAllUsers', () => {
    it('should GET all users without filters', () => {
      const paginatedResponse = {
        content: [mockUser],
        totalElements: 1,
        totalPages: 1,
        size: 10,
        number: 0
      };

      service.getAllUsers().subscribe(result => {
        expect(result.content.length).toBe(1);
        expect(result.totalElements).toBe(1);
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('GET');
      req.flush(paginatedResponse);
    });

    it('should GET users with filters', () => {
      const paginatedResponse = {
        content: [mockUser],
        totalElements: 1,
        totalPages: 1,
        size: 10,
        number: 0
      };

      service.getAllUsers({ type: UserType.CLIENT, page: 0, size: 10 }).subscribe(result => {
        expect(result.content.length).toBe(1);
      });

      const req = httpMock.expectOne(r => r.url === apiUrl && r.params.has('type'));
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('type')).toBe('CLIENT');
      req.flush(paginatedResponse);
    });
  });

  describe('getUsersByType', () => {
    it('should GET users by type', () => {
      const paginatedResponse = {
        content: [mockUser],
        totalElements: 1,
        totalPages: 1,
        size: 100,
        number: 0
      };

      service.getUsersByType(UserType.CLIENT).subscribe(result => {
        expect(result.content.length).toBe(1);
      });

      const req = httpMock.expectOne(r => r.url === `${apiUrl}/type/CLIENT`);
      expect(req.request.method).toBe('GET');
      req.flush(paginatedResponse);
    });
  });
});
