import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import {
  JobOffer,
  CreateJobOfferDto,
  UpdateJobOfferDto,
  JobOfferFilters,
  JobOfferStats
} from '../models/job-offer.model';

@Injectable({
  providedIn: 'root'
})
export class JobOfferService {
  private apiUrl = `${environment.jobOffersApiUrl}/job-offers`;
  private authService = inject(AuthService);

  constructor(private http: HttpClient) {}

  /**
   * Create a new job offer
   */
  createJobOffer(jobOffer: CreateJobOfferDto): Observable<JobOffer> {
    return this.http.post<JobOffer>(this.apiUrl, jobOffer);
  }

  /**
   * Retrieve all offers (or filter by status)
   */
  getAllJobOffers(filters?: JobOfferFilters): Observable<JobOffer[]> {
    if (filters?.status) {
      return this.http.get<JobOffer[]>(`${this.apiUrl}/status/${filters.status}`);
    }
    return this.http.get<JobOffer[]>(this.apiUrl);
  }

  /**
   * Retrieve active offers
   */
  getActiveJobOffers(): Observable<JobOffer[]> {
    return this.http.get<JobOffer[]>(`${this.apiUrl}/active`);
  }

  /**
   * Retrieve offers by category
   */
  getJobOffersByCategory(category: string): Observable<JobOffer[]> {
    return this.http.get<JobOffer[]>(`${this.apiUrl}/category/${category}`);
  }

  /**
   * Retrieve remote offers
   */
  getRemoteJobOffers(): Observable<JobOffer[]> {
    return this.http.get<JobOffer[]>(`${this.apiUrl}/remote`);
  }

  /**
   * Retrieve offers by experience level
   */
  getJobOffersByExperienceLevel(level: string): Observable<JobOffer[]> {
    return this.http.get<JobOffer[]>(`${this.apiUrl}/experience-level/${level}`);
  }

  /**
   * Retrieve an offer by its ID
   */
  getJobOfferById(id: string): Observable<JobOffer> {
    return this.http.get<JobOffer>(`${this.apiUrl}/${id}`);
  }

  /**
   * Update an offer
   */
  updateJobOffer(id: string, jobOffer: UpdateJobOfferDto): Observable<JobOffer> {
    return this.http.put<JobOffer>(`${this.apiUrl}/${id}`, jobOffer);
  }

  /**
   * Delete an offer (soft delete)
   */
  deleteJobOffer(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Change the status of an offer
   */
  changeStatus(id: string, status: string): Observable<JobOffer> {
    return this.http.patch<JobOffer>(`${this.apiUrl}/${id}/status?status=${status}`, {});
  }

  /**
   * Publish an offer
   */
  publishJobOffer(id: string): Observable<JobOffer> {
    return this.changeStatus(id, 'OPEN');
  }

  /**
   * Archive an offer
   */
  archiveJobOffer(id: string): Observable<JobOffer> {
    return this.http.patch<JobOffer>(`${this.apiUrl}/${id}/archive`, {});
  }

  /**
   * Retrieve offers for a specific client
   */
  getClientJobOffers(clientId: string, filters?: JobOfferFilters): Observable<JobOffer[]> {
    return this.http.get<JobOffer[]>(`${this.apiUrl}/client/${clientId}`);
  }

  /**
   * Retrieve my offers (logged-in client) - uses client ID from auth
   */
  getMyJobOffers(filters?: JobOfferFilters): Observable<JobOffer[]> {
    const currentUser = this.authService.getCurrentUser();
    if (currentUser?.id) {
      return this.getClientJobOffers(currentUser.id, filters);
    }
    return this.getAllJobOffers(filters);
  }

  /**
   * Retrieve recommended offers for a freelancer
   * Returns active job offers sorted by most recent
   */
  getRecommendedJobOffers(freelanceId: string): Observable<JobOffer[]> {
    return this.getActiveJobOffers();
  }

  /**
   * Increment view count (already handled by backend in getJobOfferById)
   */
  incrementViewCount(id: string): Observable<void> {
    // Backend automatically increments on GET /{id}
    return new Observable(observer => {
      observer.next();
      observer.complete();
    });
  }

  /**
   * Upload files for an offer
   * Backend endpoint: POST /api/job-offers/upload
   * @param files Array of files to upload
   * @returns Array of file URLs
   */
  uploadAttachments(files: File[]): Observable<string[]> {
    const formData = new FormData();
    files.forEach(file => formData.append('files', file));
    return this.http.post<string[]>(`${this.apiUrl}/upload`, formData);
  }

  /**
   * Retrieve offer statistics (admin)
   */
  getJobOfferStats(): Observable<JobOfferStats> {
    return this.http.get<JobOfferStats>(`${this.apiUrl}/stats`);
  }

  /**
   * Get category distribution for analytics
   */
  getCategoryDistribution(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/analytics/categories`);
  }

  /**
   * Get budget distribution for analytics
   */
  getBudgetDistribution(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/analytics/budget`);
  }

  /**
   * Get top clients for analytics
   */
  getTopClients(limit: number = 5): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/analytics/top-clients?limit=${limit}`);
  }

  /**
   * Get monthly data for analytics
   */
  getMonthlyData(months: number = 6): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/analytics/monthly?months=${months}`);
  }

  /**
   * Search offers by keywords - client-side filtering until backend search is implemented
   */
  searchJobOffers(query: string, filters?: JobOfferFilters): Observable<JobOffer[]> {
    return this.getAllJobOffers(filters).pipe(
      map(jobs => {
        if (!query || query.trim().length === 0) return jobs;
        const q = query.toLowerCase();
        return jobs.filter(job =>
          (job.title && job.title.toLowerCase().includes(q)) ||
          (job.description && job.description.toLowerCase().includes(q)) ||
          (job.category && job.category.toLowerCase().includes(q)) ||
          (job.requiredSkills && job.requiredSkills.some((s: string) => s.toLowerCase().includes(q))) ||
          (job.location && job.location.toLowerCase().includes(q))
        );
      })
    );
  }
}
