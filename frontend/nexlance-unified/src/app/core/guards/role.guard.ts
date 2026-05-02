import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../../shared/models/user.model';

export const roleGuard = (allowedRoles: UserRole[]): CanActivateFn => {
  return (route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    const user = authService.getCurrentUser();

    if (!user || !user.role) {
      router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
      return false;
    }

    if (allowedRoles.includes(user.role)) {
      return true;
    }

    // Rediriger vers le dashboard approprié selon le rôle
    switch (user.role) {
      case UserRole.CLIENT:
        router.navigate(['/frontoffice/client/dashboard']);
        break;
      case UserRole.FREELANCER:
        router.navigate(['/frontoffice/freelancer/dashboard']);
        break;
      case UserRole.ADMIN:
        router.navigate(['/backoffice/admin/dashboard']);
        break;
      case UserRole.SUPPORT_AGENT:
        router.navigate(['/backoffice/agent/queue']);
        break;
      default:
        router.navigate(['/']);
    }

    return false;
  };
};

// Guards spécifiques par rôle
export const clientGuard: CanActivateFn        = roleGuard([UserRole.CLIENT, UserRole.ADMIN]);
export const freelancerGuard: CanActivateFn    = roleGuard([UserRole.FREELANCER, UserRole.ADMIN]);
export const adminGuard: CanActivateFn         = roleGuard([UserRole.ADMIN]);
export const supportAgentGuard: CanActivateFn  = roleGuard([UserRole.SUPPORT_AGENT, UserRole.ADMIN]);  // ← NOUVEAU