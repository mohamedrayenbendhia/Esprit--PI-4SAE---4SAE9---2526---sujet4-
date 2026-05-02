import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule } from '@angular/forms';
import { RecommendationAiService, RecommendationResult, JobOfferProfile } from '@core/services/recommendation-ai.service';
import { JobOfferService } from '@core/services/job-offer.service';
import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-client-ai-recommendations',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    TranslateModule,
    MatIconModule,
    MatCardModule,
    MatButtonModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatSelectModule
  ],
  templateUrl: './ai-recommendations.component.html',
  styleUrls: ['./ai-recommendations.component.scss']
})
export class ClientAiRecommendationsComponent implements OnInit {
  recommendations: RecommendationResult[] = [];
  myJobOffers: any[] = [];
  selectedJobOfferId: number | null = null;
  isLoading = false;
  isLoadingJobs = true;
  isServiceOnline = false;
  serviceChecking = true;
  errorMessage = '';

  constructor(
    private aiService: RecommendationAiService,
    private jobOfferService: JobOfferService,
    private authService: AuthService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.checkServiceHealth();
    this.loadMyJobOffers();
  }

  checkServiceHealth(): void {
    this.serviceChecking = true;
    this.aiService.getHealth().subscribe({
      next: (health) => {
        this.isServiceOnline = health.status === 'UP';
        this.serviceChecking = false;
      },
      error: () => {
        this.isServiceOnline = false;
        this.serviceChecking = false;
      }
    });
  }

  loadMyJobOffers(): void {
    this.isLoadingJobs = true;
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.id) return;

    this.jobOfferService.getClientJobOffers(currentUser.id).subscribe({
      next: (jobs: any) => {
        this.myJobOffers = jobs.content || jobs || [];
        this.isLoadingJobs = false;
      },
      error: () => {
        this.isLoadingJobs = false;
      }
    });
  }

  searchFreelancers(): void {
    if (!this.selectedJobOfferId) {
      this.snackBar.open('Veuillez sélectionner une offre d\'emploi', 'Fermer', { duration: 3000 });
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.recommendations = [];

    this.aiService.getRecommendationsForJob(this.selectedJobOfferId, 10).subscribe({
      next: (response) => {
        this.recommendations = response.recommendations || [];
        this.isLoading = false;
        if (this.recommendations.length === 0) {
          this.errorMessage = 'Aucun freelancer correspondant trouvé. Assurez-vous que des profils sont synchronisés avec le service IA.';
        }
      },
      error: (err) => {
        console.error('Error loading AI recommendations:', err);
        this.isLoading = false;
        this.errorMessage = 'Erreur lors de la recherche IA de freelancers.';
      }
    });
  }

  getScorePercent(score: number): number {
    return Math.round(score * 100);
  }

  getScoreColor(score: number): string {
    if (score >= 0.8) return '#10B981';
    if (score >= 0.6) return '#F59E0B';
    if (score >= 0.4) return '#F97316';
    return '#EF4444';
  }

  getScoreLabel(score: number): string {
    if (score >= 0.8) return 'Excellent';
    if (score >= 0.6) return 'Bon';
    if (score >= 0.4) return 'Moyen';
    return 'Faible';
  }
}
