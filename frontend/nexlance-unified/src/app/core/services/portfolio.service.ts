import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Portfolio, PortfolioRequest, PortfolioUpdateRequest, Project, ProjectRequest } from '../../shared/models/portfolio.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PortfolioService {
  private http = inject(HttpClient);
  private readonly API = environment.portfolioApiUrl;

  // ── Admin
  getAllPortfolios(): Observable<Portfolio[]> {
    return this.http.get<Portfolio[]>(`${this.API}/admin/portfolios`);
  }

  getPortfolioById(id: number): Observable<Portfolio> {
    return this.http.get<Portfolio>(`${this.API}/admin/portfolios/${id}`);
  }

  deletePortfolio(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/admin/portfolios/${id}`);
  }

  // ── Freelancer
  getMyPortfolio(): Observable<Portfolio> {
    return this.http.get<Portfolio>(`${this.API}/freelancer/portfolios/me`);
  }

  createPortfolio(request: PortfolioRequest): Observable<Portfolio> {
    return this.http.post<Portfolio>(`${this.API}/freelancer/portfolios`, request);
  }

  updatePortfolio(id: number, request: PortfolioUpdateRequest): Observable<Portfolio> {
    return this.http.put<Portfolio>(`${this.API}/freelancer/portfolios/${id}`, request);
  }

  addProject(portfolioId: number, request: ProjectRequest): Observable<Project> {
    return this.http.post<Project>(`${this.API}/freelancer/portfolios/${portfolioId}/projects`, request);
  }

  getProjects(portfolioId: number): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.API}/freelancer/portfolios/${portfolioId}/projects`);
  }

  updateProject(projectId: number, request: ProjectRequest): Observable<Project> {
    return this.http.put<Project>(`${this.API}/freelancer/portfolios/projects/${projectId}`, request);
  }

  deleteProject(projectId: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/freelancer/portfolios/projects/${projectId}`);
  }

  // ── Client
  getFreelancerPortfolio(userId: string): Observable<Portfolio> {
    return this.http.get<Portfolio>(`${this.API}/client/portfolios/user/${userId}`);
  }

  toggleVisibility(id: number): Observable<Portfolio> {
  return this.http.patch<Portfolio>(`${this.API}/freelancer/portfolios/${id}/visibility`, {});
}

getAllPublicPortfolios(): Observable<Portfolio[]> {
  return this.http.get<Portfolio[]>(`${this.API}/client/portfolios`);
}

incrementPortfolioViews(userId: string): Observable<Portfolio> {
  return this.http.get<Portfolio>(`${this.API}/client/portfolios/user/${userId}`);
}
}