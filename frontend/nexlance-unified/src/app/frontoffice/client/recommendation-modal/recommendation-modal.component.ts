import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule, NativeDateAdapter, DateAdapter, MAT_DATE_LOCALE, MAT_DATE_FORMATS, MAT_NATIVE_DATE_FORMATS } from '@angular/material/core';
import { JobOfferService } from '@core/services/job-offer.service';
import { AuthService } from '@core/services/auth.service';
import { RecommendationService } from '@core/services/recommendation.service';
import { JobOffer } from '@core/models/job-offer.model';

interface Freelancer {
  id: string;
  userId?: string;
  name: string;
  title: string;
  rating: number;
  reviewCount: number;
  hourlyRate: number;
  location: string;
}

@Component({
  selector: 'app-recommendation-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    MatDatepickerModule,
    MatNativeDateModule
  ],
  providers: [
    { provide: DateAdapter, useClass: NativeDateAdapter },
    { provide: MAT_DATE_LOCALE, useValue: 'en-US' },
    { provide: MAT_DATE_FORMATS, useValue: MAT_NATIVE_DATE_FORMATS }
  ],
  templateUrl: './recommendation-modal.component.html',
  styleUrl: './recommendation-modal.component.scss'
})
export class RecommendationModalComponent implements OnInit {
  recommendationForm!: FormGroup;
  defaultDeadline: Date;
  jobOffers: JobOffer[] = [];
  isLoadingOffers = true;
  
  constructor(
    private fb: FormBuilder,
    private jobOfferService: JobOfferService,
    private authService: AuthService,
    private recommendationService: RecommendationService,
    public dialogRef: MatDialogRef<RecommendationModalComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { freelancer: Freelancer }
  ) {
    // Default deadline: +7 days
    this.defaultDeadline = new Date();
    this.defaultDeadline.setDate(this.defaultDeadline.getDate() + 7);
  }

  ngOnInit() {
    this.loadJobOffers();
    
    this.recommendationForm = this.fb.group({
      jobOfferId: ['', Validators.required],
      proposedBudget: [''],
      message: ['', [Validators.required, Validators.maxLength(1000)]],
      expirationDate: [this.defaultDeadline, Validators.required]
    });

    // Set default message
    this.recommendationForm.get('message')?.setValue(
      `Hello ${this.data.freelancer.name},\n\nI noticed your profile and your skills in ${this.data.freelancer.title}. I think you would be perfect for my project...`
    );
  }

  loadJobOffers() {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.id) {
      console.error('User not authenticated');
      this.isLoadingOffers = false;
      return;
    }

    // Load client's job offers
    this.jobOfferService.getMyJobOffers().subscribe({
      next: (offers) => {
        // Filter only OPEN offers (accepted/in-progress are excluded)
        const openOffers = offers.filter(offer => offer.status === 'OPEN');
        
        // Duplicate recommendations are handled by the create endpoint with 409.
        // Avoid a preflight recommendations request here so the modal still works
        // when the optional recommendations listing endpoint is unavailable.
        this.jobOffers = openOffers;
        this.isLoadingOffers = false;
      },
      error: (error) => {
        console.error('Error loading job offers:', error);
        this.isLoadingOffers = false;
      }
    });
  }

  onJobOfferChange(jobOfferId: string) {
    const selectedOffer = this.jobOffers.find(offer => offer.id === jobOfferId);
    if (selectedOffer && selectedOffer.budget) {
      // Set proposed budget based on job offer budget
      this.recommendationForm.get('proposedBudget')?.setValue(selectedOffer.budget);
    }
  }

  getCharacterCount(): number {
    return this.recommendationForm.get('message')?.value?.length || 0;
  }

  onCancel() {
    this.dialogRef.close();
  }

  onSubmit() {
    if (this.recommendationForm.valid) {
      const currentUser = this.authService.getCurrentUser();
      if (!currentUser?.id) {
        alert('User not authenticated');
        return;
      }

      // Always prefer userId (User ID) over id (FreelanceProfile ID)
      // so the freelancer can find their recommendations by their login ID
      const freelanceId = this.data.freelancer.userId || this.data.freelancer.id;
      console.log('📤 [Recommendation] Creating with freelanceId:', freelanceId,
        '(userId:', this.data.freelancer.userId, ', profileId:', this.data.freelancer.id, ')');

      const recommendation = {
        clientId: currentUser.id,
        clientName: currentUser.firstName + ' ' + currentUser.lastName,
        freelanceId: freelanceId,
        jobOfferId: this.recommendationForm.value.jobOfferId,
        message: this.recommendationForm.value.message,
        proposedBudget: this.recommendationForm.value.proposedBudget || null,
        expirationDate: this.recommendationForm.value.expirationDate
      };
      
      this.dialogRef.close(recommendation);
    }
  }

  getBudgetRange(offerId: string): string {
    const offer = this.jobOffers.find(o => o.id === offerId);
    if (!offer || !offer.budget) return '';
    
    return `${offer.budget.toLocaleString()} DT`;
  }
}
