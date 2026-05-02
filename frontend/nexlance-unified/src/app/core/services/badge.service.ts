import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Badge {
  id: number;
  name: string;
  description: string;
  minScore: number;
  minProjects: number;
  icon: string;
  createdAt: Date;
}

export interface UserBadge {
  id: number;
  userId: string;
  badge: Badge;
  assignedAt: Date;
}

@Injectable({
  providedIn: 'root'
})
export class BadgeService {
  // ✅ CORRIGÉ: Utiliser freelancer au lieu de client
  private baseUrl = `${environment.evaluationApiUrl}/freelancer/badges`;

  constructor(private http: HttpClient) {}

  getAllBadges(): Observable<Badge[]> {
    return this.http.get<Badge[]>(this.baseUrl);
  }

  getFreelancerBadges(freelancerId: string): Observable<UserBadge[]> {
    return this.http.get<UserBadge[]>(`${this.baseUrl}/my-badges/${freelancerId}`);
  }

  getUserBadges(userId: string): Observable<UserBadge[]> {
    return this.getFreelancerBadges(userId);
  }

  getBadgeDetails(badgeId: number): Observable<Badge> {
    return this.http.get<Badge>(`${this.baseUrl}/${badgeId}`);
  }

  getBadgeProgress(freelancerId: string, badgeId: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/badge/${badgeId}/progress/${freelancerId}`);
  }

  getAllBadgesWithProgress(freelancerId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/all-with-progress/${freelancerId}`);
  }

// Dans badge.service.ts
checkAndAssignBadges(freelancerId: string, averageScore: number, totalProjects: number): Observable<any> {
  // Vous devez créer cet endpoint dans le backend
  return this.http.post<any>(`${this.baseUrl}/check-and-assign`, {
    userId: freelancerId,
    averageScore: averageScore,
    totalProjects: totalProjects
  });
}

}