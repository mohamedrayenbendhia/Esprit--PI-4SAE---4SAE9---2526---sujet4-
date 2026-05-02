import { Component, Inject, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { KycService } from '../../../../core/services/kyc.service';
import { KYCVerification, KYCStatus } from '../../../../core/models/kyc-verification.model';

@Component({
  selector: 'app-kyc-review-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  template: `
    <div class="kyc-review-dialog">
      <div class="dialog-header">
        <h2 mat-dialog-title>
          <mat-icon>verified_user</mat-icon>
          KYC Review - {{ getDocumentTypeLabel(data.documentType) }}
        </h2>
        <button mat-icon-button (click)="close()">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <mat-dialog-content>
        <div class="user-section">
          <div class="info-row">
            <span class="label">User ID:</span>
            <span class="value">{{ data.userId }}</span>
          </div>
          <div class="info-row">
            <span class="label">Submission date:</span>
            <span class="value">{{ data.submittedAt | date:'dd/MM/yyyy at HH:mm' }}</span>
          </div>
          <div class="info-row">
            <span class="label">Document type:</span>
            <span class="value">{{ getDocumentTypeLabel(data.documentType) }}</span>
          </div>
          <div class="info-row">
            <span class="label">Waiting time:</span>
            <span class="value waiting">{{ getWaitingTime(data.submittedAt) }}</span>
          </div>
        </div>

        <div class="document-section">
          <div class="document-viewer">
            <img 
              [src]="getDocumentUrl()" 
              alt="Document" 
              [class.zoomed]="isZoomed()"
              (click)="toggleZoom()"
            />
            <button 
              mat-mini-fab 
              color="primary" 
              class="zoom-btn"
              (click)="toggleZoom()"
              [matTooltip]="isZoomed() ? 'Zoom out' : 'Zoom in'">
              <mat-icon>{{ isZoomed() ? 'zoom_out' : 'zoom_in' }}</mat-icon>
            </button>
          </div>
        </div>

        <form [formGroup]="reviewForm" class="review-form">
          <h3>Review Decision</h3>
          
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Decision</mat-label>
            <mat-select formControlName="status" (selectionChange)="onStatusChange()">
              <mat-option [value]="KYCStatus.APPROVED">
                <mat-icon class="status-icon approved">check_circle</mat-icon>
                Approve
              </mat-option>
              <mat-option [value]="KYCStatus.REJECTED">
                <mat-icon class="status-icon rejected">cancel</mat-icon>
                Reject
              </mat-option>
            </mat-select>
          </mat-form-field>

          @if (reviewForm.get('status')?.value === KYCStatus.REJECTED) {
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Rejection reason *</mat-label>
              <textarea 
                matInput 
                formControlName="rejectionReason" 
                rows="3"
                placeholder="Explain why this document is rejected...">
              </textarea>
              <mat-error *ngIf="reviewForm.get('rejectionReason')?.hasError('required')">
                Rejection reason is required
              </mat-error>
            </mat-form-field>
          }

          @if (reviewForm.get('status')?.value === KYCStatus.APPROVED) {
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Expiry date (optional)</mat-label>
              <input matInput [matDatepicker]="picker" formControlName="expiryDate">
              <mat-datepicker-toggle matSuffix [for]="picker"></mat-datepicker-toggle>
              <mat-datepicker #picker></mat-datepicker>
              <mat-hint>Leave empty if the document does not expire</mat-hint>
            </mat-form-field>
          }

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Internal notes (optional)</mat-label>
            <textarea 
              matInput 
              formControlName="notes" 
              rows="2"
              placeholder="Notes for the admin team...">
            </textarea>
          </mat-form-field>
        </form>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button (click)="close()">Cancel</button>
        <button 
          mat-raised-button 
          [color]="reviewForm.get('status')?.value === KYCStatus.APPROVED ? 'primary' : 'warn'"
          [disabled]="reviewForm.invalid || submitting()"
          (click)="submit()">
          @if (submitting()) {
            <mat-spinner diameter="20"></mat-spinner>
          } @else {
            <ng-container>
              <mat-icon>{{ reviewForm.get('status')?.value === KYCStatus.APPROVED ? 'check' : 'close' }}</mat-icon>
              {{ reviewForm.get('status')?.value === KYCStatus.APPROVED ? 'Approve' : 'Reject' }}
            </ng-container>
          }
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .kyc-review-dialog {
      .dialog-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 1.5rem 1.5rem 0;
        
        h2 {
          display: flex;
          align-items: center;
          gap: 0.75rem;
          margin: 0;
          font-size: 1.5rem;
          color: #2c3e50;
          
          mat-icon {
            color: #667eea;
          }
        }
      }

      mat-dialog-content {
        padding: 1.5rem;
        max-height: 70vh;
        overflow-y: auto;
      }

      .user-section {
        background: linear-gradient(135deg, #f5f7fa 0%, #e9ecef 100%);
        padding: 1.25rem;
        border-radius: 12px;
        margin-bottom: 1.5rem;

        .info-row {
          display: flex;
          justify-content: space-between;
          padding: 0.5rem 0;
          border-bottom: 1px solid rgba(0, 0, 0, 0.05);

          &:last-child {
            border-bottom: none;
          }

          .label {
            font-weight: 500;
            color: #64748b;
          }

          .value {
            color: #2c3e50;
            font-weight: 600;

            &.waiting {
              color: #f59e0b;
            }
          }
        }
      }

      .document-section {
        margin-bottom: 1.5rem;

        .document-viewer {
          position: relative;
          background: #f8f9fa;
          border-radius: 12px;
          overflow: hidden;
          min-height: 300px;
          display: flex;
          align-items: center;
          justify-content: center;

          img {
            max-width: 100%;
            max-height: 500px;
            cursor: zoom-in;
            transition: transform 0.3s;
            border-radius: 8px;

            &.zoomed {
              transform: scale(1.5);
              cursor: zoom-out;
            }
          }

          .zoom-btn {
            position: absolute;
            bottom: 1rem;
            right: 1rem;
            z-index: 10;
          }
        }
      }

      .review-form {
        h3 {
          margin: 0 0 1rem 0;
          font-size: 1.1rem;
          color: #2c3e50;
        }

        .full-width {
          width: 100%;
        }

        .status-icon {
          vertical-align: middle;
          margin-right: 0.5rem;

          &.approved {
            color: #10b981;
          }

          &.rejected {
            color: #ef4444;
          }
        }
      }

      mat-dialog-actions {
        padding: 1rem 1.5rem;
        border-top: 1px solid #e9ecef;

        button {
          mat-spinner {
            display: inline-block;
            margin-right: 0.5rem;
          }
        }
      }
    }
  `]
})
export class KYCReviewDialogComponent {
  private fb = inject(FormBuilder);
  private kycService = inject(KycService);
  private dialogRef = inject(MatDialogRef<KYCReviewDialogComponent>);
  
  reviewForm: FormGroup;
  submitting = signal(false);
  isZoomed = signal(false);
  KYCStatus = KYCStatus;

  constructor(@Inject(MAT_DIALOG_DATA) public data: KYCVerification) {
    this.reviewForm = this.fb.group({
      status: ['', Validators.required],
      rejectionReason: [''],
      expiryDate: [null],
      notes: ['']
    });
  }

  onStatusChange(): void {
    const status = this.reviewForm.get('status')?.value;
    const rejectionReasonControl = this.reviewForm.get('rejectionReason');
    
    if (status === KYCStatus.REJECTED) {
      rejectionReasonControl?.setValidators([Validators.required]);
    } else {
      rejectionReasonControl?.clearValidators();
    }
    rejectionReasonControl?.updateValueAndValidity();
  }

  toggleZoom(): void {
    this.isZoomed.update(v => !v);
  }

  getDocumentUrl(): string {
    return this.kycService.getDocumentUrl(this.data.id);
  }

  getDocumentTypeLabel(type: string): string {
    const labels: { [key: string]: string } = {
      'IDENTITY_CARD': 'Identity Card',
      'PASSPORT': 'Passport',
      'DRIVER_LICENSE': 'Driver\'s License',
      'PROOF_ADDRESS': 'Proof of Address',
      'BANK_STATEMENT': 'Bank Statement'
    };
    return labels[type] || type;
  }

  getWaitingTime(submittedAt: Date): string {
    const now = new Date();
    const submitted = new Date(submittedAt);
    const diff = now.getTime() - submitted.getTime();
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const days = Math.floor(hours / 24);
    
    if (days > 0) {
      return `${days} day${days > 1 ? 's' : ''}`;
    } else {
      return `${hours} hour${hours > 1 ? 's' : ''}`;
    }
  }

  submit(): void {
    if (this.reviewForm.valid) {
      this.submitting.set(true);
      const formValue = this.reviewForm.value;

      this.kycService.reviewDocument(this.data.id, {
        status: formValue.status,
        rejectionReason: formValue.rejectionReason,
        expiryDate: formValue.expiryDate,
        notes: formValue.notes
      }).subscribe({
        next: () => {
          this.submitting.set(false);
          this.dialogRef.close(true);
        },
        error: (err) => {
          console.error('Error during review:', err);
          this.submitting.set(false);
        }
      });
    }
  }

  close(): void {
    this.dialogRef.close();
  }
}
