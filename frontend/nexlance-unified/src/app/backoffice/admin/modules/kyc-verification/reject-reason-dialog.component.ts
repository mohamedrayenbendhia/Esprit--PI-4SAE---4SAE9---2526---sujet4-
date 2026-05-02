import { Component, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { KYCVerification } from '../../../../core/models/kyc-verification.model';

@Component({
  selector: 'app-reject-reason-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule
  ],
  template: `
    <div class="reject-dialog">
      <h2 mat-dialog-title>
        <mat-icon color="warn">error</mat-icon>
        Reject Document
      </h2>
      
      <mat-dialog-content>
        <p class="warning-text">
          You are about to reject the document of user <strong>{{ data.verification.userId }}</strong>.
        </p>
        
        <form [formGroup]="rejectForm">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Rejection reason *</mat-label>
            <textarea 
              matInput 
              formControlName="reason" 
              rows="4"
              placeholder="Clearly explain why this document is rejected...">
            </textarea>
            <mat-hint>This reason will be visible to the user</mat-hint>
            <mat-error *ngIf="rejectForm.get('reason')?.hasError('required')">
              The reason is required
            </mat-error>
            <mat-error *ngIf="rejectForm.get('reason')?.hasError('minlength')">
              Minimum 10 characters
            </mat-error>
          </mat-form-field>
        </form>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button (click)="cancel()">Cancel</button>
        <button 
          mat-raised-button 
          color="warn" 
          [disabled]="rejectForm.invalid"
          (click)="confirm()">
          <mat-icon>close</mat-icon>
          Confirm Rejection
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .reject-dialog {
      h2 {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        margin: 0;
      }

      mat-dialog-content {
        padding: 1.5rem 0;
        min-width: 400px;

        .warning-text {
          margin-bottom: 1.5rem;
          padding: 1rem;
          background: #fef2f2;
          border-left: 4px solid #ef4444;
          border-radius: 4px;
          color: #991b1b;
        }

        .full-width {
          width: 100%;
        }
      }

      mat-dialog-actions {
        padding-top: 1rem;
        border-top: 1px solid #e9ecef;
      }
    }
  `]
})
export class RejectReasonDialogComponent {
  private fb = inject(FormBuilder);
  private dialogRef = inject(MatDialogRef<RejectReasonDialogComponent>);

  rejectForm: FormGroup;

  constructor(@Inject(MAT_DIALOG_DATA) public data: { verification: KYCVerification }) {
    this.rejectForm = this.fb.group({
      reason: ['', [Validators.required, Validators.minLength(10)]]
    });
  }

  confirm(): void {
    if (this.rejectForm.valid) {
      this.dialogRef.close(this.rejectForm.value.reason);
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
