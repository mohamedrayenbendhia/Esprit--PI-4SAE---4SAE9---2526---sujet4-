import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { FreelanceProfileService } from '../../../../core/services/freelance-profile.service';
import { UserService } from '../../../../core/services/user.service';
import { AuthService } from '../../../../core/services/auth.service';
import { FreelanceProfile, Availability } from '../../../../core/models/freelance-profile.model';
import { User } from '../../../../shared/models/user.model';
import { UserKycComponent } from '../../../../shared/components/user-kyc/user-kyc.component';

@Component({
  selector: 'app-freelancer-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, UserKycComponent],
  templateUrl: './freelancer-profile.component.html',
  styleUrls: ['./freelancer-profile.component.scss']
})
export class FreelancerProfileComponent implements OnInit {
  private fb = inject(FormBuilder);
  private profileService = inject(FreelanceProfileService);
  private userService = inject(UserService);
  private authService = inject(AuthService);
  private router = inject(Router);

  user = signal<User | null>(null);
  profile = signal<FreelanceProfile | null>(null);
  loading = signal(false);
  activeTab = signal<string>('general');

  generalForm!: FormGroup;
  profileForm!: FormGroup;

  Availability = Availability;

  ngOnInit(): void {
    // Check authentication first
    if (!this.authService.isAuthenticated()) {
      console.error('User not authenticated! Redirecting to login...');
      alert('You must be logged in to access this page. Please login first.');
      this.router.navigate(['/login']);
      return;
    }

    const token = this.authService.getToken();
    console.log('Token exists:', !!token);
    console.log('Token preview:', token ? token.substring(0, 20) + '...' : 'null');

    this.initForms();
    this.loadUserData();
    this.loadProfile();
  }

  initForms(): void {
    this.generalForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phoneNumber: ['']
    });

    this.profileForm = this.fb.group({
      title: ['', Validators.required],
      bio: ['', [Validators.required, Validators.minLength(100)]],
      hourlyRate: [0, [Validators.required, Validators.min(0)]],
      availability: [Availability.AVAILABLE, Validators.required],
      experienceYears: [0, [Validators.required, Validators.min(0)]],
      location: ['', Validators.required],
      languagesInput: [''],
      timezone: ['UTC', Validators.required]
    });
  }

  loadUserData(): void {
    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        this.user.set(user);
        this.generalForm.patchValue({
          firstName: user.firstName,
          lastName: user.lastName,
          email: user.email,
          phoneNumber: user.phoneNumber
        });
      },
      error: (err) => console.error('Error loading user:', err)
    });
  }

  loadProfile(): void {
    this.loading.set(true);
    this.profileService.getMyProfile().subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.profileForm.patchValue({
          ...profile,
          languagesInput: profile.languages?.join(', ') || ''
        });
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading profile:', err);
        this.loading.set(false);
      }
    });
  }

  setActiveTab(tab: string): void {
    this.activeTab.set(tab);
  }

  onUpdateGeneral(): void {
    if (this.generalForm.valid) {
      this.loading.set(true);
      this.userService.updateCurrentUser(this.generalForm.value).subscribe({
        next: () => {
          alert('General information updated successfully');
          this.loading.set(false);
        },
        error: (err) => {
          console.error('Error updating:', err);
          alert('Failed to update information');
          this.loading.set(false);
        }
      });
    }
  }

  onUpdateProfile(): void {
    if (this.profileForm.valid) {
      this.loading.set(true);
      const formValue = this.profileForm.value;
      
      // Convert comma-separated languages to array
      const languages = formValue.languagesInput
        ? formValue.languagesInput.split(',').map((lang: string) => lang.trim()).filter((lang: string) => lang)
        : [];

      const profileData = {
        ...formValue,
        languages,
        languagesInput: undefined // Remove this field from submission
      };

      delete profileData.languagesInput;

      this.profileService.updateMyProfile(profileData).subscribe({
        next: (profile) => {
          this.profile.set(profile);
          alert('Professional profile updated successfully');
          this.loading.set(false);
        },
        error: (err) => {
          console.error('Error updating profile:', err);
          alert('Failed to update profile');
          this.loading.set(false);
        }
      });
    }
  }

  onAvatarChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      
      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        alert('File size must be less than 5MB');
        return;
      }
      
      // Validate file type
      const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif'];
      if (!allowedTypes.includes(file.type)) {
        alert('Only JPG, PNG, and GIF images are allowed');
        return;
      }
      
      this.loading.set(true);
      this.userService.uploadAvatar(file).subscribe({
        next: (updatedUser) => {
          this.user.set(updatedUser);
          alert('Avatar updated successfully');
          this.loading.set(false);
        },
        error: (err) => {
          console.error('Error uploading avatar:', err);
          alert('Failed to upload avatar: ' + (err.error?.message || err.message || 'Unknown error'));
          this.loading.set(false);
        }
      });
    }
  }

  updateAvailability(availability: Availability): void {
    this.profileService.updateAvailability(availability).subscribe({
      next: (profile) => {
        this.profile.set(profile);
        alert('Availability updated successfully');
      },
      error: (err) => {
        console.error('Error updating availability:', err);
        alert('Failed to update availability');
      }
    });
  }
}
