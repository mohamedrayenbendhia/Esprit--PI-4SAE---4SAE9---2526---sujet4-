import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  FreelanceInvitation,
  CreateInvitationDto,
  UpdateInvitationDto,
  InvitationFilters,
  InvitationStatus
} from '../models/freelance-invitation.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class FreelanceInvitationService {
  private apiUrl = `${environment.invitationsApiUrl || environment.jobOffersApiUrl}/invitations`;
  private authService = inject(AuthService);

  constructor(private http: HttpClient) {}

  /**
   * Create an invitation
   */
  createInvitation(invitation: CreateInvitationDto): Observable<FreelanceInvitation> {
    return this.http.post<FreelanceInvitation>(this.apiUrl, invitation);
  }

  /**
   * Retrieve all invitations (with filters)
   */
  getInvitations(filters?: InvitationFilters): Observable<FreelanceInvitation[]> {
    let params = new HttpParams();
    if (filters?.status) params = params.set('status', filters.status);
    if (filters?.jobOfferId) params = params.set('jobOfferId', filters.jobOfferId);
    if (filters?.freelanceId) params = params.set('freelanceId', filters.freelanceId);
    if (filters?.clientId) params = params.set('clientId', filters.clientId);
    
    return this.http.get<FreelanceInvitation[]>(this.apiUrl, { params });
  }

  /**
   * Retrieve invitations sent by the logged-in client
   */
  getMyInvitations(filters?: InvitationFilters): Observable<FreelanceInvitation[]> {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.id) {
      console.error('No user logged in');
      return throwError(() => new Error('User not authenticated'));
    }
    
    const params = this.buildParams(filters).set('clientId', currentUser.id);
    return this.http.get<FreelanceInvitation[]>(`${this.apiUrl}/my-invitations`, { params });
  }

  /**
   * Retrieve invitations received by the logged-in freelancer
   */
  getReceivedInvitations(filters?: InvitationFilters): Observable<FreelanceInvitation[]> {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.id) {
      console.error('No user logged in');
      return throwError(() => new Error('User not authenticated'));
    }
    
    const params = this.buildParams(filters).set('freelanceId', currentUser.id);
    return this.http.get<FreelanceInvitation[]>(`${this.apiUrl}/received`, { params });
  }

  /**
   * Retrieve an invitation by ID
   */
  getInvitationById(id: string): Observable<FreelanceInvitation> {
    return this.http.get<FreelanceInvitation>(`${this.apiUrl}/${id}`);
  }

  /**
   * Update an invitation (accept/decline)
   */
  updateInvitation(id: string, update: UpdateInvitationDto): Observable<FreelanceInvitation> {
    return this.http.put<FreelanceInvitation>(`${this.apiUrl}/${id}`, update);
  }

  /**
   * Accept an invitation
   */
  acceptInvitation(id: string, message?: string): Observable<FreelanceInvitation> {
    return this.updateInvitation(id, {
      status: InvitationStatus.ACCEPTED,
      message
    });
  }

  /**
   * Decline an invitation
   */
  declineInvitation(id: string, reason?: string): Observable<FreelanceInvitation> {
    return this.updateInvitation(id, {
      status: InvitationStatus.DECLINED,
      message: reason
    });
  }

  /**
   * Cancel an invitation (by the client)
   */
  cancelInvitation(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Resend an invitation
   */
  resendInvitation(id: string): Observable<FreelanceInvitation> {
    return this.http.post<FreelanceInvitation>(`${this.apiUrl}/${id}/resend`, {});
  }

  /**
   * Invitation statistics (admin)
   */
  getInvitationStats(): Observable<any> {
    return this.http.get(`${this.apiUrl}/stats`);
  }

  /**
   * Expire old invitations
   */
  expireOldInvitations(): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/expire-old`, {});
  }

  /**
   * Helper to build params
   */
  private buildParams(filters?: InvitationFilters): HttpParams {
    let params = new HttpParams();
    if (filters?.status) params = params.set('status', filters.status);
    if (filters?.jobOfferId) params = params.set('jobOfferId', filters.jobOfferId);
    if (filters?.dateFrom) params = params.set('dateFrom', filters.dateFrom.toISOString());
    if (filters?.dateTo) params = params.set('dateTo', filters.dateTo.toISOString());
    return params;
  }
}
