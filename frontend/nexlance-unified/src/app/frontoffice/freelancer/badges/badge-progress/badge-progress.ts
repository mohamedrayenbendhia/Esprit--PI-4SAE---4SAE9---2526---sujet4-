import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { BadgeService, Badge, UserBadge } from '@core/services/badge.service';
import { EvaluationService } from '@core/services/evaluation.service';

@Component({
  selector: 'app-badge-progress',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './badge-progress.html',
  styleUrls: ['./badge-progress.css']
})
export class BadgeProgressComponent implements OnInit {
  badgeId: number = 0;
  badge: Badge | null = null;
  freelancerEmail: string = '';

  currentScore: number = 0;
  currentProjects: number = 0;
  scoreProgress: number = 0;
  projectsProgress: number = 0;
  overallProgress: number = 0;
  isObtained: boolean = false;

  isLoading: boolean = true;
  errorMessage: string = '';

  constructor(
    private route: ActivatedRoute,
    private badgeService: BadgeService,
    private evaluationService: EvaluationService
  ) {}

  ngOnInit(): void {
    const badgeIdParam = this.route.snapshot.paramMap.get('badgeId');
    if (badgeIdParam) {
      this.badgeId = Number(badgeIdParam);
    }

    const userStr = localStorage.getItem('current_user');
if (userStr) {
  try {
    const user = JSON.parse(userStr);
    this.freelancerEmail = user.email.toLowerCase(); // ✅ Normalisation
  } catch (e) {
    console.error('Error parsing user:', e);
  }
}


    if (!this.freelancerEmail || !this.badgeId) {
      this.errorMessage = 'Missing information';
      this.isLoading = false;
      return;
    }

    this.loadProgress();
  }

  loadProgress(): void {
    this.isLoading = true;

    // Récupérer le score et le nombre de projets
    this.evaluationService.calculateUserAverageRating(this.freelancerEmail).subscribe({
      next: (avgScore: number) => {
        this.currentScore = avgScore || 0;
        
        this.evaluationService.countUserEvaluations(this.freelancerEmail).subscribe({
          next: (count: number) => {
            this.currentProjects = count || 0;
            
            // ✅ Forcer la vérification des badges avec les données actuelles
            this.checkAndAssignBadges();
          },
          error: (error) => {
            console.error('Error loading projects:', error);
            this.loadBadgeProgress();
          }
        });
      },
      error: (error) => {
        console.error('Error loading score:', error);
        this.loadBadgeProgress();
      }
    });
  }

  // ✅ Méthode pour vérifier et assigner les badges
  checkAndAssignBadges(): void {
    // Appeler l'endpoint de vérification (à ajouter dans le service)
    this.badgeService.checkAndAssignBadges(
      this.freelancerEmail,
      this.currentScore,
      this.currentProjects
    ).subscribe({
      next: () => {
        console.log('✅ Badges vérifiés et assignés');
        // Après vérification, charger la progression
        this.loadBadgeProgress();
      },
      error: (error) => {
        console.error('Error checking badges:', error);
        this.loadBadgeProgress();
      }
    });
  }

  loadBadgeProgress(): void {
    this.badgeService.getBadgeProgress(this.freelancerEmail, this.badgeId).subscribe({
      next: (data: any) => {
        console.log('✅ Badge progress data:', data);
        this.badge = data.badge;
        this.currentScore = data.currentScore || 0;
        this.currentProjects = data.currentProjects || 0;
        this.scoreProgress = data.scoreProgress || 0;
        this.projectsProgress = data.projectsProgress || 0;
        this.overallProgress = data.overallProgress || 0;
        this.isObtained = data.isObtained || false;
        this.isLoading = false;
        
        // ✅ Si le badge vient d'être obtenu, on le voit ici !
        if (this.isObtained) {
          console.log('🎉 Félicitations ! Badge obtenu !');
        }
      },
      error: (error: any) => {
        this.isLoading = false;
        this.errorMessage = 'Error loading badge progress';
        console.error('Error:', error);
      }
    });
  }

  getProgressColor(progress: number): string {
    if (progress >= 100) return '#28a745';
    if (progress >= 70) return '#17a2b8';
    if (progress >= 40) return '#ffc107';
    if (progress >= 10) return '#fd7e14';
    return '#dc3545';
  }
}