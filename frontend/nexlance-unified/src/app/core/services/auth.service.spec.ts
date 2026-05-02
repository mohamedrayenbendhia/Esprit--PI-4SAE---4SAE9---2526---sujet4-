import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { User, UserRole, UserType, UserStatus, SubscriptionType, AuthResponse } from '../../shared/models/user.model';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockAuthResponse: AuthResponse = {
    userId: 'user-123',
    email: 'test@example.com',
    firstName: 'John',
    lastName: 'Doe',
    role: UserRole.CLIENT,
    status: 'ACTIVE',
    emailVerified: true,
    token: 'fake-jwt-token'
  };

  beforeEach(() => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthService,
        { provide: Router, useValue: routerSpy }
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('login', () => {
    it('should send POST request to login endpoint', () => {
      const credentials = { email: 'test@example.com', password: 'password123' };

      service.login(credentials).subscribe(response => {
        expect(response).toEqual(mockAuthResponse);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(credentials);
      req.flush(mockAuthResponse);
    });

    it('should store token in localStorage after login', () => {
      const credentials = { email: 'test@example.com', password: 'password123' };

      service.login(credentials).subscribe(() => {
        expect(localStorage.getItem('auth_token')).toBe('fake-jwt-token');
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      req.flush(mockAuthResponse);
    });

    it('should store user in localStorage after login', () => {
      const credentials = { email: 'test@example.com', password: 'password123' };

      service.login(credentials).subscribe(() => {
        const storedUser = JSON.parse(localStorage.getItem('current_user')!);
        expect(storedUser.email).toBe('test@example.com');
        expect(storedUser.firstName).toBe('John');
        expect(storedUser.role).toBe(UserRole.CLIENT);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      req.flush(mockAuthResponse);
    });

    it('should update currentUser$ observable after login', () => {
      const credentials = { email: 'test@example.com', password: 'password123' };

      service.login(credentials).subscribe(() => {
        service.currentUser$.subscribe(user => {
          expect(user).toBeTruthy();
          expect(user!.email).toBe('test@example.com');
        });
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      req.flush(mockAuthResponse);
    });
  });

  describe('registerClient', () => {
    it('should send POST request with CLIENT role', () => {
      const data = {
        email: 'client@test.com',
        password: 'pass123',
        confirmPassword: 'pass123',
        role: UserRole.CLIENT,
        firstName: 'Jane',
        lastName: 'Doe'
      };

      service.registerClient(data).subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/register`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body.role).toBe(UserRole.CLIENT);
      req.flush(mockAuthResponse);
    });
  });

  describe('registerFreelancer', () => {
    it('should send POST request with FREELANCER role', () => {
      const data = {
        email: 'freelancer@test.com',
        password: 'pass123',
        confirmPassword: 'pass123',
        role: UserRole.FREELANCER,
        firstName: 'Bob',
        lastName: 'Smith'
      };

      service.registerFreelancer(data).subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/register`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body.role).toBe(UserRole.FREELANCER);
      req.flush(mockAuthResponse);
    });
  });

  describe('logout', () => {
    it('should clear localStorage', () => {
      localStorage.setItem('auth_token', 'some-token');
      localStorage.setItem('current_user', '{}');

      service.logout();

      expect(localStorage.getItem('auth_token')).toBeNull();
      expect(localStorage.getItem('current_user')).toBeNull();
    });

    it('should set currentUser$ to null', () => {
      service.logout();

      service.currentUser$.subscribe(user => {
        expect(user).toBeNull();
      });
    });

    it('should navigate to login page', () => {
      service.logout();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
    });
  });

  describe('getCurrentUser', () => {
    it('should return null when no user is stored', () => {
      expect(service.getCurrentUser()).toBeNull();
    });

    it('should return current user after login', () => {
      const credentials = { email: 'test@example.com', password: 'pass' };

      service.login(credentials).subscribe(() => {
        const user = service.getCurrentUser();
        expect(user).toBeTruthy();
        expect(user!.email).toBe('test@example.com');
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      req.flush(mockAuthResponse);
    });
  });

  describe('isAuthenticated', () => {
    it('should return false when no token exists', () => {
      expect(service.isAuthenticated()).toBeFalse();
    });

    it('should return true when token exists', () => {
      localStorage.setItem('auth_token', 'some-token');
      expect(service.isAuthenticated()).toBeTrue();
    });
  });

  describe('getToken', () => {
    it('should return null when no token exists', () => {
      expect(service.getToken()).toBeNull();
    });

    it('should return token when it exists', () => {
      localStorage.setItem('auth_token', 'my-token');
      expect(service.getToken()).toBe('my-token');
    });
  });

  describe('hasRole', () => {
    it('should return false when no user is logged in', () => {
      expect(service.hasRole(UserRole.CLIENT)).toBeFalse();
    });

    it('should return true when user has the specified role', () => {
      const credentials = { email: 'test@example.com', password: 'pass' };

      service.login(credentials).subscribe(() => {
        expect(service.hasRole(UserRole.CLIENT)).toBeTrue();
        expect(service.hasRole(UserRole.ADMIN)).toBeFalse();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      req.flush(mockAuthResponse);
    });
  });

  describe('isAdmin / isClient / isFreelancer', () => {
    it('should correctly identify admin role', () => {
      const adminResponse = { ...mockAuthResponse, role: UserRole.ADMIN };
      service.login({ email: 'a@b.com', password: 'p' }).subscribe(() => {
        expect(service.isAdmin()).toBeTrue();
        expect(service.isClient()).toBeFalse();
        expect(service.isFreelancer()).toBeFalse();
      });
      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      req.flush(adminResponse);
    });
  });

  describe('redirectAfterLogin', () => {
    it('should navigate to client dashboard for CLIENT role', () => {
      service.redirectAfterLogin(UserRole.CLIENT);
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/frontoffice/client/dashboard']);
    });

    it('should navigate to freelancer dashboard for FREELANCER role', () => {
      service.redirectAfterLogin(UserRole.FREELANCER);
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/frontoffice/freelancer/dashboard']);
    });

    it('should navigate to admin dashboard for ADMIN role', () => {
      service.redirectAfterLogin(UserRole.ADMIN);
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/backoffice/admin/dashboard']);
    });
  });
});
