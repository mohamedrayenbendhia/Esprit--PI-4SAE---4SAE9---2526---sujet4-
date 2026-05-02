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
import { ProjectService } from '../../../core/services/project.service';
import { ApplicationService } from '../../../core/services/application.service';
import { FreelanceInvitationService } from '../../../core/services/freelance-invitation.service';
import { JobOfferService } from '../../../core/services/job-offer.service';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-freelancer-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule, 
    MatButtonModule, 
    MatIconModule,
    MatGridListModule,
    TranslateModule
  ],
  templateUrl: './freelancer-dashboard.component.html',
  styleUrls: ['./freelancer-dashboard.component.scss']
})
export class FreelancerDashboardComponent implements OnInit {
  private authService = inject(AuthService);
  private userService = inject(UserService);
  private projectService = inject(ProjectService);
  private applicationService = inject(ApplicationService);
  private invitationService = inject(FreelanceInvitationService);
  private jobOfferService = inject(JobOfferService);
  private router = inject(Router);

  user = this.authService.getCurrentUser();
  avatarUrl = signal<string | null>(null);
  isLoadingStats = signal(true);
  
  // Freelancer statistics - loaded from backend
  stats = signal({
    totalProjects: 0,
    activeProjects: 0,
    completedProjects: 0,
    totalApplications: 0,
    pendingApplications: 0,
    acceptedApplications: 0,
    pendingInvitations: 0,
    successRate: 0
  });

  // Recent applications from backend
  recentApplications = signal<any[]>([]);

  // Available jobs count
  availableJobsCount = signal(0);

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
    this.loadAvailableJobsCount();
  }

  loadDashboardStats(): void {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.id) {
      this.isLoadingStats.set(false);
      return;
    }

    forkJoin({
      projects: this.projectService.getProjectsByFreelanceId(currentUser.id),
      applications: this.applicationService.getMyApplications({ freelanceId: currentUser.id })
    }).subscribe({
      next: ({ projects, applications }) => {
        const totalProjects = projects.length;
        const activeProjects = projects.filter((p: any) => p.status === 'ACTIVE').length;
        const completedProjects = projects.filter((p: any) => p.status === 'COMPLETED').length;
        const totalApps = applications.length;
        const pendingApps = applications.filter((a: any) => a.status === 'PENDING').length;
        const acceptedApps = applications.filter((a: any) => a.status === 'ACCEPTED').length;
        const successRate = totalProjects > 0 ? Math.round((completedProjects / totalProjects) * 100) : 0;

        this.stats.set({
          totalProjects,
          activeProjects,
          completedProjects,
          totalApplications: totalApps,
          pendingApplications: pendingApps,
          acceptedApplications: acceptedApps,
          pendingInvitations: 0,
          successRate
        });

        this.recentApplications.set(
          applications
            .sort((a: any, b: any) => new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime())
            .slice(0, 3)
        );

        this.isLoadingStats.set(false);
      },
      error: (err) => {
        console.error('Error loading dashboard stats:', err);
        this.isLoadingStats.set(false);
      }
    });

    // Load pending invitations count
    this.invitationService.getReceivedInvitations().subscribe({
      next: (invitations) => {
        const pending = invitations.filter((i: any) => i.status === 'PENDING').length;
        this.stats.update(s => ({ ...s, pendingInvitations: pending }));
      },
      error: () => {}
    });
  }

  loadAvailableJobsCount(): void {
    this.jobOfferService.getActiveJobOffers().subscribe({
      next: (jobs) => this.availableJobsCount.set(jobs.length),
      error: () => {}
    });
  }

  navigateToProfile(): void {
    this.router.navigate(['/profile/professional']);
  }

  navigateToKyc(): void {
    this.router.navigate(['/profile/kyc']);
  }

  navigateToBrowseJobs(): void {
    this.router.navigate(['/frontoffice/freelancer/browse-jobs']);
  }

  navigateToMyApplications(): void {
    this.router.navigate(['/frontoffice/freelancer/my-applications']);
  }

  navigateToMyInvitations(): void {
    this.router.navigate(['/frontoffice/freelancer/my-invitations']);
  }

  navigateToMyProjects(): void {
    this.router.navigate(['/frontoffice/freelancer/my-projects']);
  }
}
