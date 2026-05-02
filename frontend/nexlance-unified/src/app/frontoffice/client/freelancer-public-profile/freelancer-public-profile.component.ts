import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FreelanceProfileService } from '@core/services/freelance-profile.service';
import { FreelanceProfile, Availability } from '@core/models/freelance-profile.model';
import { EvaluationService } from '@core/services/evaluation.service';
import { UserService } from '@core/services/user.service';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatCardModule } from '@angular/material/card';
import { ReputationBadgeComponent } from '../../../shared/components/reputation-badge/reputation-badge.component';

@Component({
  selector: 'app-freelancer-public-profile',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
    MatCardModule,
    ReputationBadgeComponent
  ],
  templateUrl: './freelancer-public-profile.component.html',
  styleUrls: ['./freelancer-public-profile.component.scss']
})
export class FreelancerPublicProfileComponent implements OnInit {
  profile: FreelanceProfile | null = null;
  loading = true;
  error = false;
  Availability = Availability;
  averageRating = 0;
  evaluationCount = 0;
  userEmail = '';
  userPhone = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private profileService: FreelanceProfileService,
    private evaluationService: EvaluationService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    const profileId = this.route.snapshot.paramMap.get('id');
    if (profileId) {
      this.loadProfile(profileId);
    } else {
      this.error = true;
      this.loading = false;
    }
  }

  loadProfile(profileId: string): void {
    this.loading = true;
    this.profileService.getProfileById(profileId).subscribe({
      next: (profile) => {
        this.profile = profile;
        this.loading = false;
        this.loadEvaluationData(profile);
        this.loadUserData(profile);
      },
      error: (err) => {
        if (err.status === 404) {
          // Might be a userId instead of profileId, try lookup by userId
          this.profileService.getProfileByUserId(profileId).subscribe({
            next: (profile) => {
              this.profile = profile;
              this.loading = false;
              this.loadEvaluationData(profile);
              this.loadUserData(profile);
            },
            error: () => {
              console.log('ℹ️ Freelance profile not found - user may not have created their profile yet');
              this.error = true;
              this.loading = false;
            }
          });
        } else {
          console.error('❌ Error loading profile:', err);
          this.error = true;
          this.loading = false;
        }
      }
    });
  }

  loadEvaluationData(profile: FreelanceProfile): void {
    const email = profile.email;
    if (email) {
      this.evaluationService.calculateUserAverageRating(email).subscribe({
        next: (avg) => this.averageRating = avg || 0,
        error: () => this.averageRating = 0
      });
      this.evaluationService.countUserEvaluations(email).subscribe({
        next: (count) => this.evaluationCount = count || 0,
        error: () => this.evaluationCount = 0
      });
    }
  }

  loadUserData(profile: FreelanceProfile): void {
    const userId = profile.userId;
    if (userId) {
      this.userService.getUserById(userId).subscribe({
        next: (user) => {
          this.userEmail = user.email || profile.email || '';
          this.userPhone = user.phoneNumber || '';
        },
        error: () => {
          this.userEmail = profile.email || '';
        }
      });
    } else {
      this.userEmail = profile.email || '';
    }
  }

  goBack(): void {
    this.router.navigate(['/frontoffice/client/freelancers']);
  }

  contactFreelancer(): void {
    // TODO: Implement messaging functionality
    console.log('Contact freelancer:', this.profile?.id);
  }
}
