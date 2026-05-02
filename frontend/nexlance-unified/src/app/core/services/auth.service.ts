import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest, User, UserRole, UserType, UserStatus, SubscriptionType } from '../../shared/models/user.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  
  private readonly API_URL = `${environment.apiUrl}/auth`;
  private readonly TOKEN_KEY = 'auth_token';
  private readonly USER_KEY = 'current_user';

  private currentUserSubject = new BehaviorSubject<User | null>(this.getUserFromStorage());
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor() {}

  private getUserFromStorage(): User | null {
    if (typeof window !== 'undefined') {
      const userStr = localStorage.getItem(this.USER_KEY);
      return userStr ? JSON.parse(userStr) : null;
    }
    return null;
  }

  /**
   * Login - unified authentication for all roles
   */
  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/login`, credentials).pipe(
      tap(response => {
        this.setSession(response);
      })
    );
  }

  /**
   * Register - registration by role
   */
  registerClient(data: RegisterRequest): Observable<AuthResponse> {
    return this.register({ ...data, role: UserRole.CLIENT });
  }

  registerFreelancer(data: RegisterRequest): Observable<AuthResponse> {
    return this.register({ ...data, role: UserRole.FREELANCER });
  }

  registerAdmin(data: RegisterRequest): Observable<AuthResponse> {
    return this.register({ ...data, role: UserRole.ADMIN });
  }

  private register(data: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/register`, data).pipe(
      tap(response => {
        this.setSession(response);
      })
    );
  }

  /**
   * Save session
   */
  private setSession(authResult: AuthResponse): void {
    if (typeof window !== 'undefined') {
      localStorage.setItem(this.TOKEN_KEY, authResult.token);
      
      // Backend can return 'role' or 'type' - use whichever exists
      const backendRole = (authResult.role || authResult.type) as string;
      console.log('🔐 Setting session - backend returned:', backendRole);
      
      // Normalize role to UserRole enum (FREELANCE/FREELANCER → FREELANCE)
      let userRole: UserRole;
      const normalizedRole = backendRole.toUpperCase();
      if (normalizedRole === 'CLIENT') {
        userRole = UserRole.CLIENT;
      } else if (normalizedRole === 'FREELANCE' || normalizedRole === 'FREELANCER') {
        userRole = UserRole.FREELANCER; // = 'FREELANCE'
      } else if (normalizedRole === 'ADMIN') {
        userRole = UserRole.ADMIN;
      } else if (normalizedRole === 'SUPPORT_AGENT') {
        userRole = UserRole.SUPPORT_AGENT;
      } else {
        console.error('❌ Unknown role from backend:', backendRole);
        userRole = backendRole as UserRole;
      }
      
      console.log('🔐 Normalized role:', userRole, '(enum value:', UserRole.FREELANCER, ')');
      
      const user: User = {
        id: authResult.userId,
        email: authResult.email,
        firstName: authResult.firstName,
        lastName: authResult.lastName,
        role: userRole,
        type: userRole === UserRole.CLIENT ? UserType.CLIENT :
              userRole === UserRole.FREELANCER ? UserType.FREELANCE :
              userRole === UserRole.SUPPORT_AGENT ? UserType.SUPPORT_AGENT : UserType.ADMIN,
        status: authResult.status as any,
        subscriptionType: SubscriptionType.FREE,
        emailVerified: authResult.emailVerified,
        avatar: authResult.avatar,
        createdAt: new Date(),
        updatedAt: new Date()
      };
      
      localStorage.setItem(this.USER_KEY, JSON.stringify(user));
      this.currentUserSubject.next(user);
      console.log('✅ User session set with role:', user.role);
    }
  }

  /**
   * Redirect after login based on role
   */
  redirectAfterLogin(role: UserRole): void {
    console.log('🔄 Redirecting user with role:', role);
    
    // Normalize role (backend can return FREELANCE or FREELANCER)
    const normalizedRole = role.toString().toUpperCase();
    
    if (normalizedRole === 'CLIENT' || role === UserRole.CLIENT) {
      console.log('➡️ Navigating to client dashboard');
      this.router.navigate(['/frontoffice/client/dashboard']);
    } else if (normalizedRole === 'FREELANCE' || normalizedRole === 'FREELANCER' || role === UserRole.FREELANCER) {
      console.log('➡️ Navigating to freelancer dashboard');
      this.router.navigate(['/frontoffice/freelancer/dashboard']);
    } else if (normalizedRole === 'ADMIN' || role === UserRole.ADMIN) {
      console.log('➡️ Navigating to admin dashboard');
      this.router.navigate(['/backoffice/admin/dashboard']);
    } else if (normalizedRole === 'SUPPORT_AGENT' || role === UserRole.SUPPORT_AGENT) {
      console.log('➡️ Navigating to agent queue');
      this.router.navigate(['/backoffice/agent/queue']);
    } else {
      console.log('➡️ Role not recognized, navigating to landing');
      this.router.navigate(['/landing']);
    }
  }

  /**
   * Logout
   */
  logout(): void {
    if (typeof window !== 'undefined') {
      localStorage.removeItem(this.TOKEN_KEY);
      localStorage.removeItem(this.USER_KEY);
    }
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return this.getToken() !== null;
  }

  /**
   * Get token
   */
  getToken(): string | null {
    if (typeof window !== 'undefined') {
      return localStorage.getItem(this.TOKEN_KEY);
    }
    return null;
  }

  /**
   * Get current user
   */
  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  /**
   * Check user role
   */
  hasRole(role: UserRole): boolean {
    const user = this.getCurrentUser();
    return user?.role === role;
  }

  /**
   * Check if admin
   */
  isAdmin(): boolean {
    return this.hasRole(UserRole.ADMIN);
  }

  /**
   * Check if client
   */
  isClient(): boolean {
    return this.hasRole(UserRole.CLIENT);
  }

  /**
   * Check if freelancer
   */
  isFreelancer(): boolean {
    return this.hasRole(UserRole.FREELANCER);
  }

  /**
   * Update user session (for profile updates)
   */
  public updateUserSession(user: User): void {
    const token = localStorage.getItem(this.TOKEN_KEY) || '';
    this.setSession({
      token,
      userId: user.id,
      email: user.email,
      firstName: user.firstName,
      lastName: user.lastName,
      role: user.role,
      type: user.type,
      status: user.status as any,
      emailVerified: user.emailVerified || false,
      avatar: user.avatar
    });
  }
}
