import { Component, Input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { OrganizationService } from '@core/services/organization.service';
import { CreateApplicationRequest } from '@core/models/organization.model';
import { AuthService } from '@core/services/auth.service';
import { UserRole } from '@shared/models/user.model';

@Component({
  selector: 'app-apply-to-org',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSnackBarModule],
  template: `
    @if (sent()) {
      <div class="success-msg"><mat-icon>check_circle</mat-icon> Application sent successfully!</div>
    } @else if (!showForm()) {
      <button mat-raised-button color="primary" (click)="showForm.set(true)">
        <mat-icon>person_add</mat-icon> Apply to Organization
      </button>
    } @else {
      <div class="apply-form">
        <h4><mat-icon>person_add</mat-icon> Apply to Organization</h4>
        <mat-form-field appearance="outline" style="width:100%">
          <mat-label>Motivation message *</mat-label>
          <textarea matInput rows="4" [(ngModel)]="message"
                    placeholder="Introduce yourself and explain your interest..."></textarea>
        </mat-form-field>
        <mat-form-field appearance="outline" style="width:100%">
          <mat-label>CV link (optional)</mat-label>
          <input matInput [(ngModel)]="cvUrl" placeholder="https://...">
          <mat-icon matPrefix>attach_file</mat-icon>
        </mat-form-field>
        <div class="actions">
          <button mat-button (click)="showForm.set(false)">Cancel</button>
          <button mat-raised-button color="primary" [disabled]="sending() || !message.trim()" (click)="submit()">
            <mat-icon>send</mat-icon> {{ sending() ? 'Sending...' : 'Submit' }}
          </button>
        </div>
      </div>
    }
  `,
  styles: [`
    .apply-form { border: 1px solid #e5e7eb; border-radius: 12px; padding: 16px; margin-top: 8px; }
    .apply-form h4 { display: flex; align-items: center; gap: 6px; margin: 0 0 12px; font-size: 0.95rem; }
    .actions { display: flex; gap: 8px; justify-content: flex-end; }
    .success-msg { display: flex; align-items: center; gap: 6px; color: #059669; font-weight: 500; padding: 8px 0; }
  `]
})
export class ApplyToOrgComponent {
  @Input() orgId!: string;

  showForm = signal(false);
  sending  = signal(false);
  sent     = signal(false);
  message  = '';
  cvUrl    = '';

  constructor(
    private orgService: OrganizationService,
    private authService: AuthService,
    private snack: MatSnackBar
  ) {}

  submit() {
    if (this.authService.getCurrentUser()?.role !== UserRole.FREELANCER) {
      this.snack.open('Only freelancers can apply to join an organization.', 'OK', { duration: 3000 });
      return;
    }
    this.sending.set(true);
    const req: CreateApplicationRequest = { message: this.message, cvUrl: this.cvUrl || undefined };
    this.orgService.applyToOrg(this.orgId, req).subscribe({
      next: () => {
        this.sent.set(true);
        this.sending.set(false);
        this.showForm.set(false);
      },
      error: err => {
        this.snack.open(err?.error?.message ?? 'Failed to submit application.', 'OK', { duration: 3000 });
        this.sending.set(false);
      }
    });
  }
}
