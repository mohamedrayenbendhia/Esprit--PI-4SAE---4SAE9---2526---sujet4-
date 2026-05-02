import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { TranslateModule } from '@ngx-translate/core';
import { FreelanceProfileService } from '../../../core/services/freelance-profile.service';
import { Availability } from '../../../core/models/freelance-profile.model';
import { HeaderComponent } from '../../../shared/components/header/header.component';


@Component({
  selector: 'app-freelancer-profile',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    TranslateModule,
    MatChipsModule,
    MatSlideToggleModule,
    MatSnackBarModule,
    MatDividerModule,
    MatProgressBarModule,
    HeaderComponent
  ],
  templateUrl: './freelancer-profile.component.html',
  styleUrls: ['./freelancer-profile.component.scss']
})
export class FreelancerProfileComponent implements OnInit {
  private fb = inject(FormBuilder);
  private snackBar = inject(MatSnackBar);
  private profileService = inject(FreelanceProfileService);

  profileForm!: FormGroup;
  isLoading = signal(false);
  profileId = signal<string | null>(null);

  Availability = Availability;
  
  availableLanguages = [
    'French', 'English', 'Spanish', 'German', 'Italian', 
    'Arabic', 'Portuguese', 'Chinese', 'Japanese', 'Russian'
  ];

  availableTimezones = [
    'Europe/Paris', 'Europe/London', 'America/New_York', 'America/Los_Angeles',
    'Asia/Tokyo', 'Asia/Dubai', 'Australia/Sydney'
  ];

  // Statistics
  completionRate = signal(85);
  averageResponseTime = signal(2.5);
  totalProjects = signal(12);
  rating = signal(4.8);

  ngOnInit(): void {
    this.initForm();
    this.loadProfile();
  }

  initForm(): void {
    this.profileForm = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(5)]],
      bio: ['', [Validators.required, Validators.minLength(50)]],
      hourlyRate: [0, [Validators.required, Validators.min(1)]],
      availability: [Availability.AVAILABLE, Validators.required],
      experienceYears: [0, [Validators.required, Validators.min(0), Validators.max(50)]],
      location: ['', Validators.required],
      timezone: ['Europe/Paris', Validators.required],
      languages: [[], Validators.required]
    });
  }

  loadProfile(): void {
    this.isLoading.set(true);
    
    // Load profile from backend API
    this.profileService.getMyProfile().subscribe({
      next: (profile) => {
        console.log('✓ Freelance profile loaded:', profile);
        this.profileId.set(profile.id);
        
        // Patch form values
        this.profileForm.patchValue({
          title: profile.title,
          bio: profile.bio,
          hourlyRate: profile.hourlyRate,
          availability: profile.availability,
          experienceYears: profile.experienceYears,
          location: profile.location,
          timezone: profile.timezone,
          languages: profile.languages || []
        });

        // Update statistics
        this.completionRate.set(profile.completionRate || 0);
        this.averageResponseTime.set(profile.responseTime || 0);
        
        this.isLoading.set(false);
      },
      error: (error) => {
        this.isLoading.set(false);
        
        if (error.status === 404) {
          // No profile found - this is normal for new freelancers
          console.log('ℹ️ No professional profile found yet - please create one');
          this.snackBar.open('Welcome! Please create your professional profile to get started', 'OK', {
            duration: 5000
          });
        } else {
          // Only log actual errors (not 404)
          console.error('❌ Error loading profile:', error);
          this.snackBar.open('Error loading profile', 'Close', {
            duration: 3000,
            panelClass: ['error-snackbar']
          });
        }
      }
    });
  }

  getAvailabilityLabel(status: Availability): string {
    const labels = {
      [Availability.AVAILABLE]: 'Available',
      [Availability.BUSY]: 'Busy',
      [Availability.UNAVAILABLE]: 'Unavailable'
    };
    return labels[status];
  }

  getAvailabilityColor(status: Availability): string {
    const colors = {
      [Availability.AVAILABLE]: '#4caf50',
      [Availability.BUSY]: '#ff9800',
      [Availability.UNAVAILABLE]: '#f44336'
    };
    return colors[status];
  }

  onSubmit(): void {
    if (this.profileForm.invalid) {
      this.snackBar.open('Please fill in all required fields', 'Close', {
        duration: 3000,
        panelClass: ['error-snackbar']
      });
      return;
    }

    this.isLoading.set(true);

    // Prepare data for the API
    const profileData = {
      ...this.profileForm.value
    };

    // If profile already exists, update; otherwise create
    const apiCall = this.profileId() 
      ? this.profileService.updateMyProfile(profileData)
      : this.profileService.createProfile(profileData);

    apiCall.subscribe({
      next: (profile) => {
        console.log('Profile saved:', profile);
        this.profileId.set(profile.id);
        this.isLoading.set(false);
        this.snackBar.open('Professional profile updated successfully', 'Close', {
          duration: 3000,
          panelClass: ['success-snackbar']
        });
      },
      error: (error) => {
        console.error('Error saving profile:', error);
        this.isLoading.set(false);
        const message = error.error?.message || 'Error updating profile';
        this.snackBar.open(`${message}`, 'Close', {
          duration: 3000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }
}
