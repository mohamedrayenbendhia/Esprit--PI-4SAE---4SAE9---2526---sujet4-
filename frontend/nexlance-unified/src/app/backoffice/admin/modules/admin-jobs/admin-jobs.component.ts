import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { JobOfferService } from '@core/services/job-offer.service';
import { JobOffer, JobOfferFilters, JobOfferStatus } from '@core/models/job-offer.model';

@Component({
  selector: 'app-admin-jobs',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, MatIconModule, TranslateModule],
  templateUrl: './admin-jobs.component.html',
  styleUrls: ['./admin-jobs.component.scss']
})
export class AdminJobsComponent implements OnInit {
  jobs: JobOffer[] = [];
  isLoading = true;
  searchQuery = '';
  
  filters: JobOfferFilters = {
    page: 0,
    size: 20
  };

  totalElements = 0;
  totalPages = 0;

  get openJobsCount(): number {
    return this.jobs.filter(j => j.status === JobOfferStatus.OPEN).length;
  }

  get inProgressJobsCount(): number {
    return this.jobs.filter(j => j.status === JobOfferStatus.IN_PROGRESS).length;
  }

  get completedJobsCount(): number {
    return this.jobs.filter(j => j.status === JobOfferStatus.COMPLETED).length;
  }

  constructor(
    private jobOfferService: JobOfferService,
    private router: Router,
    private translate: TranslateService
  ) {}

  ngOnInit(): void {
    this.loadJobs();
  }

  loadJobs(): void {
    this.isLoading = true;
    
    if (this.searchQuery) {
      this.jobOfferService.searchJobOffers(this.searchQuery, this.filters).subscribe({
        next: (jobs: any) => {
          this.jobs = jobs; // Direct array
          this.totalElements = jobs.length;
          this.totalPages = 1; // No backend pagination yet
          this.isLoading = false;
        },
        error: (error: any) => {
          console.error('Error searching jobs:', error);
          this.isLoading = false;
        }
      });
    } else {
      this.jobOfferService.getAllJobOffers(this.filters).subscribe({
        next: (jobs: any) => {
          this.jobs = jobs; // Direct array
          this.totalElements = jobs.length;
          this.totalPages = 1; // No backend pagination yet
          this.isLoading = false;
        },
        error: (error: any) => {
          console.error('Error loading jobs:', error);
          this.isLoading = false;
        }
      });
    }
  }

  onSearch(): void {
    this.filters.page = 0;
    this.loadJobs();
  }

  onFilterChange(): void {
    this.filters.page = 0;
    this.loadJobs();
  }

  viewJobDetails(jobId: string): void {
    this.router.navigate(['/backoffice/admin/jobs', jobId]);
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

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  exportToCSV(): void {
    const csvData = this.convertToCSV(this.jobs);
    const blob = new Blob([csvData], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `job_offers_${new Date().toISOString()}.csv`;
    link.click();
  }

  private convertToCSV(jobs: JobOffer[]): string {
    const headers = ['ID', 'Title', 'Client', 'Category', 'Budget', 'Status', 'Applications', 'Views', 'Published Date'];
    const rows = jobs.map(job => [
      job.id,
      job.title,
      `${job.client?.firstName || ''} ${job.client?.lastName || ''}`,
      job.category,
      job.budget,
      job.status,
      job.applicantCount,
      job.viewCount,
      this.formatDate(job.publishedAt || job.createdAt)
    ]);

    return [headers, ...rows].map(row => row.join(',')).join('\n');
  }

  nextPage(): void {
    if (this.filters.page! < this.totalPages - 1) {
      this.filters.page!++;
      this.loadJobs();
    }
  }

  previousPage(): void {
    if (this.filters.page! > 0) {
      this.filters.page!--;
      this.loadJobs();
    }
  }
}
