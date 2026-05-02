import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../../../core/services/auth.service';
import { UserService } from '../../../../core/services/user.service';
import { User, UserRole } from '../../../models/user.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatTabsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    TranslateModule
  ],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private userService = inject(UserService);
  private snackBar = inject(MatSnackBar);
  private router = inject(Router);

  currentUser = signal<User | null>(null);
  profileForm!: FormGroup;
  isLoading = signal(false);
  selectedFile: File | null = null;
  previewUrl: string | null = null;

  UserRole = UserRole;

  ngOnInit(): void {
    this.loadUserProfile();
    this.initForm();
  }

  loadUserProfile(): void {
    // Load from backend API
    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        this.currentUser.set(user);
        this.patchFormValues(user);
        // Also update localStorage via AuthService
        this.authService.updateUserSession(user);
      },
      error: (error) => {
        console.error('Error loading profile:', error);
        // Fallback to localStorage data
        const user = this.authService.getCurrentUser();
        if (user) {
          this.currentUser.set(user);
          this.patchFormValues(user);
        }
      }
    });
  }

  initForm(): void {
    this.profileForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phoneNumber: ['', [Validators.pattern(/^[+]?[(]?[0-9]{1,4}[)]?[-\s\.]?[(]?[0-9]{1,4}[)]?[-\s\.]?[0-9]{1,9}$/)]]
    });
  }

  patchFormValues(user: User): void {
    this.profileForm.patchValue({
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      phoneNumber: user.phoneNumber || ''
    });
    
    if (user.avatar) {
      this.previewUrl = user.avatar;
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      this.selectedFile = input.files[0];
      
      // Preview
      const reader = new FileReader();
      reader.onload = (e: ProgressEvent<FileReader>) => {
        this.previewUrl = e.target?.result as string;
      };
      reader.readAsDataURL(this.selectedFile);
    }
  }

  removePhoto(): void {
    this.selectedFile = null;
    this.previewUrl = null;
  }

  onSubmit(): void {
    if (this.profileForm.invalid) {
      return;
    }

    this.isLoading.set(true);
    
    // Prepare user data (without avatar)
    const userData = {
      firstName: this.profileForm.value.firstName,
      lastName: this.profileForm.value.lastName,
      phoneNumber: this.profileForm.value.phoneNumber || ''
    };

      // If avatar file is selected, upload it separately
    if (this.selectedFile) {
      this.userService.uploadAvatar(this.selectedFile).subscribe({
        next: (user) => {
          // Avatar uploaded, now update other info
          this.updateUserInfo(userData);
        },
        error: (error) => {
          console.error('Error uploading avatar:', error);
          this.isLoading.set(false);
          this.snackBar.open('Error uploading photo', 'Close', {
            duration: 3000,
            panelClass: ['error-snackbar']
          });
        }
      });
    } else {
      // No avatar, just update info
      this.updateUserInfo(userData);
    }
  }

  private updateUserInfo(userData: any): void {
    this.userService.updateCurrentUser(userData).subscribe({
      next: (user) => {
        this.isLoading.set(false);
        this.currentUser.set(user);
        // Update localStorage
        this.authService.updateUserSession(user);
        this.snackBar.open('Profile updated successfully', 'Close', {
          duration: 3000,
          panelClass: ['success-snackbar']
        });
        this.selectedFile = null;
      },
      error: (error) => {
        console.error('Error updating profile:', error);
        this.isLoading.set(false);
        this.snackBar.open('Error updating profile', 'Close', {
          duration: 3000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  get isFreelancer(): boolean {
    return this.currentUser()?.role === UserRole.FREELANCER;
  }

  get isClient(): boolean {
    return this.currentUser()?.role === UserRole.CLIENT;
  }

  get isAdmin(): boolean {
    return this.currentUser()?.role === UserRole.ADMIN;
  }
}
