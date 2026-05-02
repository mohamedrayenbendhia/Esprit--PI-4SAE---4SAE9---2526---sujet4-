import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSliderModule } from '@angular/material/slider';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { RecommendationModalComponent } from '../recommendation-modal/recommendation-modal.component';
import { RecommendationService } from '../../../core/services/recommendation.service';
import { InviteFreelancerModalComponent } from '../invite-freelancer-modal/invite-freelancer-modal.component';
import { FreelanceInvitationService } from '../../../core/services/freelance-invitation.service';
import { FreelanceProfileService } from '../../../core/services/freelance-profile.service';
import { FreelanceProfile, Availability } from '../../../core/models/freelance-profile.model';
import { EvaluationService } from '../../../core/services/evaluation.service';

interface Freelancer {
  id: string;
  userId: string;
  name: string;
  email: string;
  title: string;
  location: string;
  available: boolean;
  rating: number;
  reviewCount: number;
  hourlyRate: number;
  experience: number;
  successRate: number;
  skills: string[];
  availableIn?: string;
}

@Component({
  selector: 'app-search-freelancers',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatSliderModule,
    MatCheckboxModule,
    MatDialogModule,
    MatSnackBarModule,
    TranslateModule
  ],
  templateUrl: './search-freelancers.component.html',
  styleUrl: './search-freelancers.component.scss'
})
export class SearchFreelancersComponent implements OnInit {
  searchQuery = '';
  selectedSkills: string[] = [];
  availableSkills = ['React', 'Node.js', 'Python', 'Angular', 'Vue.js', 'MongoDB', 'AWS', 'Docker'];
  
  // Filters
  experienceFilter = 'all';
  experienceOptions = [
    { value: 'all', label: 'All' },
    { value: '0-2', label: '0-2 years' },
    { value: '3-5', label: '3-5 years' },
    { value: '5+', label: '5+ years' }
  ];
  
  minRate = 0;
  maxRate = 1000;
  minRating = 0;
  
  availabilityFilters = {
    immediate: true,
    twoWeeks: true,
    month: false
  };
  
  sortBy = 'rating';
  isLoading = false;
  
  // Real data from backend
  freelancers: Freelancer[] = [];
  
  filteredFreelancers: Freelancer[] = [];

  constructor(
    private router: Router,
    private dialog: MatDialog,
    private recommendationService: RecommendationService,
    private invitationService: FreelanceInvitationService,
    private freelanceProfileService: FreelanceProfileService,
    private evaluationService: EvaluationService,
    private snackBar: MatSnackBar,
    private translate: TranslateService
  ) {}

  ngOnInit() {
    this.loadFreelancers();
  }

  loadFreelancers() {
    this.isLoading = true;
    this.freelanceProfileService.getAllProfiles(0, 100).subscribe({
      next: (response) => {
        this.freelancers = response.content.map(profile => this.mapProfileToFreelancer(profile));
        // Build available skills from real data
        const allSkills = new Set<string>();
        this.freelancers.forEach(f => f.skills.forEach(s => allSkills.add(s)));
        this.availableSkills = Array.from(allSkills).sort();
        this.applyFilters();
        this.loadEvaluationRatings();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading freelancers:', error);
        this.snackBar.open('Error loading freelancers', 'OK', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });
        this.isLoading = false;
      }
    });
  }

  mapProfileToFreelancer(profile: FreelanceProfile): Freelancer {
    return {
      id: profile.id,
      userId: profile.userId,
      email: profile.email || '',
      name: profile.firstName && profile.lastName
        ? `${profile.firstName} ${profile.lastName}`
        : 'Freelancer',
      title: profile.title || 'Freelancer',
      location: profile.location || 'Not specified',
      available: profile.availability === Availability.AVAILABLE,
      availableIn: profile.availability === Availability.BUSY ? 'Busy' : undefined,
      rating: 0,
      reviewCount: 0,
      hourlyRate: profile.hourlyRate || 0,
      experience: profile.experienceYears || 0,
      successRate: profile.completionRate || 0,
      skills: profile.skills || []
    };
  }

  loadEvaluationRatings(): void {
    this.evaluationService.getAllFreelancersOverview().subscribe({
      next: (overview: any) => {
        // Backend returns a map: { "email@example.com": { averageRating, totalEvaluations, ... } }
        if (overview && typeof overview === 'object') {
          Object.keys(overview).forEach((email: string) => {
            const data = overview[email];
            const freelancer = this.freelancers.find(f => f.email === email);
            if (freelancer && data) {
              freelancer.rating = data.averageRating || 0;
              freelancer.reviewCount = data.totalEvaluations || 0;
            }
          });
          this.applyFilters();
        }
      },
      error: (err: any) => {
        if (err.status !== 503) {
          console.warn('Could not load evaluation ratings:', err);
        }
      }
    });
  }

  get filteredSkills(): string[] {
    if (!this.searchQuery) return this.availableSkills;
    const q = this.searchQuery.toLowerCase();
    return this.availableSkills.filter(s => s.toLowerCase().includes(q));
  }

  onSkillSearch(event: Event) {
    const input = event.target as HTMLInputElement;
    this.searchQuery = input.value;
  }

  toggleSkill(skill: string) {
    const index = this.selectedSkills.indexOf(skill);
    if (index > -1) {
      this.selectedSkills.splice(index, 1);
    } else {
      this.selectedSkills.push(skill);
    }
    this.applyFilters();
  }

  removeSkill(skill: string) {
    const index = this.selectedSkills.indexOf(skill);
    if (index > -1) {
      this.selectedSkills.splice(index, 1);
    }
    this.applyFilters();
  }

  applyFilters() {
    this.filteredFreelancers = this.freelancers.filter(freelancer => {
      // Experience filter
      if (this.experienceFilter !== 'all') {
        const [min, max] = this.experienceFilter.includes('+') 
          ? [5, Infinity] 
          : this.experienceFilter.split('-').map(Number);
        if (freelancer.experience < min || (max && freelancer.experience > max)) {
          return false;
        }
      }

      // Rate filter
      if (freelancer.hourlyRate < this.minRate || freelancer.hourlyRate > this.maxRate) {
        return false;
      }

      // Rating filter
      if (freelancer.rating < this.minRating) {
        return false;
      }

      // Skills filter
      if (this.selectedSkills.length > 0) {
        const hasAllSkills = this.selectedSkills.every(skill => 
          freelancer.skills.some(s => s.toLowerCase().includes(skill.toLowerCase()))
        );
        if (!hasAllSkills) {
          return false;
        }
      }

      return true;
    });

    this.sortFreelancers();
  }

  resetFilters() {
    this.selectedSkills = [];
    this.experienceFilter = 'all';
    this.minRate = 0;
    this.maxRate = 1000;
    this.minRating = 0;
    this.availabilityFilters = {
      immediate: true,
      twoWeeks: true,
      month: false
    };
    this.applyFilters();
  }

  sortFreelancers() {
    this.filteredFreelancers.sort((a, b) => {
      switch (this.sortBy) {
        case 'rating':
          return b.rating - a.rating;
        case 'rate':
          return a.hourlyRate - b.hourlyRate;
        case 'experience':
          return b.experience - a.experience;
        default:
          return 0;
      }
    });
  }

  onSortChange(event: Event) {
    const select = event.target as HTMLSelectElement;
    this.sortBy = select.value;
    this.sortFreelancers();
  }

  viewProfile(freelancerId: string) {
    const freelancer = this.freelancers.find(f => f.id === freelancerId);
    if (freelancer) {
      this.router.navigate(['/frontoffice/client/freelancers', freelancer.id]);
    }
  }

  openRecommendationModal(freelancerId: string) {
    const freelancer = this.freelancers.find(f => f.id === freelancerId);
    if (!freelancer) return;

    const dialogRef = this.dialog.open(RecommendationModalComponent, {
      width: '650px',
      maxWidth: '95vw',
      data: { freelancer }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        // Submit recommendation
        this.recommendationService.createRecommendation(result).subscribe({
          next: (response) => {
            console.log('Recommendation created successfully:', response);
            this.snackBar.open('Recommendation sent successfully!', 'OK', {
              duration: 5000,
              panelClass: ['success-snackbar']
            });
          },
          error: (error) => {
            console.error('Error creating recommendation:', error);
            if (error.status === 409) {
              this.snackBar.open('You have already recommended this freelancer for this job offer', 'OK', {
                duration: 5000,
                panelClass: ['error-snackbar']
              });
            } else if (error.status === 503) {
              this.snackBar.open('Recommendation service is currently unavailable. Please try again later.', 'OK', {
                duration: 5000,
                panelClass: ['error-snackbar']
              });
            } else {
              this.snackBar.open('Error sending recommendation', 'OK', {
                duration: 5000,
                panelClass: ['error-snackbar']
              });
            }
          }
        });
      }
    });
  }

  openInviteModal(freelancerId: string) {
    const freelancer = this.freelancers.find(f => f.id === freelancerId);
    if (!freelancer) return;

    const dialogRef = this.dialog.open(InviteFreelancerModalComponent, {
      width: '700px',
      maxWidth: '95vw',
      data: { freelancer }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        console.log('📤 Dialog result (invitation data):', result);
        // Submit invitation
        this.invitationService.createInvitation(result).subscribe({
          next: (response) => {
            console.log('✅ Invitation sent successfully:', response);
            this.translate.get('invite.successMessage').subscribe((msg: string) => {
              this.snackBar.open(msg, 'OK', {
                duration: 5000,
                panelClass: ['success-snackbar']
              });
            });
          },
          error: (error) => {
            console.error('❌ Error sending invitation:', error);
            if (error.error) {
              console.error('Error details:', error.error);
            }
            this.translate.get('invite.errorMessage').subscribe((msg: string) => {
              this.snackBar.open(msg, 'OK', {
                duration: 5000,
                panelClass: ['error-snackbar']
              });
            });
          }
        });
      }
    });
  }

  getStars(rating: number): number[] {
    return Array(5).fill(0).map((_, i) => i < Math.floor(rating) ? 1 : 0);
  }
}
