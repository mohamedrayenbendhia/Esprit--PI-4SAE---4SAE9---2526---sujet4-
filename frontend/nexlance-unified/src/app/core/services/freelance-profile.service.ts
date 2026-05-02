import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FreelanceProfile, CreateFreelanceProfileRequest, Availability } from '../models/freelance-profile.model';
import { environment } from '../../../environments/environment';

export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: any;
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalPages: number;
  totalElements: number;
  last: boolean;
  first: boolean;
  size: number;
  number: number;
  sort: any;
  numberOfElements: number;
  empty: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class FreelanceProfileService {
  private http = inject(HttpClient);
  private readonly API_URL = `${environment.apiUrl}/freelance-profiles`;

  getMyProfile(): Observable<FreelanceProfile> {
    return this.http.get<FreelanceProfile>(`${this.API_URL}/me`);
  }

  getProfileByUserId(userId: string): Observable<FreelanceProfile> {
    return this.http.get<FreelanceProfile>(`${this.API_URL}/user/${userId}`);
  }

  getProfileById(id: string): Observable<FreelanceProfile> {
    return this.http.get<FreelanceProfile>(`${this.API_URL}/${id}`);
  }

  getAllProfiles(page: number = 0, size: number = 20): Observable<PageResponse<FreelanceProfile>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<FreelanceProfile>>(`${this.API_URL}`, { params });
  }

  getProfilesByAvailability(availability: Availability, page: number = 0, size: number = 20): Observable<PageResponse<FreelanceProfile>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<FreelanceProfile>>(`${this.API_URL}/search/availability/${availability}`, { params });
  }

  getProfilesByLocation(location: string, page: number = 0, size: number = 20): Observable<PageResponse<FreelanceProfile>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<FreelanceProfile>>(`${this.API_URL}/search/location/${location}`, { params });
  }

  createProfile(profile: CreateFreelanceProfileRequest): Observable<FreelanceProfile> {
    return this.http.post<FreelanceProfile>(`${this.API_URL}`, profile);
  }

  updateProfile(id: string, profile: Partial<FreelanceProfile>): Observable<FreelanceProfile> {
    return this.http.put<FreelanceProfile>(`${this.API_URL}/${id}`, profile);
  }

  updateMyProfile(profile: Partial<FreelanceProfile>): Observable<FreelanceProfile> {
    return this.http.put<FreelanceProfile>(`${this.API_URL}/me`, profile);
  }

  updateAvailability(availability: Availability): Observable<FreelanceProfile> {
    return this.http.patch<FreelanceProfile>(`${this.API_URL}/me/availability`, { availability });
  }

  deleteProfile(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  searchProfiles(filters: any): Observable<FreelanceProfile[]> {
    return this.http.post<FreelanceProfile[]>(`${this.API_URL}/search`, filters);
  }
}
