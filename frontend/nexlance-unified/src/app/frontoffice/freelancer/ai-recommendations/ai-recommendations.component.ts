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
import { RecommendationAiService, RecommendationResult, AiHealthResponse } from '@core/services/recommendation-ai.service';
import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-freelancer-ai-recommendations',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    TranslateModule,
    MatIconModule,
    MatCardModule,
    MatButtonModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatSnackBarModule,
    MatTooltipModule
  ],
  templateUrl: './ai-recommendations.component.html',
  styleUrls: ['./ai-recommendations.component.scss']
})
export class FreelancerAiRecommendationsComponent implements OnInit {
  recommendations: RecommendationResult[] = [];
  isLoading = false;
  isServiceOnline = false;
  serviceChecking = true;
  healthInfo: AiHealthResponse | null = null;
  errorMessage = '';

  constructor(
    private aiService: RecommendationAiService,
    private authService: AuthService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.checkServiceHealth();
  }

  checkServiceHealth(): void {
    this.serviceChecking = true;
    this.aiService.getHealth().subscribe({
      next: (health) => {
        this.healthInfo = health;
        this.isServiceOnline = health.status === 'UP';
        this.serviceChecking = false;
        if (this.isServiceOnline) {
          this.loadRecommendations();
        }
      },
      error: () => {
        this.isServiceOnline = false;
        this.serviceChecking = false;
        this.errorMessage = 'Le service IA est actuellement indisponible. Veuillez réessayer plus tard.';
      }
    });
  }

  loadRecommendations(): void {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.id) {
      this.errorMessage = 'Utilisateur non connecté.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.aiService.getRecommendationsForFreelancer(Number(currentUser.id), 10).subscribe({
      next: (response) => {
        this.recommendations = response.recommended_jobs || [];
        this.isLoading = false;
        if (this.recommendations.length === 0) {
          this.errorMessage = 'Aucune recommandation IA disponible pour le moment. Assurez-vous que votre profil et des offres sont synchronisés.';
        }
      },
      error: (err) => {
        console.error('Error loading AI recommendations:', err);
        this.isLoading = false;
        this.errorMessage = 'Erreur lors du chargement des recommandations IA.';
        this.snackBar.open('Erreur lors du chargement des recommandations IA', 'Fermer', { duration: 3000 });
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

  refreshRecommendations(): void {
    this.checkServiceHealth();
  }
}
