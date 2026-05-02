import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MatIconModule } from '@angular/material/icon';
import { ApplicationService } from '@core/services/application.service';
import { AuthService } from '@core/services/auth.service';
import { JobOfferService } from '@core/services/job-offer.service';
import { Application, ApplicationStatus } from '@core/models/application.model';
import { forkJoin, of } from 'rxjs';

@Component({
  selector: 'app-my-applications',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule, MatIconModule],
  templateUrl: './my-applications.component.html',
  styleUrls: ['./my-applications.component.scss']
})
export class MyApplicationsComponent implements OnInit {
  applications: Application[] = [];
  filteredApplications: Application[] = [];
  isLoading = true;
  activeTab: string = 'all';

  tabs = [
    { id: 'all', label: 'All', status: null },
    { id: 'pending', label: 'Pending', status: ApplicationStatus.PENDING },
    { id: 'shortlisted', label: 'Shortlisted', status: ApplicationStatus.SHORTLISTED },
    { id: 'accepted', label: 'Accepted', status: ApplicationStatus.ACCEPTED },
    { id: 'rejected', label: 'Rejected', status: ApplicationStatus.REJECTED }
  ];

  constructor(
    private applicationService: ApplicationService,
    private authService: AuthService,
    private jobOfferService: JobOfferService,
    private router: Router,
    private translate: TranslateService
  ) {}

  ngOnInit(): void {
    this.loadMyApplications();
  }

  loadMyApplications(): void {
    this.isLoading = true;
    const currentUser = this.authService.getCurrentUser();
    
    // Filter by the current freelancer's ID
    const filters = currentUser?.id ? { freelanceId: currentUser.id } : {};
    
    this.applicationService.getMyApplications(filters).subscribe({
      next: (applications: any) => {
        this.applications = applications;
        
        // Enrich applications with job offer data
        this.enrichApplicationsWithJobData();
      },
      error: (error: any) => {
        console.error('Error loading applications:', error);
        this.isLoading = false;
      }
    });
  }

  /**
   * Fetch job offer details for each application and attach them
   */
  private enrichApplicationsWithJobData(): void {
    if (this.applications.length === 0) {
      this.filterApplications();
      this.isLoading = false;
      return;
    }

    // Get unique job offer IDs
    const uniqueJobOfferIds = [...new Set(this.applications.map(app => app.jobOfferId))];
    
    // Fetch all job offers in parallel
    const jobRequests = uniqueJobOfferIds.map(id =>
      this.jobOfferService.getJobOfferById(id)
    );

    forkJoin(jobRequests).subscribe({
      next: (jobOffers) => {
        // Create a map of jobOfferId -> jobOffer
        const jobMap = new Map<string, any>();
        jobOffers.forEach(job => {
          jobMap.set(job.id, job);
        });

        // Attach job offer data to each application
        this.applications = this.applications.map(app => ({
          ...app,
          jobOffer: jobMap.get(app.jobOfferId) || null
        }));

        this.filterApplications();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading job offer details:', error);
        // Still show applications even without enriched data
        this.filterApplications();
        this.isLoading = false;
      }
    });
  }

  changeTab(tabId: string): void {
    this.activeTab = tabId;
    this.filterApplications();
  }

  filterApplications(): void {
    const tab = this.tabs.find(t => t.id === this.activeTab);
    if (!tab || !tab.status) {
      this.filteredApplications = this.applications;
    } else {
      this.filteredApplications = this.applications.filter(app => app.status === tab.status);
    }
  }

  getTabCount(tabId: string): number {
    const tab = this.tabs.find(t => t.id === tabId);
    if (!tab) return 0;
    
    if (tabId === 'all') {
      return this.applications.length;
    }
    
    return this.applications.filter(app => app.status === tab.status).length;
  }

  viewJobOffer(jobOfferId: string): void {
    this.router.navigate(['/frontoffice/freelancer/jobs', jobOfferId]);
  }

  async withdrawApplication(applicationId: string, event: Event): Promise<void> {
    event.stopPropagation();
    if (confirm('Are you sure you want to withdraw this application?')) {
      try {
        await this.applicationService.withdrawApplication(applicationId).toPromise();
        alert('Application withdrawn successfully');
        this.loadMyApplications();
      } catch (error) {
        console.error('Error withdrawing application:', error);
        alert('Error withdrawing the application');
      }
    }
  }

  getStatusBadgeClass(status: ApplicationStatus): string {
    const classes: { [key: string]: string } = {
      [ApplicationStatus.PENDING]: 'badge-pending',
      [ApplicationStatus.SHORTLISTED]: 'badge-shortlisted',
      [ApplicationStatus.ACCEPTED]: 'badge-accepted',
      [ApplicationStatus.REJECTED]: 'badge-rejected',
      [ApplicationStatus.WITHDRAWN]: 'badge-withdrawn'
    };
    return classes[status] || '';
  }

  getStatusLabel(status: ApplicationStatus): string {
    return this.translate.instant(`applications.${status.toLowerCase()}`);
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

  canWithdraw(status: ApplicationStatus): boolean {
    return status === ApplicationStatus.PENDING || status === ApplicationStatus.SHORTLISTED;
  }
}
