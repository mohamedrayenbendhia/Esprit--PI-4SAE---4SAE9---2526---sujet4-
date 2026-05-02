import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { MatIconModule } from '@angular/material/icon';
import { Subject, Subscription, debounceTime, distinctUntilChanged, switchMap, of, forkJoin } from 'rxjs';
import { JobOfferService } from '@core/services/job-offer.service';
import { ApplicationService } from '@core/services/application.service';
import { AuthService } from '@core/services/auth.service';
import { AlgoliaSearchService } from '@core/services/algolia-search.service';
import { PosthogService } from '@core/services/posthog.service';
import { CurrencyService } from '@core/services/currency.service';
import { JobOffer, JobCategory, BudgetType, ExperienceLevel, JobOfferFilters } from '@core/models/job-offer.model';
import { JobSmartBadgesComponent } from '../../../shared/components/job-smart-badges/job-smart-badges.component';

@Component({
  selector: 'app-browse-jobs',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslateModule, MatIconModule, JobSmartBadgesComponent],
  templateUrl: './browse-jobs.component.html',
  styleUrls: ['./browse-jobs.component.scss']
})
export class BrowseJobsComponent implements OnInit, OnDestroy {
  jobs: JobOffer[] = [];
  recommendedJobs: JobOffer[] = [];
  isLoading = true;
  searchQuery = '';

  // Live search
  private searchSubject = new Subject<string>();
  private searchSubscription?: Subscription;
  searchSuggestions: JobOffer[] = [];
  showSuggestions = false;

  // Set of jobOfferIds the current user already applied to
  private appliedJobIds = new Set<string>();
  
  filters: JobOfferFilters = {
    page: 0,
    size: 20
  };
  
  // Filter options
  categories = Object.values(JobCategory);
  budgetTypes = Object.values(BudgetType);
  experienceLevels = Object.values(ExperienceLevel);
  
  // UI state
  showFilters = true;
  totalElements = 0;
  totalPages = 0;

  // Algolia search metrics
  algoliaSearchTime = 0;
  useAlgolia = true; // Toggle between Algolia and local search

  // Currency conversion
  selectedCurrency = 'TND';
  currencyRates: { currency: string; amount: number; symbol: string }[] = [];

  constructor(
    private jobOfferService: JobOfferService,
    private applicationService: ApplicationService,
    private authService: AuthService,
    private algoliaService: AlgoliaSearchService,
    private posthogService: PosthogService,
    private currencyService: CurrencyService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.posthogService.trackPageView('browse_jobs');
    this.loadRecommendedJobs();
    this.loadMyApplicationsThenJobs();
    this.initLiveSearch();
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
  }

  /** Load freelancer's applications first, then load jobs excluding already-applied ones */
  private loadMyApplicationsThenJobs(): void {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.id) {
      this.loadJobs();
      return;
    }
    this.applicationService.getMyApplications({ freelanceId: currentUser.id }).subscribe({
      next: (apps) => {
        this.appliedJobIds = new Set(apps.map(a => a.jobOfferId));
        this.loadJobs();
      },
      error: () => {
        // If we can't load applications, still show jobs
        this.loadJobs();
      }
    });
  }

  initLiveSearch(): void {
    this.searchSubscription = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(query => {
        if (query.length < 2) {
          return of([]);
        }
        // Use Algolia for live suggestions if available
        if (this.useAlgolia) {
          return this.algoliaService.getSuggestions(query, 5).pipe(
            switchMap(hits => {
              if (hits.length > 0) {
                this.algoliaSearchTime = 0; // Updated from full search
                return of(hits.map(hit => ({
                  id: hit.objectID,
                  title: hit.title,
                  description: hit.description,
                  category: hit.category,
                  budget: hit.budget,
                  budgetType: hit.budgetType,
                  requiredSkills: hit.requiredSkills || [],
                  location: hit.location,
                  experienceLevel: hit.experienceLevel
                } as any)));
              }
              // Fallback to local search if Algolia returns nothing
              return this.jobOfferService.searchJobOffers(query, { page: 0, size: 5 });
            })
          );
        }
        return this.jobOfferService.searchJobOffers(query, { page: 0, size: 5 });
      })
    ).subscribe({
      next: (results: any) => {
        this.searchSuggestions = Array.isArray(results) ? results.slice(0, 5) : [];
        this.showSuggestions = this.searchSuggestions.length > 0;
      },
      error: () => {
        this.searchSuggestions = [];
        this.showSuggestions = false;
      }
    });
  }

  onSearchInput(event: Event): void {
    const query = (event.target as HTMLInputElement).value;
    this.searchQuery = query;
    this.searchSubject.next(query);
  }

  selectSuggestion(job: JobOffer): void {
    this.showSuggestions = false;
    this.viewJobDetail(job.id);
  }

  hideSuggestions(): void {
    setTimeout(() => this.showSuggestions = false, 200);
  }

  loadRecommendedJobs(): void {
    const currentUser = this.authService.getCurrentUser();
    const userId = currentUser?.id || 'anonymous';
    this.jobOfferService.getRecommendedJobOffers(userId).subscribe({
      next: (jobs: any) => {
        this.recommendedJobs = jobs.slice(0, 3);
      },
      error: (error: any) => {
        console.error('Error loading recommended jobs:', error);
      }
    });
  }

  loadJobs(): void {
    this.isLoading = true;
    
    const applyFilters = (jobs: any[]) => {
      let filtered = jobs;

      // Only show OPEN jobs (not DRAFT, ARCHIVED, COMPLETED, etc.)
      filtered = filtered.filter(j => j.status === 'OPEN');

      // Exclude jobs the freelancer already applied to
      if (this.appliedJobIds.size > 0) {
        filtered = filtered.filter(j => !this.appliedJobIds.has(j.id));
      }
      
      // Apply client-side filters
      if (this.filters.category) {
        filtered = filtered.filter(j => j.category === this.filters.category);
      }
      if (this.filters.budgetType) {
        filtered = filtered.filter(j => j.budgetType === this.filters.budgetType);
      }
      if (this.filters.minBudget) {
        filtered = filtered.filter(j => j.budget >= this.filters.minBudget!);
      }
      if (this.filters.maxBudget) {
        filtered = filtered.filter(j => j.budget <= this.filters.maxBudget!);
      }
      if (this.filters.experienceLevel) {
        filtered = filtered.filter(j => j.experienceLevel === this.filters.experienceLevel);
      }
      if (this.filters.location) {
        const loc = this.filters.location.toLowerCase();
        filtered = filtered.filter(j => j.location && j.location.toLowerCase().includes(loc));
      }
      if (this.filters.isRemote) {
        filtered = filtered.filter(j => j.isRemote === true);
      }
      
      return filtered;
    };
    
    if (this.searchQuery) {
      this.jobOfferService.searchJobOffers(this.searchQuery, this.filters).subscribe({
        next: (jobs: any) => {
          this.jobs = applyFilters(jobs);
          this.totalElements = this.jobs.length;
          this.totalPages = 1;
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
          this.jobs = applyFilters(jobs);
          this.totalElements = this.jobs.length;
          this.totalPages = 1;
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
    this.posthogService.trackSearch(this.searchQuery, this.totalElements, this.filters as any);
    this.loadJobs();
  }

  onFilterChange(): void {
    this.filters.page = 0;
    this.posthogService.capture('filter_changed', this.filters as any);
    this.loadJobs();
  }

  clearFilters(): void {
    this.filters = {
      page: 0,
      size: 20
    };
    this.searchQuery = '';
    this.loadJobs();
  }

  viewJobDetail(jobId: string): void {
    this.posthogService.trackJobOfferEvent('viewed', { id: jobId });
    this.jobOfferService.incrementViewCount(jobId).subscribe();
    this.router.navigate(['/frontoffice/freelancer/jobs', jobId]);
  }

  toggleFilters(): void {
    this.showFilters = !this.showFilters;
  }

  formatDate(date: Date): string {
    const now = new Date();
    const jobDate = new Date(date);
    const diffTime = Math.abs(now.getTime() - jobDate.getTime());
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    if (diffDays === 0) return 'Today';
    if (diffDays === 1) return 'Yesterday';
    if (diffDays < 7) return `${diffDays} days ago`;
    if (diffDays < 30) return `${Math.floor(diffDays / 7)} weeks ago`;
    return jobDate.toLocaleDateString('en-US');
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

  nextPage(): void {
    if (this.filters.page! < this.totalPages - 1) {
      this.filters.page!++;
      this.loadJobs();
      window.scrollTo(0, 0);
    }
  }

  previousPage(): void {
    if (this.filters.page! > 0) {
      this.filters.page!--;
      this.loadJobs();
      window.scrollTo(0, 0);
    }
  }

  hasActiveFilters(): boolean {
    return !!(
      this.filters.category ||
      this.filters.minBudget ||
      this.filters.maxBudget ||
      this.filters.budgetType ||
      this.filters.experienceLevel ||
      this.filters.location ||
      this.filters.isRemote !== undefined
    );
  }
}
