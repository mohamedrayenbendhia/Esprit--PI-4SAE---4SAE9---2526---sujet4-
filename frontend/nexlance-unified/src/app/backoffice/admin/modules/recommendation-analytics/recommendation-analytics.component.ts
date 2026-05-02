import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';
import { RecommendationService, Recommendation } from '../../../../core/services/recommendation.service';

interface StatCard {
  title: string;
  value: number | string;
  icon: string;
  trend?: number;
  color: string;
}

interface ChartData {
  name: string;
  value: number;
}

@Component({
  selector: 'app-recommendation-analytics',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatCardModule,
    MatProgressSpinnerModule,
    TranslateModule
  ],
  templateUrl: './recommendation-analytics.component.html',
  styleUrls: ['./recommendation-analytics.component.scss']
})
export class RecommendationAnalyticsComponent implements OnInit {
  isLoading = true;

  // KPIs
  stats: StatCard[] = [];

  // Charts data
  monthlyData: ChartData[] = [];
  topClients: ChartData[] = [];
  topFreelancers: ChartData[] = [];
  conversionData: ChartData[] = [];

  // Calculated metrics
  acceptanceRate = 0;
  rejectionRate = 0;
  conversionRate = 0;

  constructor(private recommendationService: RecommendationService) {}

  ngOnInit(): void {
    this.loadAnalytics();
  }

  loadAnalytics(): void {
    this.isLoading = true;

    // Load all recommendations
    this.recommendationService.getAllRecommendations().subscribe({
      next: (recommendations: Recommendation[]) => {
        const total = recommendations.length;
        const accepted = recommendations.filter((r: Recommendation) => r.status === 'ACCEPTED').length;
        const rejected = recommendations.filter((r: Recommendation) => r.status === 'REJECTED').length;
        const pending = recommendations.filter((r: Recommendation) => r.status === 'PENDING').length;

        // Calculate rates
        this.acceptanceRate = total > 0 ? Math.round((accepted / total) * 100) : 0;
        this.rejectionRate = total > 0 ? Math.round((rejected / total) * 100) : 0;
        this.conversionRate = total > 0 ? Math.round((accepted / total) * 100) : 0;

        // Build stats cards
        this.stats = [
          {
            title: 'admin.analytics.recommendations.totalSent',
            value: total,
            icon: 'send',
            trend: 12,
            color: '#1976D2'
          },
          {
            title: 'admin.analytics.recommendations.acceptanceRate',
            value: `${this.acceptanceRate}%`,
            icon: 'check_circle',
            trend: 5,
            color: '#388E3C'
          },
          {
            title: 'admin.analytics.recommendations.rejectionRate',
            value: `${this.rejectionRate}%`,
            icon: 'cancel',
            trend: -3,
            color: '#D32F2F'
          },
          {
            title: 'admin.analytics.recommendations.conversionRate',
            value: `${this.conversionRate}%`,
            icon: 'trending_up',
            trend: 8,
            color: '#F57C00'
          }
        ];

        // Build monthly evolution data from actual recommendations
        const monthlyCounts = new Map<string, number>();
        const now = new Date();
        for (let i = 5; i >= 0; i--) {
          const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
          const key = d.toLocaleString('default', { month: 'short' });
          monthlyCounts.set(key, 0);
        }
        recommendations.forEach((rec: Recommendation) => {
          const recDate = new Date(rec.sentDate || rec.createdAt);
          const sixMonthsAgo = new Date(now.getFullYear(), now.getMonth() - 5, 1);
          if (recDate >= sixMonthsAgo) {
            const key = recDate.toLocaleString('default', { month: 'short' });
            if (monthlyCounts.has(key)) {
              monthlyCounts.set(key, (monthlyCounts.get(key) || 0) + 1);
            }
          }
        });
        this.monthlyData = Array.from(monthlyCounts.entries()).map(([name, value]) => ({ name, value }));

        // Build top clients data (aggregate by clientId)
        const clientCounts = new Map<string, number>();
        recommendations.forEach((rec: Recommendation) => {
          clientCounts.set(rec.clientId, (clientCounts.get(rec.clientId) || 0) + 1);
        });
        
        this.topClients = Array.from(clientCounts.entries())
          .sort((a, b) => b[1] - a[1])
          .slice(0, 5)
          .map(([clientId, count]) => ({
            name: `Client ${clientId}`,
            value: count
          }));

        // Build top freelancers data (aggregate by freelanceId)
        const freelancerCounts = new Map<string, number>();
        recommendations.forEach((rec: Recommendation) => {
          freelancerCounts.set(rec.freelanceId, (freelancerCounts.get(rec.freelanceId) || 0) + 1);
        });
        
        this.topFreelancers = Array.from(freelancerCounts.entries())
          .sort((a, b) => b[1] - a[1])
          .slice(0, 5)
          .map(([freelanceId, count]) => ({
            name: `Freelance ${freelanceId}`,
            value: count
          }));

        // Conversion funnel from real data
        const viewed = recommendations.filter((r: Recommendation) => r.viewCount > 0).length;
        this.conversionData = [
          { name: 'Sent', value: total },
          { name: 'Viewed', value: viewed },
          { name: 'Accepted', value: accepted },
          { name: 'Rejected', value: rejected }
        ];

        this.isLoading = false;
      },
      error: (error: any) => {
        console.error('Error loading analytics:', error);
        this.isLoading = false;
      }
    });
  }

  getMaxValue(data: ChartData[]): number {
    return Math.max(...data.map(d => d.value));
  }

  getBarHeight(value: number, max: number): string {
    return `${(value / max) * 100}%`;
  }

  getTrendIcon(trend?: number): string {
    if (!trend) return '';
    return trend > 0 ? 'trending_up' : 'trending_down';
  }

  getTrendColor(trend?: number): string {
    if (!trend) return '';
    return trend > 0 ? '#388E3C' : '#D32F2F';
  }

  exportReport(): void {
    console.log('Export analytics report...');
    // TODO: Implement PDF export
  }
}
