import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors, HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

describe('authInterceptor', () => {
  let httpMock: HttpTestingController;
  let httpClient: HttpClient;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['getToken', 'isAuthenticated', 'logout']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should add Authorization header for API calls when token exists', () => {
    authServiceSpy.getToken.and.returnValue('my-token');

    httpClient.get('/api/test').subscribe();

    const req = httpMock.expectOne('/api/test');
    expect(req.request.headers.get('Authorization')).toBe('Bearer my-token');
    req.flush({});
  });

  it('should NOT add Authorization header when no token exists', () => {
    authServiceSpy.getToken.and.returnValue(null);

    httpClient.get('/api/test').subscribe();

    const req = httpMock.expectOne('/api/test');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });

  it('should NOT add Authorization header for non-API calls', () => {
    authServiceSpy.getToken.and.returnValue('my-token');

    httpClient.get('https://api.cloudinary.com/upload').subscribe();

    const req = httpMock.expectOne('https://api.cloudinary.com/upload');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });

  it('should logout and redirect on 401 error when authenticated', () => {
    authServiceSpy.getToken.and.returnValue('expired-token');
    authServiceSpy.isAuthenticated.and.returnValue(true);

    httpClient.get('/api/test').subscribe({
      error: () => {
        expect(authServiceSpy.logout).toHaveBeenCalled();
        expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
      }
    });

    const req = httpMock.expectOne('/api/test');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
  });

  it('should NOT logout on 401 when not authenticated', () => {
    authServiceSpy.getToken.and.returnValue(null);
    authServiceSpy.isAuthenticated.and.returnValue(false);

    httpClient.get('/api/test').subscribe({
      error: () => {
        expect(authServiceSpy.logout).not.toHaveBeenCalled();
      }
    });

    const req = httpMock.expectOne('/api/test');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
  });

  it('should NOT interfere with non-401 errors', () => {
    authServiceSpy.getToken.and.returnValue('my-token');

    httpClient.get('/api/test').subscribe({
      error: (err) => {
        expect(err.status).toBe(500);
        expect(authServiceSpy.logout).not.toHaveBeenCalled();
      }
    });

    const req = httpMock.expectOne('/api/test');
    req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
  });
});
