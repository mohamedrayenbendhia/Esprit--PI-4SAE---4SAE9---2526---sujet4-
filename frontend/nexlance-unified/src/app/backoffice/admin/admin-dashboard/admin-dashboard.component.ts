import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatGridListModule } from '@angular/material/grid-list';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';
import { Router } from '@angular/router';
import { UserManagementService, UserListItem } from '../../../core/services/user-management.service';
import { UserService } from '../../../core/services/user.service';
import { JobOfferService } from '../../../core/services/job-offer.service';
import { ProjectService } from '../../../core/services/project.service';
import { KycService } from '../../../core/services/kyc.service';
import { RecommendationService } from '../../../core/services/recommendation.service';
import { PosthogService } from '../../../core/services/posthog.service';
import { QuoteService, Quote } from '../../../core/services/quote.service';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatIconModule, MatGridListModule, TranslateModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);
  private userManagementService = inject(UserManagementService);
  private userService = inject(UserService);
  private jobOfferService = inject(JobOfferService);
  private projectService = inject(ProjectService);
  private kycService = inject(KycService);
  private recommendationService = inject(RecommendationService);
  private posthogService = inject(PosthogService);
  private quoteService = inject(QuoteService);

  user = this.authService.getCurrentUser();

  // Motivational quote (from Quotable API)
  dailyQuote = signal<Quote | null>(null);

  stats = signal({
    totalUsers: 0,
    activeProjects: 0,
    totalRevenue: 0,
    pendingKyc: 0,
    freelancers: 0,
    clients: 0,
    completedProjects: 0,
    avgRating: 0,
    totalJobOffers: 0,
    openJobOffers: 0,
    totalApplications: 0,
    totalRecommendations: 0,
    pendingRecommendations: 0,
    overdueProjects: 0,
    totalBadges: 0,
    reportedEvaluations: 0,
    evaluationsWithResponses: 0,
    totalEvaluations: 0
  });

  recentUsers = signal<UserListItem[]>([]);
  recentJobs = signal<any[]>([]);
  isLoadingStats = signal(true);

  ngOnInit(): void {
    this.posthogService.trackPageView('admin_dashboard');
    this.loadRecentUsers();
    this.loadDashboardStats();
    this.loadRecentJobs();
    this.loadDailyQuote();
  }

  private loadDailyQuote(): void {
    this.quoteService.getRandomQuote().subscribe(quote => {
      this.dailyQuote.set(quote);
    });
  }

  loadDashboardStats(): void {
    forkJoin({
      userStats: this.userService.getUserStats(),
      jobStats: this.jobOfferService.getJobOfferStats(),
      projects: this.projectService.getAllProjects(),
      kycPending: this.kycService.getPendingVerifications(),
      allJobs: this.jobOfferService.getAllJobOffers(),
      recommendations: this.recommendationService.getAllRecommendations()
    }).subscribe({
      next: ({ userStats, jobStats, projects, kycPending, allJobs, recommendations }) => {
        const activeProjects = projects.filter((p: any) => p.status === 'ACTIVE').length;
        const completedProjects = projects.filter((p: any) => p.status === 'COMPLETED').length;
        const overdueProjects = projects.filter((p: any) => {
          if (p.status === 'ACTIVE' && p.endDate) {
            return new Date(p.endDate) < new Date();
          }
          return false;
        }).length;
        const totalBudget = allJobs.reduce((sum: number, j: any) => sum + (j.budget || 0), 0);
        const openJobOffers = allJobs.filter((j: any) => j.status === 'OPEN').length;
        const totalApplications = allJobs.reduce((sum: number, j: any) => sum + (j.applicantCount || 0), 0);
        const pendingRecommendations = recommendations.filter((r: any) => r.status === 'PENDING').length;

        this.stats.set({
          totalUsers: userStats.totalUsers || 0,
          activeProjects,
          totalRevenue: totalBudget,
          pendingKyc: Array.isArray(kycPending) ? kycPending.length : 0,
          freelancers: userStats.freelancers || 0,
          clients: userStats.clients || 0,
          completedProjects,
          avgRating: 0,
          totalJobOffers: allJobs.length,
          openJobOffers,
          totalApplications,
          totalRecommendations: recommendations.length,
          pendingRecommendations,
          overdueProjects,
          totalBadges: 0,
          reportedEvaluations: 0,
          evaluationsWithResponses: 0,
          totalEvaluations: 0
        });
        this.isLoadingStats.set(false);

        // Track dashboard KPIs to PostHog
        this.posthogService.trackDashboardView(this.stats());
      },
      error: (err) => {
        console.error('Error loading dashboard stats:', err);
        this.loadStatsIndividually();
      }
    });
  }

  private loadStatsIndividually(): void {
    this.userService.getUserStats().subscribe({
      next: (userStats) => {
        this.stats.update(s => ({
          ...s,
          totalUsers: userStats.totalUsers || 0,
          freelancers: userStats.freelancers || 0,
          clients: userStats.clients || 0
        }));
      },
      error: (err) => console.error('Error loading user stats:', err)
    });

    this.projectService.getAllProjects().subscribe({
      next: (projects) => {
        this.stats.update(s => ({
          ...s,
          activeProjects: projects.filter((p: any) => p.status === 'ACTIVE').length,
          completedProjects: projects.filter((p: any) => p.status === 'COMPLETED').length
        }));
      },
      error: (err) => console.error('Error loading project stats:', err)
    });

    this.kycService.getPendingVerifications().subscribe({
      next: (pending) => {
        this.stats.update(s => ({
          ...s,
          pendingKyc: Array.isArray(pending) ? pending.length : 0
        }));
      },
      error: (err) => console.error('Error loading KYC stats:', err)
    });

    this.jobOfferService.getAllJobOffers().subscribe({
      next: (jobs) => {
        this.stats.update(s => ({
          ...s,
          totalJobOffers: jobs.length,
          openJobOffers: jobs.filter((j: any) => j.status === 'OPEN').length,
          totalApplications: jobs.reduce((sum: number, j: any) => sum + (j.applicantCount || 0), 0),
          totalRevenue: jobs.reduce((sum: number, j: any) => sum + (j.budget || 0), 0)
        }));
      },
      error: (err) => console.error('Error loading job stats:', err)
    });

    this.recommendationService.getAllRecommendations().subscribe({
      next: (recs) => {
        this.stats.update(s => ({
          ...s,
          totalRecommendations: recs.length,
          pendingRecommendations: recs.filter((r: any) => r.status === 'PENDING').length
        }));
      },
      error: (err) => console.error('Error loading recommendation stats:', err)
    });

    this.isLoadingStats.set(false);
  }

  loadRecentJobs(): void {
    this.jobOfferService.getAllJobOffers().subscribe({
      next: (jobs) => {
        this.recentJobs.set(
          jobs
            .sort((a: any, b: any) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
            .slice(0, 5)
        );
      },
      error: () => {}
    });
  }

  loadRecentUsers(): void {
    this.userManagementService.getUsers({ page: 0, size: 6 }).subscribe({
      next: (response) => {
        this.recentUsers.set(response.users);
      },
      error: (err) => {
        console.error('Error loading recent users:', err);
      }
    });
  }

  getInitials(user: UserListItem): string {
    return (user.firstName?.charAt(0) || '') + (user.lastName?.charAt(0) || '');
  }

  // Navigation existante
  navigateToUsers(): void {
    this.router.navigate(['/backoffice/admin/users']);
  }

  navigateToKyc(): void {
    this.router.navigate(['/backoffice/admin/kyc']);
  }

  navigateToAuditLog(): void {
    this.router.navigate(['/backoffice/admin/audit-log']);
  }

  navigateToJobs(): void {
    this.router.navigate(['/backoffice/admin/jobs']);
  }

  navigateToProjects(): void {
    this.router.navigate(['/backoffice/admin/projects']);
  }

  navigateToRecommendations(): void {
    this.router.navigate(['/backoffice/admin/recommendations']);
  }

  navigateToJobAnalytics(): void {
    this.router.navigate(['/backoffice/admin/analytics/jobs']);
  }

  navigateToMilestoneAnalytics(): void {
    this.router.navigate(['/backoffice/admin/analytics/milestones']);
  }

  navigateToRecommendationAnalytics(): void {
    this.router.navigate(['/backoffice/admin/analytics/recommendations']);
  }

  // NOUVELLES NAVIGATIONS POUR LES COMPOSANTS AJOUTÉS
  navigateToBadges(): void {
    this.router.navigate(['/backoffice/admin/badges']);
  }

  navigateToEvaluations(): void {
    this.router.navigate(['/backoffice/admin/evaluations']);
  }

  navigateToReportedEvaluations(): void {
    this.router.navigate(['/backoffice/admin/reported']);
  }

  navigateToResponses(): void {
    this.router.navigate(['/backoffice/admin/responses']);
  }
}