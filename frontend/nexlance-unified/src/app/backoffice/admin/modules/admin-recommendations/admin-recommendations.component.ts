import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatMenuModule } from '@angular/material/menu';
import { Router, RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { RecommendationService, Recommendation, RecommendationStatus } from '../../../../core/services/recommendation.service';

@Component({
  selector: 'app-admin-recommendations',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatIconModule,
    MatButtonModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatChipsModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatMenuModule,
    TranslateModule
  ],
  templateUrl: './admin-recommendations.component.html',
  styleUrls: ['./admin-recommendations.component.scss']
})
export class AdminRecommendationsComponent implements OnInit {
  displayedColumns: string[] = ['client', 'freelance', 'jobOffer', 'status', 'sentDate', 'deadline', 'views', 'actions'];
  
  recommendations: Recommendation[] = [];
  filteredRecommendations: Recommendation[] = [];
  paginatedRecommendations: Recommendation[] = [];
  
  isLoading = false;
  searchQuery = '';
  statusFilter = 'all';
  
  // Pagination
  pageSize = 10;
  pageIndex = 0;
  totalRecords = 0;
  
  // Stats
  stats = {
    total: 0,
    pending: 0,
    accepted: 0,
    rejected: 0,
    cancelled: 0,
    expired: 0
  };

  RecommendationStatus = RecommendationStatus;

  statusOptions = [
    { value: 'all', label: 'All' },
    { value: 'PENDING', label: 'Pending' },
    { value: 'ACCEPTED', label: 'Accepted' },
    { value: 'REJECTED', label: 'Rejected' },
    { value: 'CANCELLED', label: 'Cancelled' },
    { value: 'EXPIRED', label: 'Expired' }
  ];

  constructor(
    private router: Router,
    private recommendationService: RecommendationService
  ) {}

  ngOnInit(): void {
    this.loadRecommendations();
  }

  loadRecommendations(): void {
    this.isLoading = true;
    
    this.recommendationService.getAllRecommendations().subscribe({
      next: (data: Recommendation[]) => {
        this.recommendations = data;
        this.calculateStats();
        this.applyFilters();
        this.isLoading = false;
      },
      error: (error: any) => {
        console.error('Error loading recommendations:', error);
        this.isLoading = false;
      }
    });
  }

  calculateStats(): void {
    this.stats.total = this.recommendations.length;
    this.stats.pending = this.recommendations.filter(r => r.status === RecommendationStatus.PENDING).length;
    this.stats.accepted = this.recommendations.filter(r => r.status === RecommendationStatus.ACCEPTED).length;
    this.stats.rejected = this.recommendations.filter(r => r.status === RecommendationStatus.REJECTED).length;
    this.stats.cancelled = this.recommendations.filter(r => r.status === RecommendationStatus.CANCELLED).length;
    this.stats.expired = this.recommendations.filter(r => r.status === RecommendationStatus.EXPIRED).length;
  }

  applyFilters(): void {
    let filtered = [...this.recommendations];

    // Status filter
    if (this.statusFilter !== 'all') {
      filtered = filtered.filter(rec => rec.status === this.statusFilter);
    }

    // Search filter
    if (this.searchQuery.trim()) {
      const query = this.searchQuery.toLowerCase();
      filtered = filtered.filter(rec =>
        rec.id.toString().includes(query) ||
        rec.clientId.toString().includes(query) ||
        rec.freelanceId.toString().includes(query) ||
        rec.jobOfferId.toString().includes(query) ||
        rec.message.toLowerCase().includes(query)
      );
    }

    this.filteredRecommendations = filtered;
    this.totalRecords = filtered.length;
    this.updatePagination();
  }

  updatePagination(): void {
    const startIndex = this.pageIndex * this.pageSize;
    const endIndex = startIndex + this.pageSize;
    this.paginatedRecommendations = this.filteredRecommendations.slice(startIndex, endIndex);
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.updatePagination();
  }

  onSortChange(sort: Sort): void {
    const data = [...this.filteredRecommendations];
    
    if (!sort.active || sort.direction === '') {
      this.filteredRecommendations = data;
      return;
    }

    this.filteredRecommendations = data.sort((a, b) => {
      const isAsc = sort.direction === 'asc';
      
      switch (sort.active) {
        case 'id':
          return this.compare(a.id, b.id, isAsc);
        case 'status':
          return this.compare(a.status, b.status, isAsc);
        case 'sentDate':
          return this.compare(new Date(a.sentDate).getTime(), new Date(b.sentDate).getTime(), isAsc);
        case 'views':
          return this.compare(a.viewCount, b.viewCount, isAsc);
        default:
          return 0;
      }
    });
    
    this.updatePagination();
  }

  compare(a: number | string, b: number | string, isAsc: boolean): number {
    return (a < b ? -1 : 1) * (isAsc ? 1 : -1);
  }

  viewDetails(recommendation: Recommendation): void {
    this.router.navigate(['/admin/recommendations', recommendation.id]);
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

  exportData(): void {
    // TODO: Implement CSV export
    console.log('Export data...');
  }
}
