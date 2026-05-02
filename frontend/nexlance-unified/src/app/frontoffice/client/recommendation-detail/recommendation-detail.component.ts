import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { RecommendationService, Recommendation, RecommendationStatus } from '../../../core/services/recommendation.service';
import { JobOfferService } from '../../../core/services/job-offer.service';
import { UserService } from '../../../core/services/user.service';
import { FreelanceProfileService } from '../../../core/services/freelance-profile.service';
import { ProjectService } from '../../../core/services/project.service';
import { JobOffer } from '../../../core/models/job-offer.model';
import { User } from '../../../shared/models/user.model';
import { CreateProjectRequest, MilestoneStatus } from '../../../core/models/project.model';
import { CreateProjectDialogComponent, CreateProjectDialogResult } from '../job-detail-client/create-project-dialog/create-project-dialog.component';
import { AuthService } from '../../../core/services/auth.service';
import { forkJoin, of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-recommendation-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule,
    TranslateModule
  ],
  templateUrl: './recommendation-detail.component.html',
  styleUrl: './recommendation-detail.component.scss'
})
export class RecommendationDetailComponent implements OnInit {
  recommendation?: Recommendation;
  jobOffer?: JobOffer;
  freelancer?: User;
  isLoading = true;
  isProcessing = false;
  RecommendationStatus = RecommendationStatus;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private recommendationService: RecommendationService,
    private jobOfferService: JobOfferService,
    private userService: UserService,
    private freelanceProfileService: FreelanceProfileService,
    private projectService: ProjectService,
    private authService: AuthService,
    private snackBar: MatSnackBar,
    private translate: TranslateService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadRecommendationDetails(parseInt(id, 10));
    }
  }

  loadRecommendationDetails(id: number): void {
    this.isLoading = true;
    
    this.recommendationService.getRecommendationById(id).subscribe({
      next: (recommendation) => {
        this.recommendation = recommendation;
        
        // Load related data (with individual error handling for resilience)
        // For freelancer: try User API first, fallback to FreelanceProfile API
        const freelancerObs = this.userService.getUserById(recommendation.freelanceId.toString()).pipe(
          catchError(() =>
            this.freelanceProfileService.getProfileById(recommendation.freelanceId.toString()).pipe(
              switchMap(profile => profile?.userId
                ? this.userService.getUserById(profile.userId).pipe(catchError(() => of({ firstName: profile.firstName, lastName: profile.lastName, email: profile.email } as User)))
                : of({ firstName: profile.firstName, lastName: profile.lastName, email: profile.email } as User)
              ),
              catchError(() => of(null))
            )
          )
        );
        forkJoin({
          jobOffer: this.jobOfferService.getJobOfferById(recommendation.jobOfferId.toString()).pipe(catchError(() => of(null))),
          freelancer: freelancerObs
        }).subscribe({
          next: (data) => {
            this.jobOffer = data.jobOffer as JobOffer;
            this.freelancer = data.freelancer as User;
            this.isLoading = false;
          },
          error: (error) => {
            console.error('Error loading related data:', error);
            this.isLoading = false;
          }
        });
      },
      error: (error) => {
        console.error('Error loading recommendation:', error);
        this.showMessage('recommendations.errors.loadFailed', true);
        this.isLoading = false;
        this.goBack();
      }
    });
  }

  cancelRecommendation(): void {
    if (!this.recommendation) return;

    const confirmation = confirm(this.translate.instant('recommendations.confirmCancel'));
    if (!confirmation) return;

    this.isProcessing = true;
    this.recommendationService.cancelRecommendation(this.recommendation.id).subscribe({
      next: () => {
        this.showMessage('recommendations.cancelSuccess', false);
        this.goBack();
      },
      error: (error) => {
        console.error('Error canceling recommendation:', error);
        this.showMessage('recommendations.errors.cancelFailed', true);
        this.isProcessing = false;
      }
    });
  }

  sendReminder(): void {
    if (!this.recommendation) return;

    this.isProcessing = true;
    this.recommendationService.sendReminder(this.recommendation.id).subscribe({
      next: (updated) => {
        this.recommendation = updated;
        this.showMessage('recommendations.reminderSent', false);
        this.isProcessing = false;
      },
      error: (error) => {
        console.error('Error sending reminder:', error);
        this.showMessage('recommendations.errors.reminderFailed', true);
        this.isProcessing = false;
      }
    });
  }

  viewJobOffer(): void {
    if (this.recommendation) {
      this.router.navigate(['/frontoffice/client/my-jobs', this.recommendation.jobOfferId]);
    }
  }

  viewFreelancerProfile(): void {
    if (this.recommendation) {
      this.router.navigate(['/frontoffice/client/freelancers', this.recommendation.freelanceId]);
    }
  }

  getStatusColor(status: RecommendationStatus): string {
    switch (status) {
      case RecommendationStatus.PENDING:
        return 'warn';
      case RecommendationStatus.ACCEPTED:
        return 'primary';
      case RecommendationStatus.REJECTED:
      case RecommendationStatus.CANCELLED:
        return 'accent';
      default:
        return '';
    }
  }

  getStatusIcon(status: RecommendationStatus): string {
    switch (status) {
      case RecommendationStatus.PENDING:
        return 'schedule';
      case RecommendationStatus.ACCEPTED:
        return 'check_circle';
      case RecommendationStatus.REJECTED:
        return 'cancel';
      case RecommendationStatus.CANCELLED:
        return 'block';
      case RecommendationStatus.EXPIRED:
        return 'access_time';
      default:
        return 'help';
    }
  }

  canCancel(): boolean {
    return this.recommendation?.status === RecommendationStatus.PENDING;
  }

  canSendReminder(): boolean {
    if (!this.recommendation) return false;
    return this.recommendation.status === RecommendationStatus.PENDING && 
           !this.recommendation.isReminderSent;
  }

  showMessage(key: string, isError: boolean): void {
    this.translate.get(key).subscribe((message: string) => {
      this.snackBar.open(message, 'OK', {
        duration: 5000,
        panelClass: isError ? ['error-snackbar'] : ['success-snackbar']
      });
    });
  }

  openCreateProjectDialog(): void {
    if (!this.recommendation) return;

    const dialogRef = this.dialog.open(CreateProjectDialogComponent, {
      width: '720px',
      disableClose: true,
      data: {
        jobOffer: this.jobOffer || { title: 'Project', id: this.recommendation.jobOfferId },
        application: {
          freelanceId: this.recommendation.freelanceId.toString(),
          freelance: this.freelancer || null,
          proposedRate: this.recommendation.proposedBudget || 0
        }
      }
    });

    dialogRef.afterClosed().subscribe((result: CreateProjectDialogResult | null) => {
      if (!result || !this.recommendation) return;

      const currentUser = this.authService.getCurrentUser();
      const projectRequest: CreateProjectRequest = {
        jobOfferId: this.recommendation.jobOfferId.toString(),
        freelanceId: this.recommendation.freelanceId.toString(),
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
        next: () => {
          this.showMessage('Project created successfully!', false);
        },
        error: (err) => {
          console.error('Error creating project:', err);
          this.showMessage('Error creating project', true);
        }
      });
    });
  }

  goBack(): void {
    this.router.navigate(['/frontoffice/client/my-recommendations']);
  }
}
