import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { EvaluationService, Evaluation } from '@core/services/evaluation.service';

@Component({
  selector: 'app-evaluations-given',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './evaluations-given.html',
  styleUrls: ['./evaluations-given.css']
})
export class EvaluationsGivenComponent implements OnInit {
  evaluations: Evaluation[] = [];
  displayedEvaluations: Evaluation[] = [];
  showAllEvaluations = false;
  clientId: string = '';
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
        this.clientId = user.id;
        console.log('Client ID retrieved:', this.clientId);
      } catch (e) {
        console.error('Error parsing user:', e);
      }
    }

    if (!this.clientId) {
      this.errorMessage = 'User not logged in';
      this.isLoading = false;
      return;
    }

    this.loadEvaluations();
  }

  loadEvaluations() {
    this.isLoading = true;
    this.errorMessage = '';

    this.evaluationService.getMyGivenEvaluations(this.clientId).subscribe({
      next: (data: Evaluation[]) => {
        this.evaluations = data;
        this.updateDisplayedEvaluations();
        this.isLoading = false;
      },
      error: (error: any) => {
        this.isLoading = false;
        console.error('Error loading evaluations:', error);

        if (error.status === 0) {
          this.errorMessage = 'Backend server (port 8083) unreachable or CORS not configured.';
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
    if (!this.clientId) {
      this.router.navigate(['/login']);
      return;
    }
    
    const freelancerId = '123e4567-e89b-12d3-a456-426614174000';
    const projectId = 'project-123';
    
    this.router.navigate(['/frontoffice/client/evaluations/evaluate']);
  }

  deleteEvaluation(id: number) {
    if (confirm('Are you sure you want to delete this evaluation?')) {
      this.evaluationService.deleteMyEvaluation(id, this.clientId).subscribe({
        next: () => {
          this.evaluations = this.evaluations.filter(e => e.id !== id);
          this.updateDisplayedEvaluations();
        },
        error: (error: any) => {
          console.error('Error deleting evaluation:', error);
          alert('Unable to delete evaluation');
        }
      });
    }
  }

  getStars(rating: number): number[] {
    return Array(5).fill(0).map((_, i) => i + 1);
  }
}