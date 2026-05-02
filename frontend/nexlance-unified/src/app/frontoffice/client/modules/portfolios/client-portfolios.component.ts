import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PortfolioService } from '../../../../core/services/portfolio.service';
import { Portfolio } from '../../../../shared/models/portfolio.model';
import { UserService } from '../../../../core/services/user.service';

@Component({
  selector: 'app-client-portfolios',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatSnackBarModule, MatTooltipModule
  ],
  templateUrl: './client-portfolios.component.html',
  styleUrls: ['./client-portfolios.component.scss']
})
export class ClientPortfoliosComponent implements OnInit {
  private portfolioService = inject(PortfolioService);
  private snackBar = inject(MatSnackBar);
  private userService = inject(UserService);
userNames = signal<Record<string, string>>({});

  portfolios = signal<Portfolio[]>([]);
  selectedPortfolio = signal<Portfolio | null>(null);
  loading = signal(false);

  ngOnInit(): void {
    this.loadPortfolios();
  }

  loadPortfolios(): void {
  this.loading.set(true);
  this.portfolioService.getAllPublicPortfolios().subscribe({
    next: (data) => {
      this.portfolios.set(data);
      this.loading.set(false);
      // Load user names with delay to avoid ERR_INSUFFICIENT_RESOURCES
      data.forEach((p, index) => {
        setTimeout(() => {
          this.userService.getUserById(p.userId).subscribe({
            next: (user) => {
              this.userNames.update(names => ({
                ...names,
                [p.userId]: `${user.firstName} ${user.lastName}`
              }));
            },
            error: () => {
              this.userNames.update(names => ({
                ...names,
                [p.userId]: p.userId.substring(0, 8) + '...'
              }));
            }
          });
        }, index * 100); // 100ms delay between each request
      });
    },
    error: () => { this.snackBar.open('Error loading portfolios', 'Close', { duration: 3000 }); this.loading.set(false); }
  });
}

getName(userId: string): string {
  return this.userNames()[userId] || userId.substring(0, 8) + '...';
}

  selectPortfolio(p: Portfolio): void {
  this.selectedPortfolio.set(p);
  this.portfolioService.incrementPortfolioViews(p.userId).subscribe();
}

  back(): void {
    this.selectedPortfolio.set(null);
  }

  getProjectBadge(count: number): { label: string; color: string } {
  if (count === 0) return { label: 'No projects', color: '#e53935' };
  if (count <= 2)  return { label: 'Starter',     color: '#f5a623' };
  if (count <= 5)  return { label: 'Active',       color: '#4facfe' };
  return              { label: 'Expert',       color: '#2ecc71' };
}
}