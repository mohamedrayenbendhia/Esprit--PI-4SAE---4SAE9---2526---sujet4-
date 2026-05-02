import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Router, RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { forkJoin, of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { RecommendationService, Recommendation, RecommendationStatus } from '../../../core/services/recommendation.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { JobOfferService } from '../../../core/services/job-offer.service';
import { FreelanceProfileService } from '../../../core/services/freelance-profile.service';
import { ProjectService } from '../../../core/services/project.service';
import { User } from '../../../shared/models/user.model';
import { JobOffer } from '../../../core/models/job-offer.model';
import { CreateProjectRequest, MilestoneStatus } from '../../../core/models/project.model';
import { CreateProjectDialogComponent, CreateProjectDialogResult } from '../job-detail-client/create-project-dialog/create-project-dialog.component';

@Component({
  selector: 'app-my-recommendations',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatDialogModule,
    RouterModule,
    TranslateModule
  ],
  templateUrl: './my-recommendations.component.html',
  styleUrl: './my-recommendations.component.scss'
})
export class MyRecommendationsComponent implements OnInit {
  searchQuery = '';
  statusFilter = 'all';
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  
  // Make enum available in template
  RecommendationStatus = RecommendationStatus;

  statusOptions = [
    { value: 'all', label: 'All' },
    { value: 'PENDING', label: 'Pending' },
    { value: 'ACCEPTED', label: 'Accepted' },
    { value: 'REJECTED', label: 'Rejected' },
    { value: 'CANCELLED', label: 'Cancelled' }
  ];

  recommendations: Recommendation[] = [];
  filteredRecommendations: Recommendation[] = [];
  freelancerMap: { [id: string]: User } = {};
  jobOfferMap: { [id: string]: JobOffer } = {};

  constructor(
    private router: Router,
    private recommendationService: RecommendationService,
    private authService: AuthService,
    private userService: UserService,
    private jobOfferService: JobOfferService,
    private freelanceProfileService: FreelanceProfileService,
    private projectService: ProjectService,
    private dialog: MatDialog
  ) {}

  ngOnInit() {
    this.loadRecommendations();
  }

  loadRecommendations() {
    this.isLoading = true;
    this.errorMessage = '';
    
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser || !currentUser.id) {
      this.errorMessage = 'User not authenticated';
      this.isLoading = false;
      return;
    }

    // Use user ID directly as string
    const clientId = currentUser.id;
    
    this.recommendationService.getRecommendationsByClientId(clientId).subscribe({
      next: (data) => {
        this.recommendations = data;
        this.applyFilters();
        this.isLoading = false;
        this.loadFreelancersAndJobs(data);
      },
      error: (error) => {
        console.error('Error loading recommendations:', error);
        this.errorMessage = 'Failed to load recommendations';
        this.isLoading = false;
        this.recommendations = [];
        this.filteredRecommendations = [];
      }
    });
  }
  applyFilters() {
    this.filteredRecommendations = this.recommendations.filter(rec => {
      // Status filter
      if (this.statusFilter !== 'all' && rec.status !== this.statusFilter) {
        return false;
      }

      // Search filter - Note: Backend returns IDs, may need to fetch names separately
      if (this.searchQuery) {
        const query = this.searchQuery.toLowerCase();
        // Search in message or other fields since we may not have names directly
        return rec.message?.toLowerCase().includes(query) ||
               rec.id.toString().includes(query);
      }

      return true;
    });
  }

  onStatusFilterChange(status: string) {
    this.statusFilter = status;
    this.applyFilters();
  }

  onSearchChange(event: Event) {
    const input = event.target as HTMLInputElement;
    this.searchQuery = input.value;
    this.applyFilters();
  }

  viewDetail(recommendationId: number) {
    this.router.navigate(['/frontoffice/client/my-recommendations', recommendationId]);
  }

  sendReminder(recommendationId: number) {
    this.isLoading = true;
    this.recommendationService.sendReminder(recommendationId).subscribe({
      next: (updated) => {
        this.successMessage = 'Reminder sent successfully!';
        // Update the local recommendation
        const index = this.recommendations.findIndex(r => r.id === recommendationId);
        if (index !== -1) {
          this.recommendations[index] = updated;
          this.applyFilters();
        }
        this.isLoading = false;
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (error) => {
        console.error('Error sending reminder:', error);
        this.errorMessage = 'Failed to send reminder';
        this.isLoading = false;
        setTimeout(() => this.errorMessage = '', 3000);
      }
    });
  }

  cancelRecommendation(recommendationId: number) {
    if (!confirm('Are you sure you want to cancel this recommendation?')) {
      return;
    }

    const reason = prompt('Please provide a reason for cancellation (optional):');
    
    this.isLoading = true;
    this.recommendationService.cancelRecommendation(recommendationId, reason || undefined).subscribe({
      next: (updated) => {
        this.successMessage = 'Recommendation cancelled successfully!';
        // Update the local recommendation
        const index = this.recommendations.findIndex(r => r.id === recommendationId);
        if (index !== -1) {
          this.recommendations[index] = updated;
          this.applyFilters();
        }
        this.isLoading = false;
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (error) => {
        console.error('Error cancelling recommendation:', error);
        this.errorMessage = 'Failed to cancel recommendation';
        this.isLoading = false;
        setTimeout(() => this.errorMessage = '', 3000);
      }
    });
  }

  createProject(recommendationId: number) {
    const rec = this.recommendations.find(r => r.id === recommendationId);
    if (!rec) return;

    const freelancer = this.freelancerMap[rec.freelanceId];
    const jobOffer = this.jobOfferMap[rec.jobOfferId];

    const dialogRef = this.dialog.open(CreateProjectDialogComponent, {
      width: '720px',
      disableClose: true,
      data: {
        jobOffer: jobOffer || { title: 'Job #' + rec.jobOfferId, id: rec.jobOfferId },
        application: {
          freelanceId: rec.freelanceId.toString(),
          freelance: freelancer || null,
          proposedRate: rec.proposedBudget || 0
        }
      }
    });

    dialogRef.afterClosed().subscribe((result: CreateProjectDialogResult | null) => {
      if (!result) return;

      const currentUser = this.authService.getCurrentUser();
      const projectRequest: CreateProjectRequest = {
        jobOfferId: rec.jobOfferId.toString(),
        freelanceId: rec.freelanceId.toString(),
        clientId: currentUser?.id || '',
        title: result.title,
        startDate: new Date(result.startDate).toISOString(),
        endDate: new Date(result.endDate).toISOString(),
        requirements: result.requirements,
        deliverables: result.deliverables,
        milestones: result.milestones.map((m, index) => ({
          title: m.title,
          description: m.description,
          orderIndex: index + 1,
          dueDate: new Date(m.dueDate).toISOString(),
          status: MilestoneStatus.PENDING,
          deliverables: m.deliverables,
          acceptanceCriteria: m.acceptanceCriteria,
          requiresDocuments: m.requiresDocuments
        }))
      };

      this.projectService.createProject(projectRequest).subscribe({
        next: (project) => {
          this.successMessage = 'Project created successfully!';
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (err) => {
          console.error('Error creating project:', err);
          this.errorMessage = 'Error creating project';
          setTimeout(() => this.errorMessage = '', 3000);
        }
      });
    });
  }

  loadFreelancersAndJobs(recommendations: Recommendation[]): void {
    // Collect unique freelancer IDs and job offer IDs
    const freelancerIds = [...new Set(recommendations.map(r => r.freelanceId))];
    const jobOfferIds = [...new Set(recommendations.map(r => r.jobOfferId))];

    // Load all freelancers: try User API first, fallback to FreelanceProfile API
    if (freelancerIds.length > 0) {
      const freelancerRequests = freelancerIds.map(id =>
        this.userService.getUserById(id).pipe(
          catchError(() =>
            // Fallback: ID might be a FreelanceProfile ID, not a User ID
            this.freelanceProfileService.getProfileById(id).pipe(
              switchMap(profile => profile?.userId
                ? this.userService.getUserById(profile.userId).pipe(catchError(() => of({ firstName: profile.firstName, lastName: profile.lastName, email: profile.email } as User)))
                : of({ firstName: profile.firstName, lastName: profile.lastName, email: profile.email } as User)
              ),
              catchError(() => of(null))
            )
          )
        )
      );
      forkJoin(freelancerRequests).subscribe({
        next: (users) => {
          users.forEach((user, index) => {
            if (user) {
              this.freelancerMap[freelancerIds[index]] = user;
            }
          });
        }
      });
    }

    // Load all job offers (with individual error handling)
    if (jobOfferIds.length > 0) {
      const jobOfferRequests = jobOfferIds.map(id =>
        this.jobOfferService.getJobOfferById(id).pipe(catchError(() => of(null)))
      );
      forkJoin(jobOfferRequests).subscribe({
        next: (offers) => {
          offers.forEach((offer, index) => {
            if (offer) {
              this.jobOfferMap[jobOfferIds[index]] = offer;
            }
          });
        }
      });
    }
  }

  getFreelancerName(freelanceId: string): string {
    const user = this.freelancerMap[freelanceId];
    return user ? `${user.firstName} ${user.lastName}` : 'Freelancer';
  }

  getFreelancerEmail(freelanceId: string): string {
    const user = this.freelancerMap[freelanceId];
    return user?.email || '';
  }

  getFreelancerPhone(freelanceId: string): string {
    const user = this.freelancerMap[freelanceId];
    return user?.phoneNumber || 'N/A';
  }

  getJobOfferTitle(jobOfferId: string): string {
    const offer = this.jobOfferMap[jobOfferId];
    return offer?.title || 'Job #' + jobOfferId;
  }

  recommendForAnother(recommendationId: number) {
    console.log('Recommend freelancer for another offer:', recommendationId);
    // Open recommendation modal with this freelancer
  }

  getStatusIcon(status: RecommendationStatus): string {
    const icons: Record<string, string> = {
      'PENDING': 'hourglass_empty',
      'ACCEPTED': 'check_circle',
      'REJECTED': 'cancel',
      'CANCELLED': 'block',
      'EXPIRED': 'timer_off'
    };
    return icons[status] || 'info';
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING: 'Pending',
      ACCEPTED: 'Accepted',
      REJECTED: 'Rejected',
      CANCELLED: 'Cancelled',
      EXPIRED: 'Expired'
    };
    return labels[status] || status;
  }

  getStatusClass(status: RecommendationStatus): string {
    return `status-${status.toLowerCase()}`;
  }

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('en-US');
  }


}
