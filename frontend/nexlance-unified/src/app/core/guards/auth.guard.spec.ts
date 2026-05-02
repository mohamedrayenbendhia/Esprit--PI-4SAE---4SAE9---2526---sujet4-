import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('authGuard', () => {
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockRoute = {} as ActivatedRouteSnapshot;
  const mockState = { url: '/protected-route' } as RouterStateSnapshot;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['isAuthenticated']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });
  });

  it('should allow access when user is authenticated', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);

    TestBed.runInInjectionContext(() => {
      const result = authGuard(mockRoute, mockState);
      expect(result).toBeTrue();
    });
  });

  it('should deny access and redirect to login when user is not authenticated', () => {
    authServiceSpy.isAuthenticated.and.returnValue(false);

    TestBed.runInInjectionContext(() => {
      const result = authGuard(mockRoute, mockState);
      expect(result).toBeFalse();
      expect(routerSpy.navigate).toHaveBeenCalledWith(
        ['/login'],
        { queryParams: { returnUrl: '/protected-route' } }
      );
    });
  });

  it('should pass the current URL as returnUrl parameter', () => {
    authServiceSpy.isAuthenticated.and.returnValue(false);
    const stateWithUrl = { url: '/backoffice/admin/dashboard' } as RouterStateSnapshot;

    TestBed.runInInjectionContext(() => {
      authGuard(mockRoute, stateWithUrl);
      expect(routerSpy.navigate).toHaveBeenCalledWith(
        ['/login'],
        { queryParams: { returnUrl: '/backoffice/admin/dashboard' } }
      );
    });
  });
});
