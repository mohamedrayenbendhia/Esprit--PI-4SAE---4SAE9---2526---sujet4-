import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { roleGuard, clientGuard, freelancerGuard, adminGuard } from './role.guard';
import { AuthService } from '../services/auth.service';
import { UserRole, UserStatus } from '../../shared/models/user.model';

describe('roleGuard', () => {
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockRoute = {} as ActivatedRouteSnapshot;
  const mockState = { url: '/some-route' } as RouterStateSnapshot;

  const createMockUser = (role: UserRole) => ({
    id: 'user-1',
    email: 'test@test.com',
    firstName: 'Test',
    lastName: 'User',
    role: role,
    status: UserStatus.ACTIVE,
    emailVerified: true
  });

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['getCurrentUser']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });
  });

  describe('roleGuard factory', () => {
    it('should allow access when user has an allowed role', () => {
      authServiceSpy.getCurrentUser.and.returnValue(createMockUser(UserRole.ADMIN));
      const guard = roleGuard([UserRole.ADMIN]);

      TestBed.runInInjectionContext(() => {
        const result = guard(mockRoute, mockState);
        expect(result).toBeTrue();
      });
    });

    it('should deny access when user has a non-allowed role', () => {
      authServiceSpy.getCurrentUser.and.returnValue(createMockUser(UserRole.CLIENT));
      const guard = roleGuard([UserRole.ADMIN]);

      TestBed.runInInjectionContext(() => {
        const result = guard(mockRoute, mockState);
        expect(result).toBeFalse();
      });
    });

    it('should redirect to login when no user is logged in', () => {
      authServiceSpy.getCurrentUser.and.returnValue(null);
      const guard = roleGuard([UserRole.ADMIN]);

      TestBed.runInInjectionContext(() => {
        const result = guard(mockRoute, mockState);
        expect(result).toBeFalse();
        expect(routerSpy.navigate).toHaveBeenCalledWith(
          ['/login'],
          { queryParams: { returnUrl: '/some-route' } }
        );
      });
    });

    it('should redirect CLIENT to client dashboard when not authorized', () => {
      authServiceSpy.getCurrentUser.and.returnValue(createMockUser(UserRole.CLIENT));
      const guard = roleGuard([UserRole.ADMIN]);

      TestBed.runInInjectionContext(() => {
        guard(mockRoute, mockState);
        expect(routerSpy.navigate).toHaveBeenCalledWith(['/frontoffice/client/dashboard']);
      });
    });

    it('should redirect FREELANCER to freelancer dashboard when not authorized', () => {
      authServiceSpy.getCurrentUser.and.returnValue(createMockUser(UserRole.FREELANCER));
      const guard = roleGuard([UserRole.ADMIN]);

      TestBed.runInInjectionContext(() => {
        guard(mockRoute, mockState);
        expect(routerSpy.navigate).toHaveBeenCalledWith(['/frontoffice/freelancer/dashboard']);
      });
    });

    it('should redirect ADMIN to admin dashboard when not authorized for specific route', () => {
      authServiceSpy.getCurrentUser.and.returnValue(createMockUser(UserRole.ADMIN));
      const guard = roleGuard([UserRole.CLIENT]);

      TestBed.runInInjectionContext(() => {
        guard(mockRoute, mockState);
        expect(routerSpy.navigate).toHaveBeenCalledWith(['/backoffice/admin/dashboard']);
      });
    });
  });

  describe('clientGuard', () => {
    it('should allow CLIENT access', () => {
      authServiceSpy.getCurrentUser.and.returnValue(createMockUser(UserRole.CLIENT));

      TestBed.runInInjectionContext(() => {
        const result = clientGuard(mockRoute, mockState);
        expect(result).toBeTrue();
      });
    });

    it('should allow ADMIN access', () => {
      authServiceSpy.getCurrentUser.and.returnValue(createMockUser(UserRole.ADMIN));

      TestBed.runInInjectionContext(() => {
        const result = clientGuard(mockRoute, mockState);
        expect(result).toBeTrue();
      });
    });

    it('should deny FREELANCER access', () => {
      authServiceSpy.getCurrentUser.and.returnValue(createMockUser(UserRole.FREELANCER));

      TestBed.runInInjectionContext(() => {
        const result = clientGuard(mockRoute, mockState);
        expect(result).toBeFalse();
      });
    });
  });

  describe('freelancerGuard', () => {
    it('should allow FREELANCER access', () => {
      authServiceSpy.getCurrentUser.and.returnValue(createMockUser(UserRole.FREELANCER));

      TestBed.runInInjectionContext(() => {
        const result = freelancerGuard(mockRoute, mockState);
        expect(result).toBeTrue();
      });
    });

    it('should allow ADMIN access', () => {
      authServiceSpy.getCurrentUser.and.returnValue(createMockUser(UserRole.ADMIN));

      TestBed.runInInjectionContext(() => {
        const result = freelancerGuard(mockRoute, mockState);
        expect(result).toBeTrue();
      });
    });

    it('should deny CLIENT access', () => {
      authServiceSpy.getCurrentUser.and.returnValue(createMockUser(UserRole.CLIENT));

      TestBed.runInInjectionContext(() => {
        const result = freelancerGuard(mockRoute, mockState);
        expect(result).toBeFalse();
      });
    });
  });

  describe('adminGuard', () => {
    it('should allow ADMIN access', () => {
      authServiceSpy.getCurrentUser.and.returnValue(createMockUser(UserRole.ADMIN));

      TestBed.runInInjectionContext(() => {
        const result = adminGuard(mockRoute, mockState);
        expect(result).toBeTrue();
      });
    });

    it('should deny CLIENT access', () => {
      authServiceSpy.getCurrentUser.and.returnValue(createMockUser(UserRole.CLIENT));

      TestBed.runInInjectionContext(() => {
        const result = adminGuard(mockRoute, mockState);
        expect(result).toBeFalse();
      });
    });

    it('should deny FREELANCER access', () => {
      authServiceSpy.getCurrentUser.and.returnValue(createMockUser(UserRole.FREELANCER));

      TestBed.runInInjectionContext(() => {
        const result = adminGuard(mockRoute, mockState);
        expect(result).toBeFalse();
      });
    });
  });
});
