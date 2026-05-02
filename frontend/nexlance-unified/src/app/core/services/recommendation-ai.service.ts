import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface FreelancerProfile {
  id: number;
  skills: string[];
  experience_years: number;
  hourly_rate: number;
  rating: number;
  completed_projects: number;
}

export interface JobOfferProfile {
  id: number;
  required_skills: string[];
  budget: number;
  experience_required: number;
  category?: string;
}

export interface MatchDetails {
  skill_similarity: number;
  experience_score: number;
  budget_match: number;
  rating_score?: number;
  projects_score?: number;
}

export interface RecommendationResult {
  freelancer_id: number;
  job_offer_id: number;
  score: number;
  match_details: MatchDetails;
}

export interface RecommendationForJobResponse {
  job_offer_id: number;
  recommendations: RecommendationResult[];
  total: number;
}

export interface RecommendationForFreelancerResponse {
  freelancer_id: number;
  recommended_jobs: RecommendationResult[];
  total: number;
}

export interface AiHealthResponse {
  status: string;
  service: string;
  port: number;
  eureka: string;
  database: string;
}

@Injectable({
  providedIn: 'root'
})
export class RecommendationAiService {
  private apiUrl = `${environment.recommendationAiApiUrl}/recommendations`;

  constructor(private http: HttpClient) {}

  /** Health check du service IA */
  getHealth(): Observable<AiHealthResponse> {
    return this.http.get<AiHealthResponse>(`${this.apiUrl}/health`);
  }

  /** Info sur le service */
  getInfo(): Observable<any> {
    return this.http.get(`${this.apiUrl}/info`);
  }

  /** Synchroniser un profil freelancer */
  syncFreelancerProfile(profile: FreelancerProfile): Observable<any> {
    return this.http.post(`${this.apiUrl}/freelancers`, profile);
  }

  /** Synchroniser une offre d'emploi */
  syncJobOffer(offer: JobOfferProfile): Observable<any> {
    return this.http.post(`${this.apiUrl}/job-offers`, offer);
  }

  /** Obtenir les meilleurs freelancers pour une offre (Matching IA) */
  getRecommendationsForJob(jobOfferId: number, limit: number = 10): Observable<RecommendationForJobResponse> {
    return this.http.get<RecommendationForJobResponse>(
      `${this.apiUrl}/for-job/${jobOfferId}?limit=${limit}`
    );
  }

  /** Obtenir les meilleures offres pour un freelancer (Matching IA) */
  getRecommendationsForFreelancer(freelancerId: number, limit: number = 10): Observable<RecommendationForFreelancerResponse> {
    return this.http.get<RecommendationForFreelancerResponse>(
      `${this.apiUrl}/for-freelancer/${freelancerId}?limit=${limit}`
    );
  }

  /** Historique des recommandations */
  getHistory(freelancerId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/history/${freelancerId}`);
  }

  /** Liste les freelancers enregistrés */
  getFreelancers(): Observable<FreelancerProfile[]> {
    return this.http.get<FreelancerProfile[]>(`${this.apiUrl}/freelancers`);
  }

  /** Liste les offres enregistrées */
  getJobOffers(): Observable<JobOfferProfile[]> {
    return this.http.get<JobOfferProfile[]>(`${this.apiUrl}/job-offers`);
  }
}
