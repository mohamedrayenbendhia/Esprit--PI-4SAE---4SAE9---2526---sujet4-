import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { TranslateModule } from '@ngx-translate/core';
import { JobOfferService } from '../../../../core/services/job-offer.service';
import { ApplicationService } from '../../../../core/services/application.service';
import { UserService } from '../../../../core/services/user.service';
import { JobOffer, JobOfferStatus } from '../../../../core/models/job-offer.model';
import { Application } from '../../../../core/models/application.model';
import { User } from '../../../../shared/models/user.model';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-admin-job-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatSnackBarModule,
    MatMenuModule,
    MatDividerModule,
    TranslateModule
  ],
  templateUrl: './admin-job-detail.component.html',
  styleUrls: ['./admin-job-detail.component.scss']
})
export class AdminJobDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private jobOfferService = inject(JobOfferService);
  private applicationService = inject(ApplicationService);
  private userService = inject(UserService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  jobOffer = signal<JobOffer | null>(null);
  client = signal<User | null>(null);
  applications = signal<Application[]>([]);
  loading = signal(true);
  processing = signal(false);

  JobOfferStatus = JobOfferStatus;

  ngOnInit(): void {
    const jobId = this.route.snapshot.paramMap.get('id');
    if (jobId) {
      this.loadJobOfferDetails(jobId);
    } else {
      this.snackBar.open('Job offer ID not found', 'Close', { duration: 3000 });
      this.router.navigate(['/backoffice/admin/jobs']);
    }
  }

  loadJobOfferDetails(jobId: string): void {
    this.loading.set(true);
    
    this.jobOfferService.getJobOfferById(jobId).subscribe({
      next: (jobOffer) => {
        this.jobOffer.set(jobOffer);
        this.loadClientInfo(jobOffer.clientId);
        this.loadApplications(jobId);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading job offer:', err);
        this.snackBar.open('Error loading job offer details', 'Close', { duration: 3000 });
        this.loading.set(false);
        this.router.navigate(['/backoffice/admin/jobs']);
      }
    });
  }

  loadClientInfo(clientId: string): void {
    this.userService.getUserById(clientId).subscribe({
      next: (user) => this.client.set(user),
      error: (err) => console.error('Error loading client info:', err)
    });
  }

  loadApplications(jobOfferId: string): void {
    this.applicationService.getApplicationsByJobOffer(jobOfferId).subscribe({
      next: (applications) => this.applications.set(applications),
      error: (err) => console.error('Error loading applications:', err)
    });
  }

  getStatusIcon(status: JobOfferStatus): string {
    const icons = {
      [JobOfferStatus.DRAFT]: 'edit_note',
      [JobOfferStatus.OPEN]: 'work_outline',
      [JobOfferStatus.IN_PROGRESS]: 'pending_actions',
      [JobOfferStatus.COMPLETED]: 'check_circle',
      [JobOfferStatus.CANCELLED]: 'cancel',
      [JobOfferStatus.ARCHIVED]: 'archive'
    };
    return icons[status] || 'help_outline';
  }

  getStatusLabel(status: JobOfferStatus): string {
    const labels: { [key in JobOfferStatus]: string } = {
      [JobOfferStatus.DRAFT]: 'Brouillon',
      [JobOfferStatus.OPEN]: 'Ouverte',
      [JobOfferStatus.IN_PROGRESS]: 'En cours',
      [JobOfferStatus.COMPLETED]: 'Terminée',
      [JobOfferStatus.CANCELLED]: 'Annulée',
      [JobOfferStatus.ARCHIVED]: 'Archivée'
    };
    return labels[status] || status;
  }

  getCategoryIcon(category: string): string {
    const icons: { [key: string]: string } = {
      'DEVELOPMENT': 'code',
      'DESIGN': 'palette',
      'MARKETING': 'campaign',
      'WRITING': 'edit',
      'OTHER': 'category'
    };
    return icons[category] || 'category';
  }

  getExperienceLevelLabel(level: string): string {
    const labels: { [key: string]: string } = {
      'BEGINNER': 'Débutant',
      'INTERMEDIATE': 'Intermédiaire',
      'EXPERT': 'Expert'
    };
    return labels[level] || level;
  }

  getBudgetTypeLabel(type: string): string {
    return type === 'FIXED' ? 'Budget fixe' : 'Tarif horaire';
  }

  formatBudget(budget: number, budgetType: string): string {
    const formattedBudget = budget.toLocaleString('fr-FR');
    return budgetType === 'FIXED' 
      ? `${formattedBudget} €` 
      : `${formattedBudget} €/h`;
  }

  formatDate(date: Date | undefined): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString('fr-FR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  changeStatus(newStatus: JobOfferStatus): void {
    const jobOffer = this.jobOffer();
    if (!jobOffer) return;

    this.processing.set(true);
    this.jobOfferService.changeStatus(jobOffer.id, newStatus).subscribe({
      next: (updatedOffer) => {
        this.jobOffer.set(updatedOffer);
        this.snackBar.open(`Status changed to ${this.getStatusLabel(newStatus)}`, 'Close', { duration: 3000 });
        this.processing.set(false);
      },
      error: (err) => {
        console.error('Error changing status:', err);
        this.snackBar.open('Error changing status', 'Close', { duration: 3000 });
        this.processing.set(false);
      }
    });
  }

  archiveOffer(): void {
    this.changeStatus(JobOfferStatus.ARCHIVED);
  }

  cancelOffer(): void {
    this.changeStatus(JobOfferStatus.CANCELLED);
  }

  reopenOffer(): void {
    this.changeStatus(JobOfferStatus.OPEN);
  }

  deleteOffer(): void {
    const jobOffer = this.jobOffer();
    if (!jobOffer) return;

    if (confirm('Are you sure you want to delete this job offer? This action cannot be undone.')) {
      this.processing.set(true);
      this.jobOfferService.deleteJobOffer(jobOffer.id).subscribe({
        next: () => {
          this.snackBar.open('Job offer deleted successfully', 'Close', { duration: 3000 });
          this.router.navigate(['/backoffice/admin/jobs']);
        },
        error: (err) => {
          console.error('Error deleting job offer:', err);
          this.snackBar.open('Error deleting job offer', 'Close', { duration: 3000 });
          this.processing.set(false);
        }
      });
    }
  }

  viewApplication(application: Application): void {
    // Navigate to application detail
    this.router.navigate(['/admin/applications', application.id]);
  }

  viewClientProfile(): void {
    const client = this.client();
    if (client) {
      this.router.navigate(['/admin/users', client.id]);
    }
  }

  goBack(): void {
    this.router.navigate(['/backoffice/admin/jobs']);
  }

  getApplicationStatusClass(status: string): string {
    const classes: { [key: string]: string } = {
      'PENDING': 'status-pending',
      'ACCEPTED': 'status-accepted',
      'REJECTED': 'status-rejected',
      'WITHDRAWN': 'status-withdrawn'
    };
    return classes[status] || 'status-pending';
  }

  getApplicationStatusLabel(status: string): string {
    const labels: { [key: string]: string } = {
      'PENDING': 'En attente',
      'ACCEPTED': 'Acceptée',
      'REJECTED': 'Rejetée',
      'WITHDRAWN': 'Retirée'
    };
    return labels[status] || status;
  }
}
