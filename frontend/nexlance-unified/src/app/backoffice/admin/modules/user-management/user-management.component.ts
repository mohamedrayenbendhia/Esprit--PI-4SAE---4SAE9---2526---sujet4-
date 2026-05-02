import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { UserService, UserFilter, UserStats } from '../../../../core/services/user.service';
import { User, UserStatus, SubscriptionType, UserRole } from '../../../../shared/models/user.model';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss']
})
export class UserManagementComponent implements OnInit {
  private userService = inject(UserService);
  private router = inject(Router);

  users = signal<User[]>([]);
  stats = signal<UserStats | null>(null);
  loading = signal(false);
  filters: UserFilter = {
    page: 0,
    size: 10,
    sortBy: 'createdAt',
    sortOrder: 'desc'
  };
  
  totalPages = 0;
  currentPage = 0;

  // Enums for template
  UserRole = UserRole;
  UserStatus = UserStatus;
  SubscriptionType = SubscriptionType;

  ngOnInit(): void {
    this.loadStats();
    this.loadUsers();
  }

  loadStats(): void {
    this.userService.getUserStats().subscribe({
      next: (stats) => this.stats.set(stats),
      error: (err) => console.error('Error loading stats:', err)
    });
  }

  loadUsers(): void {
    this.loading.set(true);
    this.userService.getAllUsers(this.filters).subscribe({
      next: (response) => {
        this.users.set(response.content);
        this.totalPages = response.totalPages;
        this.currentPage = response.number;
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading users:', err);
        this.loading.set(false);
      }
    });
  }

  onFilterChange(): void {
    this.filters.page = 0;
    this.loadUsers();
  }

  onPageChange(page: number): void {
    this.filters.page = page;
    this.loadUsers();
  }

  viewUser(userId: string): void {
    this.router.navigate(['/backoffice/admin/users', userId]);
  }

  suspendUser(user: User): void {
    if (confirm(`Are you sure you want to suspend ${user.firstName} ${user.lastName}?`)) {
      this.userService.updateUserStatus(user.id, UserStatus.SUSPENDED).subscribe({
        next: () => {
          alert('User suspended successfully');
          this.loadUsers();
        },
        error: (err) => console.error('Error suspending user:', err)
      });
    }
  }

  activateUser(user: User): void {
    this.userService.updateUserStatus(user.id, UserStatus.ACTIVE).subscribe({
      next: () => {
        alert('User activated successfully');
        this.loadUsers();
      },
      error: (err) => console.error('Error activating user:', err)
    });
  }

  deleteUser(user: User): void {
    if (confirm(`Are you sure you want to delete ${user.firstName} ${user.lastName}? This action is irreversible.`)) {
      this.userService.deleteUser(user.id).subscribe({
        next: () => {
          alert('User deleted successfully');
          this.loadUsers();
        },
        error: (err) => console.error('Error deleting user:', err)
      });
    }
  }

  exportUsers(): void {
    this.userService.exportUsers(this.filters).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `users-export-${new Date().toISOString()}.csv`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => console.error('Error exporting:', err)
    });
  }

  getStatusBadgeClass(status: UserStatus): string {
    switch (status) {
      case UserStatus.ACTIVE: return 'badge-success';
      case UserStatus.SUSPENDED: return 'badge-danger';
      case UserStatus.PENDING_VERIFICATION: return 'badge-warning';
      default: return 'badge-secondary';
    }
  }

  getRoleBadgeClass(type: any): string {
    switch (type) {
      case 'freelance': return 'badge-primary';
      case 'client': return 'badge-info';
      case 'admin': return 'badge-dark';
      default: return 'badge-secondary';
    }
  }
}
