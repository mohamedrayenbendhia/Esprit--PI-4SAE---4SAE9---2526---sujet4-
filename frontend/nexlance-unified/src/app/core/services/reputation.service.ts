import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ReputationScore {
  overallScore: number;
  completedProjects: number;
  totalRecommendations: number;
  acceptedRecommendations: number;
  responseRate: number;
  onTimeDeliveryRate: number;
  totalApplications: number;
  acceptedApplications: number;
  tier: 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM';
  badges: string[];
}

@Injectable({ providedIn: 'root' })
export class ReputationService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.jobOffersApiUrl}/reputation`;

  getReputation(freelancerId: string): Observable<ReputationScore> {
    return this.http.get<ReputationScore>(`${this.baseUrl}/${freelancerId}`);
  }

  getTierIcon(tier: string): string {
    switch (tier) {
      case 'PLATINUM': return 'diamond';
      case 'GOLD': return 'emoji_events';
      case 'SILVER': return 'workspace_premium';
      default: return 'military_tech';
    }
  }

  getTierColor(tier: string): string {
    switch (tier) {
      case 'PLATINUM': return '#E5E4E2';
      case 'GOLD': return '#FFD700';
      case 'SILVER': return '#C0C0C0';
      default: return '#CD7F32';
    }
  }

  getBadgeLabel(badge: string): string {
    const labels: { [key: string]: string } = {
      'TOP_PERFORMER': 'Top Performer',
      'EXPERIENCED': 'Experienced',
      'VERIFIED_FREELANCER': 'Verified Freelancer',
      'HIGHLY_RECOMMENDED': 'Highly Recommended',
      'TRUSTED_EXPERT': 'Trusted Expert',
      'FAST_RESPONDER': 'Fast Responder',
      'ACTIVE_CONTRIBUTOR': 'Active Contributor'
    };
    return labels[badge] || badge;
  }
}
