import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { EvaluationService, Evaluation } from '@core/services/evaluation.service';

@Component({
  selector: 'app-freelancer-evaluations-given',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './freelancer-evaluations-given.html',
  styleUrls: ['./freelancer-evaluations-given.css']
})
export class FreelancerEvaluationsGivenComponent implements OnInit {
  evaluations: Evaluation[] = [];
  displayedEvaluations: Evaluation[] = [];
  showAllEvaluations = false;
  freelancerId: string = '';
  isLoading: boolean = true;
  errorMessage: string = '';

  constructor(
    private evaluationService: EvaluationService,
    private router: Router
  ) {}

  ngOnInit() {
    const userStr = localStorage.getItem('current_user');
    
    if (userStr) {
      try {
        const user = JSON.parse(userStr);
        this.freelancerId = user.id;
        console.log('📋 Freelancer ID retrieved:', this.freelancerId);
      } catch (e) {
        console.error('Error parsing user:', e);
      }
    }

    if (!this.freelancerId) {
      this.errorMessage = 'User not logged in';
      this.isLoading = false;
      return;
    }

    this.loadEvaluations();
  }

  loadEvaluations() {
    this.isLoading = true;
    this.errorMessage = '';

    // ✅ Récupérer les évaluations données par le freelancer (aux clients)
    this.evaluationService.getMyGivenEvaluationsAsFreelancer(this.freelancerId).subscribe({
      next: (data: Evaluation[]) => {
        console.log('✅ Évaluations données aux clients:', data.length);
        this.evaluations = data;
        this.updateDisplayedEvaluations();
        this.isLoading = false;
      },
      error: (error: any) => {
        this.isLoading = false;
        console.error('❌ Error loading evaluations:', error);

        if (error.status === 0) {
          this.errorMessage = 'Backend server unreachable or CORS not configured.';
        } else if (error.status === 404) {
          this.evaluations = [];
          this.updateDisplayedEvaluations();
        } else {
          this.errorMessage = `Error ${error.status}: Unable to load your evaluations.`;
        }
      }
    });
  }

  updateDisplayedEvaluations(): void {
    if (this.showAllEvaluations) {
      this.displayedEvaluations = this.evaluations;
    } else {
      this.displayedEvaluations = this.evaluations.slice(0, 5);
    }
  }

  toggleShowAll(): void {
    this.showAllEvaluations = !this.showAllEvaluations;
    this.updateDisplayedEvaluations();
  }

  navigateToCreateEvaluation(): void {
    if (!this.freelancerId) {
      this.router.navigate(['/login']);
      return;
    }
    
    // Rediriger vers le formulaire d'évaluation d'un client
    this.router.navigate(['/frontoffice/freelancer/evaluations/evaluate']);
  }

  getStars(rating: number): number[] {
    return Array(5).fill(0).map((_, i) => i + 1);
  }
}