import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { JobOfferService } from '@core/services/job-offer.service';
import { ApplicationService } from '@core/services/application.service';
import { ProjectService } from '@core/services/project.service';
import { JobOffer } from '@core/models/job-offer.model';
import { Application, ApplicationStatus } from '@core/models/application.model';
import { CreateProjectRequest, MilestoneStatus } from '@core/models/project.model';
import { CreateProjectDialogComponent, CreateProjectDialogResult } from './create-project-dialog/create-project-dialog.component';

@Component({
  selector: 'app-job-detail-client',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule, MatIconModule, MatDialogModule],
  templateUrl: './job-detail-client.component.html',
  styleUrls: ['./job-detail-client.component.scss']
})
export class JobDetailClientComponent implements OnInit {
  job!: JobOffer;
  applications: Application[] = [];
  filteredApplications: Application[] = [];
  isLoading = true;
  activeTab: string = 'all';
  applicationCounts: { [key: string]: number } = {};

  tabs = [
    { id: 'all', label: 'All', status: null },
    { id: 'pending', label: 'Pending', status: ApplicationStatus.PENDING },
    { id: 'shortlisted', label: 'Shortlisted', status: ApplicationStatus.SHORTLISTED },
    { id: 'accepted', label: 'Accepted', status: ApplicationStatus.ACCEPTED },
    { id: 'rejected', label: 'Rejected', status: ApplicationStatus.REJECTED }
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private jobOfferService: JobOfferService,
    private applicationService: ApplicationService,
    private projectService: ProjectService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    const jobId = this.route.snapshot.paramMap.get('id');
    if (jobId) {
      this.loadJobDetails(jobId);
      this.loadApplications(jobId);
      this.loadApplicationCounts(jobId);
    }
  }

  loadJobDetails(jobId: string): void {
    this.jobOfferService.getJobOfferById(jobId).subscribe({
      next: (job: any) => {
        this.job = job;
        this.isLoading = false;
      },
      error: (error: any) => {
        console.error('Error loading job:', error);
        this.isLoading = false;
      }
    });
  }

  loadApplications(jobId: string): void {
    this.applicationService.getApplicationsByJobOffer(jobId).subscribe({
      next: (applications: any) => {
        this.applications = applications; // Direct array
        this.filterApplications();
      },
      error: (error: any) => {
        console.error('Error loading applications:', error);
      }
    });
  }

  loadApplicationCounts(jobId: string): void {
    this.applicationService.getApplicationCountsByStatus(jobId).subscribe({
      next: (counts: any) => {
        this.applicationCounts = counts;
      },
      error: (error: any) => {
        console.error('Error loading counts:', error);
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
    
    return this.filteredApplications.filter(app => app.status === tab.status).length;
  }

  viewFreelancerProfile(freelanceId: string): void {
    this.router.navigate(['/frontoffice/client/freelancers', freelanceId]);
  }

  viewApplicationDetail(applicationId: string): void {
    this.router.navigate(['/frontoffice/client/my-jobs', this.job.id, 'applications', applicationId]);
  }

  async shortlistApplication(applicationId: string, event: Event): Promise<void> {
    event.stopPropagation();
    try {
      await this.applicationService.shortlistApplication(applicationId).toPromise();
      alert('Application shortlisted successfully');
      this.loadApplications(this.job.id);
    } catch (error) {
      console.error('Error shortlisting application:', error);
      alert('Error shortlisting application');
    }
  }

  async acceptApplication(applicationId: string, event: Event): Promise<void> {
    event.stopPropagation();

    // Find the application to get freelancer info
    const application = this.applications.find(a => a.id === applicationId);
    if (!application) return;

    // First accept the application
    if (!confirm('Are you sure you want to accept this application?')) return;

    try {
      await this.applicationService.acceptApplication(applicationId).toPromise();
    } catch (error) {
      console.error('Error accepting application:', error);
      alert('Error accepting application');
      return;
    }

    // Reload applications to reflect status change
    this.loadApplications(this.job.id);

    // Open dialog to create project & milestones for this freelancer
    const dialogRef = this.dialog.open(CreateProjectDialogComponent, {
      width: '720px',
      disableClose: true,
      data: {
        jobOffer: this.job,
        application: application
      }
    });

    dialogRef.afterClosed().subscribe((result: CreateProjectDialogResult | null) => {
      if (!result) return;

      const projectRequest: CreateProjectRequest = {
        jobOfferId: this.job.id,
        freelanceId: application.freelanceId,
        clientId: this.job.clientId,
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
          alert('Project and milestones created successfully!');
          console.log('Project created:', project);
        },
        error: (err) => {
          console.error('Error creating project:', err);
          alert('Application accepted but error creating the project. You can create it manually later.');
        }
      });
    });
  }

  async rejectApplication(applicationId: string, event: Event): Promise<void> {
    event.stopPropagation();
    if (confirm('Are you sure you want to reject this application?')) {
      try {
        await this.applicationService.rejectApplication(applicationId).toPromise();
        alert('Application rejected');
        this.loadApplications(this.job.id);
      } catch (error) {
        console.error('Error rejecting application:', error);
        alert('Error rejecting application');
      }
    }
  }

  markAsRead(applicationId: string): void {
    this.applicationService.markAsRead(applicationId).subscribe({
      next: () => {
        const app = this.applications.find(a => a.id === applicationId);
        if (app) app.isRead = true;
      }
    });
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
    const labels: { [key: string]: string } = {
      [ApplicationStatus.PENDING]: 'Pending',
      [ApplicationStatus.SHORTLISTED]: 'Shortlisted',
      [ApplicationStatus.ACCEPTED]: 'Accepted',
      [ApplicationStatus.REJECTED]: 'Rejected',
      [ApplicationStatus.WITHDRAWN]: 'Withdrawn'
    };
    return labels[status] || status;
  }

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  editJob(): void {
    this.router.navigate(['/frontoffice/client/edit-job', this.job.id]);
  }

  goBack(): void {
    this.router.navigate(['/frontoffice/client/my-jobs']);
  }
}
