import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule, NativeDateAdapter, DateAdapter, MAT_DATE_LOCALE, MAT_DATE_FORMATS, MAT_NATIVE_DATE_FORMATS } from '@angular/material/core';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { JobOfferService } from '../../../core/services/job-offer.service';
import { JobOffer, JobOfferStatus } from '@core/models/job-offer.model';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-invite-freelancer-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatIconModule,
    TranslateModule
  ],
  providers: [
    { provide: DateAdapter, useClass: NativeDateAdapter },
    { provide: MAT_DATE_LOCALE, useValue: 'en-US' },
    { provide: MAT_DATE_FORMATS, useValue: MAT_NATIVE_DATE_FORMATS }
  ],
  templateUrl: './invite-freelancer-modal.component.html',
  styleUrl: './invite-freelancer-modal.component.scss'
})
export class InviteFreelancerModalComponent implements OnInit {
  inviteForm!: FormGroup;
  myJobOffers: JobOffer[] = [];
  isLoading = false;
  minDate = new Date();

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<InviteFreelancerModalComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { freelancer: any },
    private jobOfferService: JobOfferService,
    private authService: AuthService
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.loadMyJobOffers();
  }

  initForm(): void {
    this.inviteForm = this.fb.group({
      jobOfferId: ['', [Validators.required]],
      message: ['', [Validators.required, Validators.minLength(20)]],
      proposedBudget: ['', [Validators.required, Validators.min(0)]],
      deadlineResponse: ['', [Validators.required]]
    });
  }

  loadMyJobOffers(): void {
    this.isLoading = true;
    this.jobOfferService.getMyJobOffers().subscribe({
      next: (offers) => {
        // Filter only OPEN offers
        this.myJobOffers = offers.filter(
          offer => offer.status === JobOfferStatus.OPEN
        );
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading job offers:', error);
        this.isLoading = false;
      }
    });
  }

  onJobSelected(jobOfferId: string): void {
    const selectedJob = this.myJobOffers.find(job => job.id === jobOfferId);
    if (selectedJob && selectedJob.budget) {
      // Pre-fill budget with job's budget
      this.inviteForm.patchValue({
        proposedBudget: selectedJob.budget
      });
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSubmit(): void {
    if (this.inviteForm.invalid) {
      return;
    }

    // Get current client ID from authenticated user
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser) {
      console.error('❌ No authenticated user found');
      return;
    }

    console.log('✅ Current user:', currentUser);
    console.log('📋 Form values:', this.inviteForm.value);

    const invitationData = {
      clientId: currentUser.id,
      freelanceId: this.data.freelancer.userId || this.data.freelancer.id,
      ...this.inviteForm.value
    };

    console.log('📤 Sending invitation data:', invitationData);

    this.dialogRef.close(invitationData);
  }
}
