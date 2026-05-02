import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CreateRecommendationRequest {
  clientId: string;
  clientName?: string;
  freelanceId: string;
  jobOfferId: string;
  proposedBudget?: number;
  message: string;
  expirationDate?: Date;
}

export enum RecommendationStatus {
  PENDING = 'PENDING',
  ACCEPTED = 'ACCEPTED',
  REJECTED = 'REJECTED',
  CANCELLED = 'CANCELLED',
  EXPIRED = 'EXPIRED'
}

export interface Recommendation {
  id: number;
  clientId: string;
  freelanceId: string;
  jobOfferId: string;
  message: string;
  proposedBudget?: number;
  status: RecommendationStatus;
  sentDate: Date;
  responseDate?: Date;
  expirationDate?: Date;
  viewCount: number;
  isReminderSent: boolean;
  reminderSentDate?: Date;
  metadata?: any;
  cancellationReason?: string;
  createdAt: Date;
  updatedAt: Date;
}

@Injectable({
  providedIn: 'root'
})
export class RecommendationService {
  private apiUrl = `${environment.jobOffersApiUrl}/recommendations`;

  constructor(private http: HttpClient) {}

  /**
   * Create a new recommendation
   */
  createRecommendation(request: CreateRecommendationRequest): Observable<Recommendation> {
    return this.http.post<Recommendation>(this.apiUrl, request);
  }

  /**
   * Get all recommendations (Admin only)
   */
  getAllRecommendations(): Observable<Recommendation[]> {
    return this.http.get<Recommendation[]>(`${this.apiUrl}/all`);
  }

  /**
   * Get all recommendations for a specific client
   */
  getRecommendationsByClientId(clientId: string, status?: RecommendationStatus): Observable<Recommendation[]> {
    if (status) {
      return this.http.get<Recommendation[]>(`${this.apiUrl}/client/${clientId}/status/${status}`);
    }
    return this.http.get<Recommendation[]>(`${this.apiUrl}/client/${clientId}`);
  }

  /**
   * Get all recommendations for a specific freelancer
   */
  getRecommendationsByFreelanceId(freelanceId: string): Observable<Recommendation[]> {
    return this.http.get<Recommendation[]>(`${this.apiUrl}/freelance/${freelanceId}`);
  }

  /**
   * Get pending recommendations for a freelancer
   */
  getPendingRecommendationsForFreelance(freelanceId: string): Observable<Recommendation[]> {
    return this.http.get<Recommendation[]>(`${this.apiUrl}/freelance/${freelanceId}/pending`);
  }

  /**
   * Send a reminder for a pending recommendation
   */
  sendReminder(recommendationId: number): Observable<Recommendation> {
    return this.http.post<Recommendation>(`${this.apiUrl}/${recommendationId}/reminder`, {});
  }

  /**
   * Cancel a recommendation
   */
  cancelRecommendation(recommendationId: number, reason?: string): Observable<Recommendation> {
    return this.http.post<Recommendation>(`${this.apiUrl}/${recommendationId}/cancel`, { reason: reason || '' });
  }

  /**
   * Get recommendation details
   */
  getRecommendationById(recommendationId: number): Observable<Recommendation> {
    return this.http.get<Recommendation>(`${this.apiUrl}/${recommendationId}`);
  }

  /**
   * Accept a recommendation (Freelancer side)
   */
  acceptRecommendation(recommendationId: number, response?: string): Observable<Recommendation> {
    return this.http.post<Recommendation>(`${this.apiUrl}/${recommendationId}/accept`, { response: response || '' });
  }

  /**
   * Reject a recommendation (Freelancer side)
   */
  rejectRecommendation(recommendationId: number, response?: string): Observable<Recommendation> {
    return this.http.post<Recommendation>(`${this.apiUrl}/${recommendationId}/reject`, { response: response || '' });
  }

  /**
   * Increment view count
   */
  incrementViews(recommendationId: number): Observable<Recommendation> {
    return this.http.post<Recommendation>(`${this.apiUrl}/${recommendationId}/view`, {});
  }

  /**
   * Get recommendations by status
   */
  getRecommendationsByStatus(status: RecommendationStatus): Observable<Recommendation[]> {
    return this.http.get<Recommendation[]>(`${this.apiUrl}/status/${status}`);
  }

  /**
   * Get recent recommendations
   */
  getRecentRecommendations(days: number = 7): Observable<Recommendation[]> {
    return this.http.get<Recommendation[]>(`${this.apiUrl}/recent?days=${days}`);
  }
}
