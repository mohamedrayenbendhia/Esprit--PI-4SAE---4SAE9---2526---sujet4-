import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { SecurityService } from '../../../core/services/security.service';

@Component({
  selector: 'app-setup-2fa-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  template: `
    <div class="setup-2fa-modal">
      <h2 mat-dialog-title>
        <mat-icon>security</mat-icon>
        Setup Two-Factor Authentication
      </h2>

      <mat-dialog-content>
        <div *ngIf="step() === 1" class="step-1">
          <p class="instructions">
            Two-factor authentication adds an extra layer of security to your account.
          </p>
          <div class="step-info">
            <mat-icon>info</mat-icon>
            <span>You'll need an authenticator app like Google Authenticator or Authy.</span>
          </div>
          <button mat-raised-button color="primary" (click)="initiate2FASetup()" [disabled]="isLoading()">
            {{ isLoading() ? 'Loading...' : 'Continue' }}
          </button>
        </div>

        <div *ngIf="step() === 2" class="step-2">
          <p class="instructions">
            Scan this QR code with your authenticator app:
          </p>
          
          <div class="qr-code-container" *ngIf="qrCode()">
            <img [src]="qrCode()" alt="QR Code for 2FA" />
          </div>

          <div class="secret-container">
            <p class="secret-label">Or enter this code manually:</p>
            <div class="secret-code">
              <code>{{ secret() }}</code>
              <button mat-icon-button (click)="copySecret()">
                <mat-icon>content_copy</mat-icon>
              </button>
            </div>
          </div>

          <div class="current-code-container" *ngIf="currentCode()">
            <p class="current-code-label">Current verification code:</p>
            <div class="current-code-value">
              <span class="code-digits">{{ currentCode() }}</span>
              <button mat-icon-button (click)="useCurrentCode()" matTooltip="Use this code">
                <mat-icon>arrow_downward</mat-icon>
              </button>
            </div>
            <p class="current-code-hint">This code changes every 30 seconds</p>
          </div>

          <form [formGroup]="verificationForm" (ngSubmit)="enable2FA()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Enter Verification Code</mat-label>
              <input
                matInput
                formControlName="code"
                placeholder="000000"
                maxlength="6"
                autocomplete="off"
              />
              <mat-error *ngIf="verificationForm.get('code')?.hasError('required')">
                Code is required
              </mat-error>
              <mat-error *ngIf="verificationForm.get('code')?.hasError('pattern')">
                Code must be 6 digits
              </mat-error>
            </mat-form-field>

            <div class="actions">
              <button mat-button type="button" (click)="step.set(1)">Back</button>
              <button
                mat-raised-button
                color="primary"
                type="submit"
                [disabled]="verificationForm.invalid || isLoading()"
              >
                {{ isLoading() ? 'Verifying...' : 'Enable 2FA' }}
              </button>
            </div>
          </form>
        </div>

        <div *ngIf="step() === 3" class="step-3">
          <div class="success-message">
            <mat-icon class="success-icon">check_circle</mat-icon>
            <h3>2FA Enabled Successfully!</h3>
          </div>

          <div class="backup-codes-container">
            <p class="warning">
              <mat-icon>warning</mat-icon>
              <strong>Important:</strong> Save these backup codes in a safe place. 
              You can use them to access your account if you lose your phone.
            </p>
            
            <div class="backup-codes">
              <div *ngFor="let code of backupCodes()" class="backup-code">
                {{ code }}
              </div>
            </div>

            <button mat-stroked-button (click)="copyBackupCodes()">
              <mat-icon>content_copy</mat-icon>
              Copy All Codes
            </button>
          </div>

          <div class="final-actions">
            <button mat-raised-button color="primary" (click)="close()">
              Done
            </button>
          </div>
        </div>
      </mat-dialog-content>
    </div>
  `,
  styles: [`
    .setup-2fa-modal {
      padding: 1rem;
      max-width: 600px;

      h2 {
        display: flex;
        align-items: center;
        gap: 12px;
        color: #2c3e50;
        
        mat-icon {
          color: #673ab7;
        }
      }
    }

    mat-dialog-content {
      padding: 2rem 0;
      min-height: 300px;
    }

    .instructions {
      text-align: center;
      color: #7f8c8d;
      margin-bottom: 1.5rem;
      line-height: 1.6;
    }

    .step-info {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 1rem;
      background-color: #e8eaf6;
      border-radius: 8px;
      margin-bottom: 2rem;
      color: #673ab7;

      mat-icon {
        flex-shrink: 0;
      }
    }

    .step-1 {
      text-align: center;

      button {
        min-width: 200px;
      }
    }

    .step-2 {
      .qr-code-container {
        display: flex;
        justify-content: center;
        margin: 2rem 0;
        
        img {
          max-width: 250px;
          border: 2px solid #e0e0e0;
          border-radius: 8px;
          padding: 1rem;
          background: white;
        }
      }

      .secret-container {
        margin-bottom: 2rem;
        
        .secret-label {
          text-align: center;
          color: #7f8c8d;
          font-size: 14px;
          margin-bottom: 1rem;
        }

        .secret-code {
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 8px;
          padding: 1rem;
          background-color: #f8f9fa;
          border-radius: 4px;
          
          code {
            font-family: monospace;
            font-size: 16px;
            color: #2c3e50;
            letter-spacing: 2px;
          }
        }
      }

      .current-code-container {
        margin-bottom: 2rem;
        text-align: center;
        padding: 1rem;
        background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
        border-radius: 8px;
        border: 1px solid #a5d6a7;

        .current-code-label {
          color: #2e7d32;
          font-size: 14px;
          font-weight: 600;
          margin-bottom: 8px;
        }

        .current-code-value {
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 8px;

          .code-digits {
            font-family: monospace;
            font-size: 32px;
            font-weight: 700;
            color: #1b5e20;
            letter-spacing: 6px;
          }
        }

        .current-code-hint {
          font-size: 12px;
          color: #689f38;
          margin-top: 6px;
          margin-bottom: 0;
        }
      }

      .full-width {
        width: 100%;
      }

      .actions {
        display: flex;
        justify-content: space-between;
        margin-top: 1rem;
      }
    }

    .step-3 {
      .success-message {
        text-align: center;
        margin-bottom: 2rem;

        .success-icon {
          font-size: 64px;
          width: 64px;
          height: 64px;
          color: #4caf50;
          margin-bottom: 1rem;
        }

        h3 {
          color: #2c3e50;
        }
      }

      .backup-codes-container {
        .warning {
          display: flex;
          align-items: flex-start;
          gap: 8px;
          padding: 1rem;
          background-color: #fff3cd;
          border-left: 4px solid #ffc107;
          border-radius: 4px;
          margin-bottom: 1.5rem;

          mat-icon {
            color: #ff9800;
            flex-shrink: 0;
          }
        }

        .backup-codes {
          display: grid;
          grid-template-columns: repeat(2, 1fr);
          gap: 12px;
          margin-bottom: 1.5rem;
          padding: 1rem;
          background-color: #f8f9fa;
          border-radius: 8px;

          .backup-code {
            font-family: monospace;
            font-size: 16px;
            padding: 8px;
            background-color: white;
            border: 1px solid #e0e0e0;
            border-radius: 4px;
            text-align: center;
          }
        }

        button {
          display: block;
          margin: 0 auto;
        }
      }

      .final-actions {
        text-align: center;
        margin-top: 2rem;

        button {
          min-width: 200px;
        }
      }
    }
  `]
})
export class Setup2FAModalComponent {
  private fb = inject(FormBuilder);
  private securityService = inject(SecurityService);
  private dialogRef = inject(MatDialogRef<Setup2FAModalComponent>);
  private snackBar = inject(MatSnackBar);

  step = signal(1);
  isLoading = signal(false);
  qrCode = signal<string>('');
  secret = signal<string>('');
  currentCode = signal<string>('');
  backupCodes = signal<string[]>([]);

  verificationForm: FormGroup;

  constructor() {
    this.verificationForm = this.fb.group({
      code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
    });
  }

  initiate2FASetup(): void {
    this.isLoading.set(true);
    this.securityService.setup2FA().subscribe({
      next: (response) => {
        this.qrCode.set(response.qrCode);
        this.secret.set(response.secret);
        this.currentCode.set(response.currentCode || '');
        this.step.set(2);
        this.isLoading.set(false);
      },
      error: (error) => {
        this.isLoading.set(false);
        this.snackBar.open(error.error?.error || 'Failed to setup 2FA', 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  enable2FA(): void {
    if (this.verificationForm.invalid) return;

    this.isLoading.set(true);
    const code = this.verificationForm.value.code;

    this.securityService.enable2FA(code).subscribe({
      next: (response) => {
        this.backupCodes.set(response.backupCodes);
        this.step.set(3);
        this.isLoading.set(false);
      },
      error: (error) => {
        this.isLoading.set(false);
        this.snackBar.open(error.error?.error || 'Invalid verification code', 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  copySecret(): void {
    navigator.clipboard.writeText(this.secret());
    this.snackBar.open('Secret copied to clipboard', 'Close', { duration: 2000 });
  }

  useCurrentCode(): void {
    const code = this.currentCode();
    if (code) {
      this.verificationForm.patchValue({ code });
    }
  }

  copyBackupCodes(): void {
    const codes = this.backupCodes().join('\n');
    navigator.clipboard.writeText(codes);
    this.snackBar.open('Backup codes copied to clipboard', 'Close', { duration: 2000 });
  }

  close(): void {
    this.dialogRef.close(true);
  }
}
