import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { UserManagementService, UserListResponse, UserListItem } from '../../../core/services/user-management.service';
import { UserRole, UserStatus } from '../../../shared/models/user.model';
import { UserDetailsDialogComponent } from './user-details-dialog.component';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatChipsModule,
    MatTooltipModule,
    MatDialogModule,
    MatSnackBarModule
  ],
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss']
})
export class UserManagementComponent implements OnInit {
  displayedColumns: string[] = ['name', 'email', 'role', 'status', 'createdAt', 'lastLogin', 'actions'];
  
  users = signal<UserListItem[]>([]);
  totalUsers = signal<number>(0);
  loading = signal<boolean>(false);
  
  // Filters
  searchControl = new FormControl('');
  roleFilter = signal<UserRole | 'ALL'>('ALL');
  statusFilter = signal<UserStatus | 'ALL'>('ALL');
  
  // Pagination
  pageIndex = signal<number>(0);
  pageSize = signal<number>(10);
  
  // Enums for template
  UserRole = UserRole;
  UserStatus = UserStatus;
  availableRoles = Object.values(UserRole);
  availableStatuses = Object.values(UserStatus);

  constructor(
    private userManagementService: UserManagementService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadUsers();
    
    // Search with debounce
    this.searchControl.valueChanges.subscribe(() => {
      this.pageIndex.set(0);
      this.loadUsers();
    });
  }

  loadUsers(): void {
    this.loading.set(true);
    
    const filters = {
      search: this.searchControl.value || '',
      role: this.roleFilter() === 'ALL' ? undefined : (this.roleFilter() as UserRole),
      status: this.statusFilter() === 'ALL' ? undefined : (this.statusFilter() as UserStatus),
      page: this.pageIndex(),
      size: this.pageSize()
    };

    this.userManagementService.getUsers(filters).subscribe({
      next: (response: UserListResponse) => {
        this.users.set(response.users);
        this.totalUsers.set(response.total);
        this.loading.set(false);
      },
      error: (error: any) => {
        console.error('Error loading users:', error);
        this.snackBar.open('Error loading users', 'Close', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadUsers();
  }

  onRoleFilterChange(role: UserRole | 'ALL'): void {
    this.roleFilter.set(role);
    this.pageIndex.set(0);
    this.loadUsers();
  }

  onStatusFilterChange(status: UserStatus | 'ALL'): void {
    this.statusFilter.set(status);
    this.pageIndex.set(0);
    this.loadUsers();
  }

  getRoleColor(role: UserRole): string {
    const colors: Record<UserRole, string> = {
      [UserRole.ADMIN]: 'primary',
      [UserRole.FREELANCER]: 'accent',
      [UserRole.CLIENT]: 'warn',
      [UserRole.SUPPORT_AGENT]: 'basic'
    };
    return colors[role] || 'default';
  }

  getStatusColor(status: UserStatus): string {
    const colors: Record<UserStatus, string> = {
      [UserStatus.ACTIVE]: 'success',
      [UserStatus.SUSPENDED]: 'warn',
      [UserStatus.PENDING_VERIFICATION]: 'info',
      [UserStatus.DELETED]: 'danger'
    };
    return colors[status] || 'default';
  }

  toggleUserStatus(user: UserListItem): void {
    const newStatus = user.status === UserStatus.ACTIVE ? UserStatus.SUSPENDED : UserStatus.ACTIVE;
    const action = newStatus === UserStatus.ACTIVE ? 'activate' : 'deactivate';
    
    if (confirm(`Are you sure you want to ${action} this user?`)) {
      this.userManagementService.updateUserStatus(user.id, newStatus).subscribe({
        next: () => {
          this.snackBar.open(`User ${newStatus === UserStatus.ACTIVE ? 'activated' : 'deactivated'} successfully`, 'Close', { duration: 3000 });
          this.loadUsers();
        },
        error: (error: any) => {
          console.error('Error updating user status:', error);
          this.snackBar.open('Error updating status', 'Close', { duration: 3000 });
        }
      });
    }
  }

  softDeleteUser(user: UserListItem): void {
    if (user.type === UserRole.ADMIN) {
      this.snackBar.open('Cannot delete an administrator', 'Close', { duration: 3000 });
      return;
    }

    if (confirm(`Are you sure you want to delete this user?\n\nThis action will mark the user as deleted (soft delete).`)) {
      this.userManagementService.updateUserStatus(user.id, UserStatus.DELETED).subscribe({
        next: () => {
          this.snackBar.open('User deleted successfully', 'Close', { duration: 3000 });
          this.loadUsers();
        },
        error: (error: any) => {
          console.error('Error deleting user:', error);
          this.snackBar.open('Error during deletion', 'Close', { duration: 3000 });
        }
      });
    }
  }

  viewUserDetails(user: UserListItem): void {
    const dialogRef = this.dialog.open(UserDetailsDialogComponent, {
      data: user,
      width: '600px',
      maxHeight: '90vh',
      autoFocus: false
    });

    dialogRef.afterClosed().subscribe(result => {
      // Refresh data if modified
      if (result) {
        this.loadUsers();
      }
    });
  }

  exportUsers(): void {
    // TODO: Implement export functionality
    this.snackBar.open('Export in progress (to be implemented)', 'Close', { duration: 2000 });
  }

  resetFilters(): void {
    this.searchControl.setValue('');
    this.roleFilter.set('ALL');
    this.statusFilter.set('ALL');
    this.pageIndex.set(0);
    this.loadUsers();
  }

  get activeUsersCount(): number {
    return this.users().filter(u => u.status === UserStatus.ACTIVE).length;
  }

  get inactiveUsersCount(): number {
    return this.users().filter(u => u.status === UserStatus.SUSPENDED).length;
  }
}
