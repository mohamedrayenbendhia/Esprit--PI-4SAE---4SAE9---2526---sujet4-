import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserRole, UserStatus } from '../../shared/models/user.model';

export interface UserListItem {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  type: UserRole;
  status: UserStatus;
  createdAt: string;
  lastLogin?: string;
  avatar?: string;
}

export interface UserListResponse {
  users: UserListItem[];
  total: number;
  page: number;
  size: number;
}

export interface UserFilters {
  search?: string;
  role?: UserRole;
  status?: UserStatus;
  page: number;
  size: number;
}

@Injectable({
  providedIn: 'root'
})
export class UserManagementService {
  private apiUrl = `${environment.apiUrl}/admin/users`;

  constructor(private http: HttpClient) {}

  getUsers(filters: UserFilters): Observable<UserListResponse> {
    let params = new HttpParams()
      .set('page', filters.page.toString())
      .set('size', filters.size.toString());

    if (filters.search) {
      params = params.set('search', filters.search);
    }
    if (filters.role) {
      params = params.set('type', filters.role);
    }
    if (filters.status) {
      params = params.set('status', filters.status);
    }

    return this.http.get<UserListResponse>(this.apiUrl, { params });
  }

  getUserById(id: string): Observable<UserListItem> {
    return this.http.get<UserListItem>(`${this.apiUrl}/${id}`);
  }

  updateUserStatus(id: string, status: UserStatus): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/status`, { status });
  }

  deleteUser(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
