import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { RecommendationService, Recommendation, RecommendationStatus } from '../../../core/services/recommendation.service';
import { UserService } from '../../../core/services/user.service';
import { JobOfferService } from '../../../core/services/job-offer.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-freelancer-my-recommendations',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    TranslateModule,
    MatIconModule,
    MatCardModule,
    MatButtonModule,
    MatChipsModule,
    MatTabsModule,
    MatSnackBarModule
  ],
  templateUrl: './my-recommendations.component.html',
  styleUrls: ['./my-recommendations.component.scss']
})
export class FreelancerMyRecommendationsComponent implements OnInit {
  recommendations: Recommendation[] = [];
  filteredRecommendations: Recommendation[] = [];
  isLoading = true;
  isProcessing = false;
  activeTab = 'all';
  activeTabIndex = 0;
  RecommendationStatus = RecommendationStatus;

  clientMap: { [id: string]: any } = {};
  jobOfferMap: { [id: string]: any } = {};

  tabs = [
    { id: 'all', label: 'All' },
    { id: 'pending', label: 'Pending', status: RecommendationStatus.PENDING },
    { id: 'accepted', label: 'Accepted', status: RecommendationStatus.ACCEPTED },
    { id: 'rejected', label: 'Rejected', status: RecommendationStatus.REJECTED }
  ];

  constructor(
    private recommendationService: RecommendationService,
    private userService: UserService,
    private jobOfferService: JobOfferService,
    private authService: AuthService,
    private snackBar: MatSnackBar,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadRecommendations();
  }

  loadRecommendations(): void {
    this.isLoading = true;
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.id) {
      this.isLoading = false;
      return;
    }

    const userId = currentUser.id;

    // Fetch recommendations by User ID
    this.recommendationService.getRecommendationsByFreelanceId(userId).pipe(
      catchError(() => of([] as Recommendation[]))
    ).subscribe({
      next: (recommendations) => {
        this.recommendations = recommendations;
        this.filterRecommendations();
        this.populateRelations(recommendations);
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading recommendations:', error);
        this.isLoading = false;
      }
    });
  }

  private populateRelations(recommendations: Recommendation[]): void {
    const clientIds = [...new Set(recommendations.map(r => r.clientId))];
    const jobOfferIds = [...new Set(recommendations.map(r => r.jobOfferId))];

    if (clientIds.length > 0) {
      forkJoin(clientIds.map(id =>
        this.userService.getUserById(id).pipe(catchError(() => of(null)))
      )).subscribe({
        next: (users) => {
          users.forEach((user, idx) => {
            if (user) this.clientMap[clientIds[idx]] = user;
          });
        }
      });
    }

    if (jobOfferIds.length > 0) {
      forkJoin(jobOfferIds.map(id =>
        this.jobOfferService.getJobOfferById(id).pipe(catchError(() => of(null)))
      )).subscribe({
        next: (offers) => {
          offers.forEach((offer, idx) => {
            if (offer) this.jobOfferMap[jobOfferIds[idx]] = offer;
          });
        }
      });
    }
  }

  onTabChange(index: number): void {
    this.activeTab = this.tabs[index].id;
    this.filterRecommendations();
  }

  filterRecommendations(): void {
    const tab = this.tabs.find(t => t.id === this.activeTab);
    if (!tab || tab.id === 'all') {
      this.filteredRecommendations = this.recommendations;
    } else {
      this.filteredRecommendations = this.recommendations.filter(r => r.status === tab.status);
    }
  }

  getTabCount(tabId: string): number {
    const tab = this.tabs.find(t => t.id === tabId);
    if (!tab || tab.id === 'all') return this.recommendations.length;
    return this.recommendations.filter(r => r.status === tab.status).length;
  }

  getClientName(clientId: string): string {
    const user = this.clientMap[clientId];
    return user ? `${user.firstName} ${user.lastName}` : 'Client';
  }

  getJobOfferTitle(jobOfferId: string): string {
    const offer = this.jobOfferMap[jobOfferId];
    return offer?.title || 'Job Offer';
  }

  acceptRecommendation(rec: Recommendation, event: Event): void {
    event.stopPropagation();
    this.isProcessing = true;
    this.recommendationService.acceptRecommendation(rec.id).subscribe({
      next: (updated) => {
        const index = this.recommendations.findIndex(r => r.id === rec.id);
        if (index !== -1) this.recommendations[index] = updated;
        this.filterRecommendations();
        this.snackBar.open('Recommendation accepted!', 'OK', { duration: 3000, panelClass: ['success-snackbar'] });
        this.isProcessing = false;
      },
      error: (error) => {
        console.error('Error accepting recommendation:', error);
        this.snackBar.open('Error accepting recommendation', 'OK', { duration: 3000, panelClass: ['error-snackbar'] });
        this.isProcessing = false;
      }
    });
  }

  rejectRecommendation(rec: Recommendation, event: Event): void {
    event.stopPropagation();
    const reason = prompt('Reason for rejecting (optional):');
    this.isProcessing = true;
    this.recommendationService.rejectRecommendation(rec.id, reason || '').subscribe({
      next: (updated) => {
        const index = this.recommendations.findIndex(r => r.id === rec.id);
        if (index !== -1) this.recommendations[index] = updated;
        this.filterRecommendations();
        this.snackBar.open('Recommendation rejected', 'OK', { duration: 3000 });
        this.isProcessing = false;
      },
      error: (error) => {
        console.error('Error rejecting recommendation:', error);
        this.snackBar.open('Error rejecting recommendation', 'OK', { duration: 3000, panelClass: ['error-snackbar'] });
        this.isProcessing = false;
      }
    });
  }

  getStatusClass(status: RecommendationStatus): string {
    const classes: Record<string, string> = {
      'PENDING': 'badge-pending',
      'ACCEPTED': 'badge-accepted',
      'REJECTED': 'badge-rejected',
      'CANCELLED': 'badge-cancelled',
      'EXPIRED': 'badge-expired'
    };
    return classes[status] || 'badge-default';
  }

  getStatusLabel(status: RecommendationStatus): string {
    const labels: Record<string, string> = {
      'PENDING': 'Pending',
      'ACCEPTED': 'Accepted',
      'REJECTED': 'Rejected',
      'CANCELLED': 'Cancelled',
      'EXPIRED': 'Expired'
    };
    return labels[status] || status;
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

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  viewJobOffer(jobOfferId: string, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/frontoffice/freelancer/jobs', jobOfferId]);
  }
}
