import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ChangePasswordComponent } from '../../../components/change-password/change-password.component';
import { Setup2FAModalComponent } from '../../../components/setup-2fa-modal/setup-2fa-modal.component';
import { SecurityService } from '../../../../core/services/security.service';

@Component({
  selector: 'app-security-settings',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTabsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatDialogModule,
    MatSnackBarModule,
    ChangePasswordComponent
  ],
  templateUrl: './security-settings.component.html',
  styleUrls: ['./security-settings.component.scss']
})
export class SecuritySettingsComponent implements OnInit {
  private securityService = inject(SecurityService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
  private fb = inject(FormBuilder);

  twoFactorEnabled = signal(false);
  isLoading = signal(false);
  showDisableForm = signal(false);
  showRegenerateForm = signal(false);

  disableForm: FormGroup;
  regenerateForm: FormGroup;

  constructor() {
    this.disableForm = this.fb.group({
      password: ['', Validators.required]
    });

    this.regenerateForm = this.fb.group({
      password: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.check2FAStatus();
  }

  check2FAStatus(): void {
    this.isLoading.set(true);
    this.securityService.get2FAStatus().subscribe({
      next: (response) => {
        this.twoFactorEnabled.set(response.enabled);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  openSetup2FAModal(): void {
    const dialogRef = this.dialog.open(Setup2FAModalComponent, {
      width: '600px',
      disableClose: true
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.check2FAStatus();
      }
    });
  }

  disable2FA(): void {
    if (this.disableForm.invalid) return;

    this.isLoading.set(true);
    const password = this.disableForm.value.password;

    this.securityService.disable2FA(password).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        this.showDisableForm.set(false);
        this.disableForm.reset();
        this.twoFactorEnabled.set(false);
        this.snackBar.open(response.message, 'Close', {
          duration: 5000,
          panelClass: ['success-snackbar']
        });
      },
      error: (error) => {
        this.isLoading.set(false);
        this.snackBar.open(error.error?.error || 'Failed to disable 2FA', 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  regenerateBackupCodes(): void {
    if (this.regenerateForm.invalid) return;

    this.isLoading.set(true);
    const password = this.regenerateForm.value.password;

    this.securityService.regenerateBackupCodes(password).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        this.showRegenerateForm.set(false);
        this.regenerateForm.reset();
        
        // Show backup codes
        const codes = response.backupCodes.join('\n');
        navigator.clipboard.writeText(codes);
        
        this.snackBar.open('Backup codes regenerated and copied to clipboard!', 'Close', {
          duration: 5000,
          panelClass: ['success-snackbar']
        });
      },
      error: (error) => {
        this.isLoading.set(false);
        this.snackBar.open(error.error?.error || 'Failed to regenerate codes', 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }
}
