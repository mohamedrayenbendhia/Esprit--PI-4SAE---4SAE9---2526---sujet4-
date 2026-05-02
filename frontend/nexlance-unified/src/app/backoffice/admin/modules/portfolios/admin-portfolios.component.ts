import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PortfolioService } from '../../../../core/services/portfolio.service';
import { UserService } from '../../../../core/services/user.service';
import { Portfolio } from '../../../../shared/models/portfolio.model';

@Component({
  selector: 'app-admin-portfolios',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatSnackBarModule, MatTooltipModule
  ],
  templateUrl: './admin-portfolios.component.html',
  styleUrls: ['./admin-portfolios.component.scss']
})
export class AdminPortfoliosComponent implements OnInit {
  private portfolioService = inject(PortfolioService);
  private userService = inject(UserService);
  private snackBar = inject(MatSnackBar);

  portfolios = signal<Portfolio[]>([]);
  userNames = signal<Record<string, string>>({});
  loading = signal(false);

  ngOnInit(): void {
    this.loadPortfolios();
  }

  loadPortfolios(): void {
    this.loading.set(true);
    this.portfolioService.getAllPortfolios().subscribe({
      next: (data) => {
        this.portfolios.set(data);
        this.loading.set(false);
        const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
        data.forEach(p => {
          if (!uuidPattern.test(p.userId)) {
            this.userNames.update(names => ({ ...names, [p.userId]: p.userId.substring(0, 8) + '...' }));
            return;
          }
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
        });
      },
      error: () => { this.snackBar.open('Error loading portfolios', 'Close', { duration: 3000 }); this.loading.set(false); }
    });
  }

  getName(userId: string): string {
    return this.userNames()[userId] || userId.substring(0, 8) + '...';
  }

  deletePortfolio(id: number): void {
    if (!confirm('Delete this portfolio?')) return;
    this.portfolioService.deletePortfolio(id).subscribe({
      next: () => { this.snackBar.open('Portfolio deleted!', 'Close', { duration: 3000 }); this.loadPortfolios(); },
      error: () => this.snackBar.open('Error deleting portfolio', 'Close', { duration: 3000 })
    });
  }

  getProjectBadge(count: number): { label: string; color: string } {
  if (count === 0) return { label: 'No projects', color: '#e53935' };
  if (count <= 2)  return { label: 'Starter',     color: '#f5a623' };
  if (count <= 5)  return { label: 'Active',       color: '#4facfe' };
  return              { label: 'Expert',       color: '#2ecc71' };
}
}