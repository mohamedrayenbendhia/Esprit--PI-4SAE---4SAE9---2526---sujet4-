import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatListModule } from '@angular/material/list';
import { UserListItem } from '../../../core/services/user-management.service';
import { UserStatus, UserRole } from '../../../shared/models/user.model';

@Component({
  selector: 'app-user-details-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatListModule
  ],
  template: `
    <div class="user-details-dialog">
      <h2 mat-dialog-title>
        <mat-icon>person</mat-icon>
        User Details
      </h2>

      <mat-dialog-content>
        <div class="user-header">
          <div class="user-avatar-large">
            <mat-icon>account_circle</mat-icon>
          </div>
          <div class="user-main-info">
            <h3>{{ data.firstName }} {{ data.lastName }}</h3>
            <p class="user-email">{{ data.email }}</p>
            <div class="user-badges">
              <mat-chip [color]="getRoleColor(data.type)" highlighted>
                <mat-icon>{{ getRoleIcon(data.type) }}</mat-icon>
                {{ data.type }}
              </mat-chip>
              <mat-chip 
                [class.status-active]="data.status === UserStatus.ACTIVE"
                [class.status-suspended]="data.status === UserStatus.SUSPENDED"
                [class.status-pending]="data.status === UserStatus.PENDING_VERIFICATION"
                [class.status-deleted]="data.status === UserStatus.DELETED">
                <mat-icon>{{ getStatusIcon(data.status) }}</mat-icon>
                {{ data.status }}
              </mat-chip>
            </div>
          </div>
        </div>

        <mat-divider></mat-divider>

        <div class="details-section">
          <h4><mat-icon>badge</mat-icon> General Information</h4>
          <mat-list>
            <mat-list-item>
              <mat-icon matListItemIcon>fingerprint</mat-icon>
              <div matListItemTitle>ID</div>
              <div matListItemLine>{{ data.id }}</div>
            </mat-list-item>
            <mat-list-item>
              <mat-icon matListItemIcon>person</mat-icon>
              <div matListItemTitle>Full Name</div>
              <div matListItemLine>{{ data.firstName }} {{ data.lastName }}</div>
            </mat-list-item>
            <mat-list-item>
              <mat-icon matListItemIcon>email</mat-icon>
              <div matListItemTitle>Email</div>
              <div matListItemLine>{{ data.email }}</div>
            </mat-list-item>
            <mat-list-item>
              <mat-icon matListItemIcon>work</mat-icon>
              <div matListItemTitle>Account Type</div>
              <div matListItemLine>{{ data.type }}</div>
            </mat-list-item>
          </mat-list>
        </div>

        <mat-divider></mat-divider>

        <div class="details-section">
          <h4><mat-icon>schedule</mat-icon> Activity</h4>
          <mat-list>
            <mat-list-item>
              <mat-icon matListItemIcon>event</mat-icon>
              <div matListItemTitle>Registration Date</div>
              <div matListItemLine>{{ data.createdAt | date: 'dd/MM/yyyy at HH:mm' }}</div>
            </mat-list-item>
            <mat-list-item>
              <mat-icon matListItemIcon>login</mat-icon>
              <div matListItemTitle>Last Login</div>
              <div matListItemLine>
                {{ data.lastLogin ? (data.lastLogin | date: 'dd/MM/yyyy at HH:mm') : 'Never logged in' }}
              </div>
            </mat-list-item>
          </mat-list>
        </div>

        <mat-divider></mat-divider>

        <div class="details-section">
          <h4><mat-icon>info</mat-icon> Account Status</h4>
          <div class="status-details">
            <div class="status-item">
              <mat-icon [class.status-icon-active]="data.status === UserStatus.ACTIVE"
                        [class.status-icon-inactive]="data.status !== UserStatus.ACTIVE">
                {{ data.status === UserStatus.ACTIVE ? 'check_circle' : 'cancel' }}
              </mat-icon>
              <div>
                <strong>Current status:</strong>
                <span class="status-text">{{ getStatusLabel(data.status) }}</span>
              </div>
            </div>
            @if (data.status === UserStatus.ACTIVE) {
              <p class="status-description">
                <mat-icon>check</mat-icon>
                This account is active and the user can log in normally.
              </p>
            } @else if (data.status === UserStatus.SUSPENDED) {
              <p class="status-description warning">
                <mat-icon>warning</mat-icon>
                This account is suspended. The user cannot log in.
              </p>
            } @else if (data.status === UserStatus.PENDING_VERIFICATION) {
              <p class="status-description info">
                <mat-icon>schedule</mat-icon>
                This account is pending verification.
              </p>
            } @else if (data.status === UserStatus.DELETED) {
              <p class="status-description error">
                <mat-icon>delete</mat-icon>
                This account has been deleted (soft delete).
              </p>
            }
          </div>
        </div>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button (click)="close()">Close</button>
        <button mat-raised-button color="primary" (click)="close()">
          <mat-icon>edit</mat-icon>
          Edit
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .user-details-dialog {
      min-width: 500px;
      max-width: 600px;
    }

    h2 {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 0;
      color: #1976d2;
    }

    .user-header {
      display: flex;
      gap: 20px;
      padding: 20px 0;
      align-items: center;
    }

    .user-avatar-large {
      flex-shrink: 0;
    }

    .user-avatar-large mat-icon {
      font-size: 80px;
      width: 80px;
      height: 80px;
      color: #1976d2;
    }

    .user-main-info {
      flex: 1;
    }

    .user-main-info h3 {
      margin: 0 0 8px 0;
      font-size: 24px;
      font-weight: 500;
    }

    .user-email {
      margin: 0 0 12px 0;
      color: #666;
      font-size: 14px;
    }

    .user-badges {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }

    .user-badges mat-chip {
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }

    .user-badges mat-chip mat-icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
    }

    .details-section {
      padding: 16px 0;
    }

    .details-section h4 {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 0 0 12px 0;
      color: #333;
      font-size: 16px;
      font-weight: 500;
    }

    .details-section mat-icon {
      color: #1976d2;
    }

    mat-list {
      padding: 0;
    }

    mat-list-item {
      height: auto !important;
      padding: 8px 0;
    }

    .status-details {
      padding: 12px;
      background: #f5f5f5;
      border-radius: 8px;
    }

    .status-item {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;
    }

    .status-item mat-icon {
      font-size: 32px;
      width: 32px;
      height: 32px;
    }

    .status-icon-active {
      color: #4caf50;
    }

    .status-icon-inactive {
      color: #f44336;
    }

    .status-text {
      display: block;
      margin-top: 4px;
      font-weight: 600;
    }

    .status-description {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px;
      margin: 0;
      background: white;
      border-radius: 4px;
      border-left: 4px solid #4caf50;
    }

    .status-description.warning {
      border-left-color: #ff9800;
      background: #fff3e0;
    }

    .status-description.info {
      border-left-color: #2196f3;
      background: #e3f2fd;
    }

    .status-description.error {
      border-left-color: #f44336;
      background: #ffebee;
    }

    .status-description mat-icon {
      font-size: 20px;
      width: 20px;
      height: 20px;
    }

    .status-active {
      background-color: #4caf50 !important;
      color: white !important;
    }

    .status-suspended {
      background-color: #ff9800 !important;
      color: white !important;
    }

    .status-pending {
      background-color: #2196f3 !important;
      color: white !important;
    }

    .status-deleted {
      background-color: #f44336 !important;
      color: white !important;
    }

    mat-dialog-actions {
      padding: 16px 0 0 0;
      margin: 0;
    }
  `]
})
export class UserDetailsDialogComponent {
  UserStatus = UserStatus;
  UserRole = UserRole;

  constructor(
    public dialogRef: MatDialogRef<UserDetailsDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: UserListItem
  ) {}

  getRoleColor(role: UserRole): string {
    const colors: Record<UserRole, string> = {
      [UserRole.ADMIN]: 'primary',
      [UserRole.FREELANCER]: 'accent',
      [UserRole.CLIENT]: 'warn',
      [UserRole.SUPPORT_AGENT]: 'basic'
    };
    return colors[role] || 'default';
  }

  getRoleIcon(role: UserRole): string {
    const icons: Record<UserRole, string> = {
      [UserRole.ADMIN]: 'admin_panel_settings',
      [UserRole.FREELANCER]: 'work',
      [UserRole.CLIENT]: 'business',
      [UserRole.SUPPORT_AGENT]: 'support_agent'
    };
    return icons[role] || 'person';
  }

  getStatusIcon(status: UserStatus): string {
    const icons: Record<UserStatus, string> = {
      [UserStatus.ACTIVE]: 'check_circle',
      [UserStatus.SUSPENDED]: 'pause_circle',
      [UserStatus.PENDING_VERIFICATION]: 'schedule',
      [UserStatus.DELETED]: 'cancel'
    };
    return icons[status] || 'help';
  }

  getStatusLabel(status: UserStatus): string {
    const labels: Record<UserStatus, string> = {
      [UserStatus.ACTIVE]: 'Active',
      [UserStatus.SUSPENDED]: 'Suspended',
      [UserStatus.PENDING_VERIFICATION]: 'Pending Verification',
      [UserStatus.DELETED]: 'Deleted'
    };
    return labels[status] || status;
  }

  close(): void {
    this.dialogRef.close();
  }
}
