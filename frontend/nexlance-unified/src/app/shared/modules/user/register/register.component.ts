import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../../../core/services/auth.service';
import { RegisterRequest, UserRole, UserType } from '../../../models/user.model';
import { LanguageSwitcherComponent } from '../../../components/language-switcher/language-switcher.component';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatCheckboxModule,
    MatTooltipModule,
    TranslateModule,
    LanguageSwitcherComponent
  ],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent implements OnInit {
  role: UserRole = UserRole.CLIENT;

  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private snackBar = inject(MatSnackBar);

  registerForm: FormGroup;
  hidePassword = true;
  hideConfirmPassword = true;
  isLoading = false;

  constructor() {
    this.registerForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phoneNumber: ['', [Validators.pattern(/^[+]?[(]?[0-9]{1,4}[)]?[-\s\.]?[(]?[0-9]{1,4}[)]?[-\s\.]?[0-9]{1,9}$/)]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]],
      acceptTerms: [false, [Validators.requiredTrue]]
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    // Get role from route data
    const routeRole = this.route.snapshot.data['role'];
    if (routeRole) {
      this.role = routeRole;
    }
  }

  passwordMatchValidator(form: FormGroup) {
    const password = form.get('password');
    const confirmPassword = form.get('confirmPassword');
    
    if (password && confirmPassword && password.value !== confirmPassword.value) {
      confirmPassword.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    }
    return null;
  }

  get roleTitle(): string {
    switch (this.role) {
      case UserRole.CLIENT:
        return 'Client';
      case UserRole.FREELANCER:
        return 'Freelancer';
      case UserRole.ADMIN:
        return 'Administrator';
      default:
        return '';
    }
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      return;
    }

    this.isLoading = true;
    
    // Map role to type (Backend expects uppercase)
    const type = this.role === UserRole.CLIENT 
      ? UserType.CLIENT
      : this.role === UserRole.FREELANCER 
        ? UserType.FREELANCE
        : UserType.ADMIN;
    
    const data: RegisterRequest = {
      ...this.registerForm.value,
      role: this.role,
      type: type
    };

    const registerObservable = this.role === UserRole.CLIENT
      ? this.authService.registerClient(data)
      : this.role === UserRole.FREELANCER
        ? this.authService.registerFreelancer(data)
        : this.authService.registerAdmin(data);

    registerObservable.subscribe({
      next: (response) => {
        console.log('✅ Registration successful:', response);
        this.snackBar.open(
          `Registration successful! Welcome ${response.firstName}!`,
          'Close',
          {
            duration: 2000,
            horizontalPosition: 'end',
            verticalPosition: 'top'
          }
        );
        
        // Redirect after successful registration
        this.isLoading = false;
        // Backend may return 'role' or 'type'
        const userRole = (response.role || response.type) as any;
        this.authService.redirectAfterLogin(userRole);
      },
      error: (error) => {
        console.error('❌ Registration error:', error);
        this.isLoading = false;
        const message = error.error?.message || 'Registration error. Please try again.';
        this.snackBar.open(message, 'Close', {
          duration: 5000,
          horizontalPosition: 'end',
          verticalPosition: 'top',
          panelClass: ['error-snackbar']
        });
      }
    });
  }
}
