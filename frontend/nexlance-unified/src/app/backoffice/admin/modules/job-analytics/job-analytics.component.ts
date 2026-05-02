import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { MatIconModule } from '@angular/material/icon';
import { JobOfferService } from '@core/services/job-offer.service';
import { PosthogService } from '@core/services/posthog.service';
import { JobOfferStats } from '@core/models/job-offer.model';

@Component({
  selector: 'app-job-analytics',
  standalone: true,
  imports: [CommonModule, TranslateModule, MatIconModule],
  templateUrl: './job-analytics.component.html',
  styleUrls: ['./job-analytics.component.scss']
})
export class JobAnalyticsComponent implements OnInit {
  stats!: JobOfferStats;
  isLoading = true;

  // Data for charts (loaded from API)
  categoryData: any[] = [];
  budgetDistribution: any[] = [];
  topClients: any[] = [];
  monthlyData: any[] = [];

  // Mock data for acceptance rates (would need application tracking to be real)
  acceptanceRates = [
    { level: 'Débutant', rate: 35 },
    { level: 'Intermédiaire', rate: 58 },
    { level: 'Expert', rate: 72 }
  ];

  constructor(
    private jobOfferService: JobOfferService,
    private posthogService: PosthogService
  ) {}

  ngOnInit(): void {
    this.posthogService.trackPageView('job_analytics');
    this.loadStats();
    this.loadAnalytics();
  }

  loadStats(): void {
    this.jobOfferService.getJobOfferStats().subscribe({
      next: (stats) => {
        this.stats = stats;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading stats:', error);
        // Use mock data if API fails
        this.stats = {
          totalOffers: 350,
          totalApplications: 1445,
          conversionRate: 24.5,
          avgApplicationsPerOffer: 4.13,
          avgResponseTime: 2.8
        };
        this.isLoading = false;
      }
    });
  }

  loadAnalytics(): void {
    // Load category distribution
    this.jobOfferService.getCategoryDistribution().subscribe({
      next: (data) => {
        this.categoryData = data;
      },
      error: (error) => {
        console.error('Error loading category data:', error);
        // Keep empty array on error
      }
    });

    // Load budget distribution
    this.jobOfferService.getBudgetDistribution().subscribe({
      next: (data) => {
        this.budgetDistribution = data;
      },
      error: (error) => {
        console.error('Error loading budget data:', error);
      }
    });

    // Load top clients
    this.jobOfferService.getTopClients(5).subscribe({
      next: (data) => {
        this.topClients = data;
      },
      error: (error) => {
        console.error('Error loading top clients:', error);
      }
    });

    // Load monthly data
    this.jobOfferService.getMonthlyData(6).subscribe({
      next: (data) => {
        this.monthlyData = data;
      },
      error: (error) => {
        console.error('Error loading monthly data:', error);
      }
    });
  }

  getMaxValue(data: any[], key: string): number {
    return Math.max(...data.map(item => item[key]));
  }

  getBarHeight(value: number, max: number): string {
    return `${(value / max) * 100}%`;
  }
}
