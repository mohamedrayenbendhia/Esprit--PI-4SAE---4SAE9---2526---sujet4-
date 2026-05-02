import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Application,
  CreateApplicationDto,
  UpdateApplicationDto,
  ApplicationFilters,
  ApplicationStatus
} from '../models/application.model';

@Injectable({
  providedIn: 'root'
})
export class ApplicationService {
  private apiUrl = `${environment.jobOffersApiUrl}/applications`;

  constructor(private http: HttpClient) {}

  /**
   * Create a new application
   */
  createApplication(application: CreateApplicationDto): Observable<Application> {
    return this.http.post<Application>(this.apiUrl, application);
  }

  /**
   * Retrieve all applications (or filter by status)
   */
  getAllApplications(filters?: ApplicationFilters): Observable<Application[]> {
    if (filters?.status) {
      return this.http.get<Application[]>(`${this.apiUrl}/status/${filters.status}`);
    }
    return this.http.get<Application[]>(this.apiUrl);
  }

  /**
   * Retrieve an application by its ID
   */
  getApplicationById(id: string): Observable<Application> {
    return this.http.get<Application>(`${this.apiUrl}/${id}`);
  }

  /**
   * Update an application
   */
  updateApplication(id: string, application: UpdateApplicationDto): Observable<Application> {
    return this.http.put<Application>(`${this.apiUrl}/${id}`, application);
  }

  /**
   * Delete an application (withdraw)
   */
  deleteApplication(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Change the status of an application
   */
  changeStatus(id: string, status: ApplicationStatus): Observable<Application> {
    return this.http.patch<Application>(`${this.apiUrl}/${id}/status?status=${status}`, {});
  }

  /**
   * Mark an application as read
   */
  markAsRead(id: string): Observable<Application> {
    return this.http.patch<Application>(`${this.apiUrl}/${id}/read`, {});
  }

  /**
   * Retrieve applications for a specific job offer
   */
  getApplicationsByJobOffer(jobOfferId: string, filters?: ApplicationFilters): Observable<Application[]> {
    return this.http.get<Application[]>(`${this.apiUrl}/job-offer/${jobOfferId}`);
  }

  /**
   * Retrieve unread applications for an offer
   */
  getUnreadApplicationsByJobOffer(jobOfferId: string): Observable<Application[]> {
    return this.http.get<Application[]>(`${this.apiUrl}/job-offer/${jobOfferId}/unread`);
  }

  /**
   * Count applications for an offer
   */
  getApplicationCount(jobOfferId: string): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/job-offer/${jobOfferId}/count`);
  }

  /**
   * Count applications by status for an offer
   */
  getApplicationCountByStatus(jobOfferId: string, status: ApplicationStatus): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/job-offer/${jobOfferId}/count/${status}`);
  }

  /**
   * Retrieve my applications (logged-in freelancer)
   * TODO: Implement with authentication in backend
   */
  getMyApplications(filters?: ApplicationFilters): Observable<Application[]> {
    // If searching for a specific offer
    if (filters?.jobOfferId) {
      return this.getApplicationsByJobOffer(filters.jobOfferId);
    }
    // If searching for a specific freelancer
    if (filters?.freelanceId) {
      return this.http.get<Application[]>(`${this.apiUrl}/freelance/${filters.freelanceId}`);
    }
    return this.getAllApplications(filters);
  }

  /**
   * Shortlist an application
   */
  shortlistApplication(id: string): Observable<Application> {
    return this.changeStatus(id, ApplicationStatus.SHORTLISTED);
  }

  /**
   * Accept an application
   */
  acceptApplication(id: string): Observable<Application> {
    return this.changeStatus(id, ApplicationStatus.ACCEPTED);
  }

  /**
   * Reject an application
   */
  rejectApplication(id: string): Observable<Application> {
    return this.changeStatus(id, ApplicationStatus.REJECTED);
  }

  /**
   * Withdraw an application (by the freelancer)
   */
  withdrawApplication(id: string): Observable<Application> {
    return this.http.patch<Application>(`${this.apiUrl}/${id}/withdraw`, {});
  }

  /**
   * Count applications by status for an offer
   * Custom method that aggregates counters
   */
  getApplicationCountsByStatus(jobOfferId: string): Observable<{ [key: string]: number }> {
    // Backend doesn't have this exact endpoint, must aggregate manually
    // or implement a new endpoint in the backend
    return this.http.get<{ [key: string]: number }>(
      `${this.apiUrl}/job-offer/${jobOfferId}/counts`
    );
  }

  /**
   * Alias for compatibility: getApplicationCountsByJobOffer
   */
  getApplicationCountsByJobOffer(jobOfferId: string): Observable<{ [key: string]: number }> {
    return this.getApplicationCountsByStatus(jobOfferId);
  }
}
