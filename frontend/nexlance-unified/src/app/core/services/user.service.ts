import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { User, UserStatus, UserType, SubscriptionType } from '../../shared/models/user.model';
import { environment } from '../../../environments/environment';

export interface UserFilter {
  type?: UserType;
  status?: UserStatus;
  subscriptionType?: SubscriptionType;
  kycStatus?: string;
  searchTerm?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface UserStats {
  totalUsers: number;
  newUsersThisWeek: number;
  kycVerificationRate: number;
  activeUsers: number;
  freelancers: number;
  clients: number;
  admins: number;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private http = inject(HttpClient);
  private readonly API_URL = `${environment.apiUrl}/users`;

  getUserById(id: string): Observable<User> {
    return this.http.get<User>(`${this.API_URL}/${id}`);
  }

  getAdminUserById(id: string): Observable<User> {
    return this.http.get<User>(`${environment.apiUrl}/admin/users/${id}`);
  }

  getUserByEmail(email: string): Observable<User> {
    return this.http.get<User>(`${this.API_URL}/email/${email}`);
  }

  getCurrentUser(): Observable<User> {
    return this.http.get<User>(`${this.API_URL}/me`);
  }

  updateUser(id: string, user: Partial<User>): Observable<User> {
    return this.http.put<User>(`${this.API_URL}/${id}`, user);
  }

  updateCurrentUser(user: Partial<User>): Observable<User> {
    return this.http.put<User>(`${this.API_URL}/me`, user);
  }

  uploadAvatar(file: File): Observable<User> {
    const formData = new FormData();
    formData.append('avatar', file);
    return this.http.post<User>(`${this.API_URL}/me/avatar`, formData);
  }

  deleteUser(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  deleteCurrentAccount(): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/me`);
  }

  // Admin endpoints
  getAllUsers(filter?: UserFilter): Observable<PaginatedResponse<User>> {
    let params = new HttpParams();
    
    if (filter) {
      Object.entries(filter).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params = params.set(key, value.toString());
        }
      });
    }

    return this.http.get<PaginatedResponse<User>>(`${this.API_URL}`, { params });
  }

  getUsersByType(type: UserType, page: number = 0, size: number = 100): Observable<PaginatedResponse<User>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<PaginatedResponse<User>>(`${this.API_URL}/type/${type}`, { params });
  }

  getUserStats(): Observable<UserStats> {
    // Backend is at /api/admin/users/stats and returns {total, active, freelancers, clients, admins}
    return this.http.get<any>(`${environment.apiUrl}/admin/users/stats`).pipe(
      map((raw: any) => ({
        totalUsers: raw.total || 0,
        newUsersThisWeek: raw.newUsersThisWeek || 0,
        kycVerificationRate: raw.kycVerificationRate || 0,
        activeUsers: raw.active || 0,
        freelancers: raw.freelancers || 0,
        clients: raw.clients || 0,
        admins: raw.admins || 0
      } as UserStats))
    );
  }

  updateUserStatus(userId: string, status: UserStatus): Observable<User> {
    return this.http.put<User>(`${this.API_URL}/${userId}/status`, { status });
  }

  updateUserSubscription(userId: string, subscriptionType: SubscriptionType): Observable<User> {
    return this.http.patch<User>(`${this.API_URL}/${userId}/subscription`, { subscriptionType });
  }

  sendEmailToUser(userId: string, subject: string, message: string): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/${userId}/send-email`, { subject, message });
  }

  getUserActivityLogs(userId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/${userId}/activity-logs`);
  }

  exportUsers(filter?: UserFilter): Observable<Blob> {
    let params = new HttpParams();
    
    if (filter) {
      Object.entries(filter).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params = params.set(key, value.toString());
        }
      });
    }

    return this.http.get(`${this.API_URL}/export`, { 
      params, 
      responseType: 'blob' 
    });
  }
}
