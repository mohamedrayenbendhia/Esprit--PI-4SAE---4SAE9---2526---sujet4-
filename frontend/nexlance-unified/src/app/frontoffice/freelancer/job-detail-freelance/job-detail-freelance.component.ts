import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule, NativeDateAdapter, DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { JobOfferService } from '@core/services/job-offer.service';
import { ApplicationService } from '@core/services/application.service';
import { AuthService } from '@core/services/auth.service';
import { CurrencyService } from '@core/services/currency.service';
import { PosthogService } from '@core/services/posthog.service';
import { JobOffer } from '@core/models/job-offer.model';
import { FormsModule } from '@angular/forms';
import { CreateApplicationDto, PortfolioItem } from '@core/models/application.model';

@Component({
  selector: 'app-job-detail-freelance',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    TranslateModule, 
    MatIconModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatFormFieldModule
  ],
  providers: [
    { provide: DateAdapter, useClass: NativeDateAdapter },
    { provide: MAT_DATE_LOCALE, useValue: 'en-US' }
  ],
  templateUrl: './job-detail-freelance.component.html',
  styleUrls: ['./job-detail-freelance.component.scss']
})
export class JobDetailFreelanceComponent implements OnInit {
  job!: JobOffer;
  isLoading = true;
  showApplicationModal = false;
  isSubmitting = false;
  hasApplied = false;

  // Notification system
  notification: { message: string; type: 'success' | 'error' } | null = null;

  // Application form data
  applicationForm = {
    coverLetter: '',
    proposedRate: 0,
    estimatedDelivery: null as Date | null,
    availableFrom: null as Date | null,
    portfolioItems: [] as PortfolioItem[]
  };

  // Available portfolio items (would come from user profile)
  availablePortfolioItems: PortfolioItem[] = [];
  selectedPortfolioIds: string[] = [];

  // Currency conversion (ExchangeRate API)
  convertedBudgets: { currency: string; amount: number; symbol: string }[] = [];
  showCurrencyConverter = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private jobOfferService: JobOfferService,
    private applicationService: ApplicationService,
    private authService: AuthService,
    private currencyService: CurrencyService,
    private posthogService: PosthogService,
    private translate: TranslateService
  ) {}

  ngOnInit(): void {
    const jobId = this.route.snapshot.paramMap.get('id');
    if (jobId) {
      this.posthogService.trackPageView('job_detail');
      this.loadJobDetails(jobId);
      this.checkIfApplied(jobId);
    }
  }

  showNotification(message: string, type: 'success' | 'error'): void {
    this.notification = { message, type };
    setTimeout(() => {
      this.notification = null;
    }, 4000);
  }

  loadJobDetails(jobId: string): void {
    this.jobOfferService.getJobOfferById(jobId).subscribe({
      next: (job: any) => {
        this.job = job;
        this.applicationForm.proposedRate = job.budget;
        this.isLoading = false;
        this.loadCurrencyConversions(job.budget);
      },
      error: (error: any) => {
        console.error('Error loading job:', error);
        this.isLoading = false;
      }
    });
  }

  private loadCurrencyConversions(budgetTND: number): void {
    this.currencyService.getConvertedDisplay(budgetTND).subscribe(conversions => {
      this.convertedBudgets = conversions;
    });
  }

  toggleCurrencyConverter(): void {
    this.showCurrencyConverter = !this.showCurrencyConverter;
  }

  checkIfApplied(jobId: string): void {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser || !currentUser.id) {
      this.hasApplied = false;
      return;
    }
    
    // Check if this user has already applied to this offer
    this.applicationService.getApplicationsByJobOffer(jobId).subscribe({
      next: (applications: any[]) => {
        // Check if the current user has an application for this offer
        this.hasApplied = applications.some(app => app.freelanceId === currentUser.id);
      },
      error: (error: any) => {
        console.error('Error checking application status:', error);
        this.hasApplied = false;
      }
    });
  }

  openApplicationModal(): void {
    if (this.hasApplied) {
      this.showNotification(this.translate.instant('jobDetail.alreadyApplied'), 'error');
      return;
    }
    
    // Pre-fill the availability date with tomorrow by default
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    
    this.applicationForm.availableFrom = tomorrow;
    this.showApplicationModal = true;
  }

  closeApplicationModal(): void {
    this.showApplicationModal = false;
    this.resetApplicationForm();
  }

  resetApplicationForm(): void {
    this.applicationForm = {
      coverLetter: '',
      proposedRate: this.job?.budget || 0,
      estimatedDelivery: null,
      availableFrom: null,
      portfolioItems: []
    };
    this.selectedPortfolioIds = [];
  }

  togglePortfolioItem(itemId: string): void {
    const index = this.selectedPortfolioIds.indexOf(itemId);
    if (index > -1) {
      this.selectedPortfolioIds.splice(index, 1);
    } else {
      this.selectedPortfolioIds.push(itemId);
    }
  }

  isPortfolioSelected(itemId: string): boolean {
    return this.selectedPortfolioIds.includes(itemId);
  }

  async submitApplication(): Promise<void> {
    if (!this.validateForm()) {
      return;
    }

    this.isSubmitting = true;

    try {
      const currentUser = this.authService.getCurrentUser();
      if (!currentUser || !currentUser.id) {
        this.showNotification(this.translate.instant('jobDetail.mustBeLoggedIn'), 'error');
        return;
      }

      const selectedPortfolio = this.availablePortfolioItems.filter(item =>
        this.selectedPortfolioIds.includes(item.id)
      );

      const applicationDto: CreateApplicationDto = {
        jobOfferId: this.job.id,
        freelanceId: currentUser.id,
        coverLetter: this.applicationForm.coverLetter,
        proposedRate: this.applicationForm.proposedRate,
        estimatedDelivery: this.applicationForm.estimatedDelivery!,
        availableFrom: this.applicationForm.availableFrom!,
        portfolioItems: selectedPortfolio
      };

      console.log('Sending application:', JSON.stringify(applicationDto));

      await this.applicationService.createApplication(applicationDto).toPromise();
      this.posthogService.trackApplication('submitted', {
        jobId: this.job.id,
        category: this.job.category,
        proposedRate: this.applicationForm.proposedRate
      });
      this.showNotification(this.translate.instant('jobDetail.applicationSuccess'), 'success');
      this.hasApplied = true;
      this.closeApplicationModal();
    } catch (error: any) {
      console.error('Error submitting application:', error);
      this.showNotification(error.error?.message || this.translate.instant('common.error'), 'error');
    } finally {
      this.isSubmitting = false;
    }
  }

  validateForm(): boolean {
    if (!this.applicationForm.coverLetter || this.applicationForm.coverLetter.length < 50) {
      this.showNotification('Cover letter must be at least 50 characters long', 'error');
      return false;
    }
    if (!this.applicationForm.proposedRate || this.applicationForm.proposedRate <= 0) {
      this.showNotification('Please enter a valid proposed rate', 'error');
      return false;
    }
    if (!this.applicationForm.estimatedDelivery) {
      this.showNotification('Please enter the estimated delivery date', 'error');
      return false;
    }
    // Le champ availableFrom est maintenant pré-rempli automatiquement
    return true;
  }

  saveJob(): void {
    // TODO: Implement save to favorites
    this.showNotification(this.translate.instant('jobDetail.jobSaved'), 'success');
  }

  viewClientProfile(): void {
    this.router.navigate(['/profile', this.job.clientId]);
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
      'development': 'code',
      'design': 'palette',
      'marketing': 'campaign',
      'writing': 'edit',
      'other': 'work'
    };
    return icons[category.toLowerCase()] || 'work';
  }

  goBack(): void {
    this.router.navigate(['/frontoffice/freelancer/browse-jobs']);
  }

  downloadAttachment(attachmentUrl: string): void {
    window.open(attachmentUrl, '_blank');
  }

  getFileName(url: string): string {
    // Extract filename from URL path
    const parts = url.split('/');
    return parts[parts.length - 1] || 'attachment';
  }
}
