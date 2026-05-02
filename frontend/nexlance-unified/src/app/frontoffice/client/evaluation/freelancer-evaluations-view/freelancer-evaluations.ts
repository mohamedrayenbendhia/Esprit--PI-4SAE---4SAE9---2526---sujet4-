import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { EvaluationService, Evaluation } from '@core/services/evaluation.service';

@Component({
  selector: 'app-freelancer-evaluations-view',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './freelancer-evaluations.html',
  styleUrls: ['./freelancer-evaluations.css']
})
export class FreelancerEvaluationsViewComponent implements OnInit {
  evaluations: Evaluation[] = [];
  freelancerId: string = '';
  freelancerEmail: string = '';
  isLoading = true;
  errorMessage = '';

  constructor(
    private evaluationService: EvaluationService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // Get freelancer email from URL
    const emailParam = this.route.snapshot.paramMap.get('freelancerId');
    if (emailParam) {
      this.freelancerEmail = emailParam;
      this.loadEvaluations();
    }
  }

  loadEvaluations(): void {
    this.isLoading = true;
    this.evaluationService.getFreelancerEvaluations(this.freelancerEmail).subscribe({
      next: (data: Evaluation[]) => {
        this.evaluations = data;
        this.isLoading = false;
      },
      error: (error: any) => {
        console.error('Error loading evaluations:', error);
        this.errorMessage = 'Unable to load evaluations';
        this.isLoading = false;
      }
    });
  }

  getStars(rating: number): number[] {
    return Array(5).fill(0).map((_, i) => i + 1);
  }
}