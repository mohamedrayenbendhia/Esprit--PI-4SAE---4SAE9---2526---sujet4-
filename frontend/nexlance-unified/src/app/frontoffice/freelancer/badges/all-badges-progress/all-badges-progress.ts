import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { BadgeService, Badge, UserBadge } from '@core/services/badge.service';
import { EvaluationService } from '@core/services/evaluation.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-all-badges-progress',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './all-badges-progress.html',
  styleUrls: ['./all-badges-progress.css']
})
export class AllBadgesProgressComponent implements OnInit {
  badges: Badge[] = [];
  userBadges: UserBadge[] = [];
  freelancerEmail: string = '';
  
  currentScore: number = 0;
  currentProjects: number = 0;
  
  isLoading: boolean = true;
  errorMessage: string = '';

  constructor(
    private badgeService: BadgeService,
    private evaluationService: EvaluationService
  ) {}

  ngOnInit(): void {
  const userStr = localStorage.getItem('current_user');
if (userStr) {
  try {
    const user = JSON.parse(userStr);
    this.freelancerEmail = user.email.toLowerCase(); // ✅ Normalisation
  } catch (e) {
    console.error('Error parsing user:', e);
  }
}


    if (!this.freelancerEmail) {
      this.errorMessage = 'Email not found';
      this.isLoading = false;
      return;
    }

    this.loadData();
  }

  loadData(): void {
    this.isLoading = true;

    this.badgeService.getAllBadges().subscribe({
      next: (badgesData: Badge[]) => {
        this.badges = badgesData;
        
        this.badgeService.getFreelancerBadges(this.freelancerEmail).subscribe({
          next: (userBadgesData: UserBadge[]) => {
            this.userBadges = userBadgesData;
            
            this.evaluationService.calculateUserAverageRating(this.freelancerEmail).subscribe({
              next: (avgScore: number) => {
                this.currentScore = avgScore || 0;
                
                this.evaluationService.countUserEvaluations(this.freelancerEmail).subscribe({
                  next: (count: number) => {
                    this.currentProjects = count || 0;
                    this.isLoading = false;
                  },
                  error: (error: HttpErrorResponse) => {
                    this.isLoading = false;
                    this.errorMessage = 'Error loading projects';
                    console.error('Error:', error);
                  }
                });
              },
              error: (error: HttpErrorResponse) => {
                this.isLoading = false;
                this.errorMessage = 'Error loading score';
                console.error('Error:', error);
              }
            });
          },
          error: (error: HttpErrorResponse) => {
            this.isLoading = false;
            this.errorMessage = 'Error loading your badges';
            console.error('Error:', error);
          }
        });
      },
      error: (error: HttpErrorResponse) => {
        this.isLoading = false;
        this.errorMessage = 'Error loading badges';
        console.error('Error:', error);
      }
    });
  }

  isBadgeObtained(badgeId: number): boolean {
    return this.userBadges.some(ub => ub.badge.id === badgeId);
  }

  getScoreProgress(badge: Badge): number {
    if (badge.minScore <= 0) return 0;
    return Math.min(100, (this.currentScore / badge.minScore) * 100);
  }

  getProjectsProgress(badge: Badge): number {
    if (badge.minProjects <= 0) return 0;
    return Math.min(100, (this.currentProjects / badge.minProjects) * 100);
  }

  getOverallProgress(badge: Badge): number {
    const scoreProgress = this.getScoreProgress(badge);
    const projectsProgress = this.getProjectsProgress(badge);
    return (scoreProgress + projectsProgress) / 2;
  }

  getProgressColor(progress: number): string {
    if (progress >= 100) return '#28a745';
    if (progress >= 70) return '#17a2b8';
    if (progress >= 40) return '#ffc107';
    if (progress >= 10) return '#fd7e14';
    return '#dc3545';
  }

  getBadgeIcon(badgeName: string): string {
    const icons: {[key: string]: string} = {
      'Expert': '🏆',
      'Confirmed': '🥈',
      'Beginner': '🌱',
      'TOP 1%': '👑',
      'Fast': '⚡',
      'Communication': '💬',
      'Quality': '✨'
    };
    return icons[badgeName] || '🎖️';
  }

  getStatusText(badge: Badge): string {
    if (this.isBadgeObtained(badge.id)) {
      return 'Obtained';
    }
    
    const scoreProgress = this.getScoreProgress(badge);
    const projectsProgress = this.getProjectsProgress(badge);
    
    if (scoreProgress >= 100 && projectsProgress >= 100) {
      return 'Ready to claim';
    } else if (scoreProgress >= 100) {
      return 'Need more projects';
    } else if (projectsProgress >= 100) {
      return 'Need higher score';
    }
    return 'In progress';
  }

  getStatusClass(badge: Badge): string {
    if (this.isBadgeObtained(badge.id)) return 'obtained';
    
    const scoreProgress = this.getScoreProgress(badge);
    const projectsProgress = this.getProjectsProgress(badge);
    
    if (scoreProgress >= 100 && projectsProgress >= 100) {
      return 'ready';
    } else if (scoreProgress >= 100 || projectsProgress >= 100) {
      return 'almost';
    }
    return 'in-progress';
  }
}