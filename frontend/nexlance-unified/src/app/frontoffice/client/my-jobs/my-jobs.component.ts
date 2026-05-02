import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MatIconModule } from '@angular/material/icon';
import { JobOfferService } from '@core/services/job-offer.service';
import { JobOffer, JobOfferStatus } from '@core/models/job-offer.model';

@Component({
  selector: 'app-my-jobs',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule, MatIconModule],
  templateUrl: './my-jobs.component.html',
  styleUrls: ['./my-jobs.component.scss']
})
export class MyJobsComponent implements OnInit {
  jobs: JobOffer[] = [];
  filteredJobs: JobOffer[] = [];
  isLoading = true;
  selectedStatus: string = 'all';
  
  statusOptions = [
    { value: 'all', label: 'All' },
    { value: JobOfferStatus.DRAFT, label: 'Drafts' },
    { value: JobOfferStatus.OPEN, label: 'Open' },
    { value: JobOfferStatus.IN_PROGRESS, label: 'In Progress' },
    { value: JobOfferStatus.COMPLETED, label: 'Completed' },
    { value: JobOfferStatus.CANCELLED, label: 'Cancelled' },
    { value: JobOfferStatus.ARCHIVED, label: 'Archived' }
  ];

  constructor(
    private jobOfferService: JobOfferService,
    private router: Router,
    private translate: TranslateService
  ) {}

  ngOnInit(): void {
    this.loadMyJobs();
  }

  loadMyJobs(): void {
    this.isLoading = true;
    this.jobOfferService.getMyJobOffers().subscribe({
      next: (jobs: any) => {
        this.jobs = jobs; // Direct array
        this.filterJobs();
        this.isLoading = false;
      },
      error: (error: any) => {
        console.error('Error loading jobs:', error);
        this.isLoading = false;
      }
    });
  }

  filterJobs(): void {
    if (this.selectedStatus === 'all') {
      this.filteredJobs = this.jobs;
    } else {
      this.filteredJobs = this.jobs.filter(job => job.status === this.selectedStatus);
    }
  }

  onStatusFilterChange(status: string): void {
    this.selectedStatus = status;
    this.filterJobs();
  }

  getStatusBadgeClass(status: JobOfferStatus): string {
    const classes: { [key: string]: string } = {
      [JobOfferStatus.DRAFT]: 'badge-draft',
      [JobOfferStatus.OPEN]: 'badge-open',
      [JobOfferStatus.IN_PROGRESS]: 'badge-in-progress',
      [JobOfferStatus.COMPLETED]: 'badge-completed',
      [JobOfferStatus.CANCELLED]: 'badge-cancelled',
      [JobOfferStatus.ARCHIVED]: 'badge-archived'
    };
    return classes[status] || 'badge-default';
  }

  getStatusLabel(status: JobOfferStatus): string {
    return this.translate.instant(`jobs.${status}`);
  }

  viewJobDetails(jobId: string): void {
    this.router.navigate(['/frontoffice/client/my-jobs', jobId]);
  }

  editJob(jobId: string, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/frontoffice/client/edit-job', jobId]);
  }

  archiveJob(jobId: string, event: Event): void {
    event.stopPropagation();
    if (confirm('Are you sure you want to archive this offer?')) {
      this.jobOfferService.archiveJobOffer(jobId).subscribe({
        next: () => {
          alert('Offer archived successfully');
          this.loadMyJobs();
        },
        error: (error: any) => {
          console.error('Error archiving job:', error);
          alert('Error archiving the offer');
        }
      });
    }
  }

  deleteJob(jobId: string, event: Event): void {
    event.stopPropagation();
    if (confirm('Are you sure you want to delete this offer? This action cannot be undone.')) {
      this.jobOfferService.deleteJobOffer(jobId).subscribe({
        next: () => {
          alert('Offer deleted successfully');
          this.loadMyJobs();
        },
        error: (error: any) => {
          console.error('Error deleting job:', error);
          alert('Error deleting the offer');
        }
      });
    }
  }

  createNewJob(): void {
    this.router.navigate(['/frontoffice/client/create-job']);
  }

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  getCategoryIcon(category: string): string {
    const icons: { [key: string]: string } = {
      'development': '💻',
      'design': '🎨',
      'marketing': '📢',
      'writing': '✍️',
      'other': '📋'
    };
    return icons[category] || '📋';
  }
}
