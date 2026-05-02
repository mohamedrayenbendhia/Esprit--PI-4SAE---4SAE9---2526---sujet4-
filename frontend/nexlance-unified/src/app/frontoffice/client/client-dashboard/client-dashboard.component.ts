import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatGridListModule } from '@angular/material/grid-list';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { JobOfferService } from '../../../core/services/job-offer.service';
import { ProjectService } from '../../../core/services/project.service';
import { RecommendationService } from '../../../core/services/recommendation.service';
import { FreelanceProfileService } from '../../../core/services/freelance-profile.service';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-client-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatGridListModule,
    TranslateModule
  ],
  templateUrl: './client-dashboard.component.html',
  styleUrls: ['./client-dashboard.component.scss']
})
export class ClientDashboardComponent implements OnInit {
  private authService = inject(AuthService);
  private userService = inject(UserService);
  private jobOfferService = inject(JobOfferService);
  private projectService = inject(ProjectService);
  private recommendationService = inject(RecommendationService);
  private freelanceProfileService = inject(FreelanceProfileService);
  private router = inject(Router);

  user = this.authService.getCurrentUser();
  avatarUrl = signal<string | null>(null);
  
  // Client statistics - loaded from backend
  stats = signal({
    activeProjects: 0,
    totalSpent: 0,
    freelancersHired: 0,
    completedProjects: 0,
    activeJobs: 0,
    totalApplicationsReceived: 0,
    pendingInvites: 0
  });

  isLoadingStats = signal(true);

  // Recent jobs loaded from backend
  recentJobs = signal<any[]>([]);

  // Top freelancers loaded from backend
  recommendedFreelancers = signal<any[]>([]);

  ngOnInit(): void {
    // Fetch full user data from API to get avatar
    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        if (user.avatar) {
          this.avatarUrl.set(user.avatar);
          this.authService.updateUserSession(user as any);
        }
      },
      error: (err) => console.error('Error fetching user data:', err)
    });

    this.loadDashboardStats();
    this.loadRecommendedFreelancers();
    this.loadRecentJobs();
  }

  loadDashboardStats(): void {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.id) {
      this.isLoadingStats.set(false);
      return;
    }

    forkJoin({
      projects: this.projectService.getProjectsByClientId(currentUser.id),
      jobs: this.jobOfferService.getClientJobOffers(currentUser.id)
    }).subscribe({
      next: ({ projects, jobs }) => {
        const activeProjects = projects.filter((p: any) => p.status === 'ACTIVE').length;
        const completedProjects = projects.filter((p: any) => p.status === 'COMPLETED').length;
        const totalSpent = jobs.reduce((sum: number, j: any) => sum + (j.budget || 0), 0);
        const activeJobs = jobs.filter((j: any) => j.status === 'OPEN' || j.status === 'IN_PROGRESS').length;
        const totalApplicationsReceived = jobs.reduce((sum: number, j: any) => sum + (j.applicantCount || 0), 0);
        // Count unique freelancers from completed projects
        const uniqueFreelancers = new Set(projects.filter((p: any) => p.freelanceId).map((p: any) => p.freelanceId));

        this.stats.set({
          activeProjects,
          totalSpent,
          freelancersHired: uniqueFreelancers.size,
          completedProjects,
          activeJobs,
          totalApplicationsReceived,
          pendingInvites: 0
        });
        this.isLoadingStats.set(false);
      },
      error: (err) => {
        console.error('Error loading dashboard stats:', err);
        this.isLoadingStats.set(false);
      }
    });

    // Load pending invites count
    this.recommendationService.getRecommendationsByClientId(currentUser.id as any).subscribe({
      next: (recs) => {
        const pending = recs.filter((r: any) => r.status === 'PENDING').length;
        this.stats.update(s => ({ ...s, pendingInvites: pending }));
      },
      error: () => {}
    });
  }

  loadRecommendedFreelancers(): void {
    this.freelanceProfileService.getAllProfiles(0, 3).subscribe({
      next: (profiles: any) => {
        const list = Array.isArray(profiles) ? profiles : (profiles?.content || []);
        this.recommendedFreelancers.set(list.slice(0, 3).map((p: any) => ({
          name: p.title || p.firstName || 'Freelancer',
          skill: Array.isArray(p.skills) ? (p.skills[0] || 'N/A') : (p.title || 'N/A'),
          rating: p.completionRate ? (p.completionRate / 20).toFixed(1) : '4.5',
          hourlyRate: p.hourlyRate || 0
        })));
      },
      error: (err) => {
        console.error('Error loading freelancers:', err);
      }
    });
  }

  loadRecentJobs(): void {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.id) return;
    this.jobOfferService.getClientJobOffers(currentUser.id).subscribe({
      next: (jobs: any[]) => {
        this.recentJobs.set(
          jobs
            .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
            .slice(0, 5)
        );
      },
      error: () => {}
    });
  }

  navigateToProfile(): void {
    this.router.navigate(['/profile']);
  }

  navigateToFindFreelancers(): void {
    this.router.navigate(['/frontoffice/client/freelancers']);
  }

  navigateToProjects(): void {
    this.router.navigate(['/frontoffice/client/projects']);
  }

  navigateToCreateJob(): void {
    this.router.navigate(['/frontoffice/client/create-job']);
  }

  navigateToMyJobs(): void {
    this.router.navigate(['/frontoffice/client/my-jobs']);
  }

  navigateToRecommendations(): void {
    this.router.navigate(['/frontoffice/client/my-recommendations']);
  }

  viewJobDetails(jobId: string): void {
    this.router.navigate(['/frontoffice/client/my-jobs', jobId]);
  }
}
