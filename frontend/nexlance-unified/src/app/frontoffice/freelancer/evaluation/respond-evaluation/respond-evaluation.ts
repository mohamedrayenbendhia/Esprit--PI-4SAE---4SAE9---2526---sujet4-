import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { EvaluationService, Evaluation } from '@core/services/evaluation.service';
import { UserService } from '@core/services/user.service';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, of } from 'rxjs';

@Component({
  selector: 'app-respond-evaluation',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './respond-evaluation.html',
  styleUrls: ['./respond-evaluation.css']
})
export class RespondEvaluationComponent implements OnInit {
  evaluationId: number = 0;
  freelancerEmail: string = '';
  evaluation: Evaluation | null = null;
  responseText: string = '';
  evaluatorEmail: string = '';

  isLoading: boolean = true;
  isSubmitting: boolean = false;
  errorMessage: string = '';
  successMessage: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private evaluationService: EvaluationService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.evaluationId = Number(idParam);
    }

    const userStr = localStorage.getItem('current_user');
    if (userStr) {
      try {
        const user = JSON.parse(userStr);
        this.freelancerEmail = user.email;
      } catch (e) {
        console.error('Error parsing user:', e);
      }
    }

    if (!this.freelancerEmail || !this.evaluationId) {
      this.errorMessage = 'Missing information';
      this.isLoading = false;
      return;
    }

    this.loadEvaluation();
  }

  loadEvaluation(): void {
    this.isLoading = true;

    this.evaluationService.getFreelancerEvaluationById(this.evaluationId, this.freelancerEmail).subscribe({
      next: (evaluation: Evaluation) => {
        this.evaluation = evaluation;
        this.responseText = evaluation.responseText || '';

        // Récupérer l'email du client via son ID
        this.userService.getUserById(evaluation.evaluatorId).pipe(
          catchError(() => of({ email: `Client #${evaluation.evaluatorId}` }))
        ).subscribe((user: any) => {
          this.evaluatorEmail = user.email ?? `Client #${evaluation.evaluatorId}`;
          this.isLoading = false;
        });
      },
      error: (error: HttpErrorResponse) => {
        if (error.status === 403) {
          this.errorMessage = 'You are not authorized to view this evaluation';
        } else if (error.status === 404) {
          this.errorMessage = 'Evaluation not found';
        } else {
          this.errorMessage = 'Error loading evaluation';
        }
        this.isLoading = false;
      }
    });
  }

  submitResponse(): void {
    if (!this.responseText.trim()) {
      this.errorMessage = 'Response cannot be empty';
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.evaluationService.respondToEvaluation(
      this.evaluationId,
      this.responseText,
      this.freelancerEmail
    ).subscribe({
      next: (updatedEvaluation: Evaluation) => {
        this.successMessage = 'Response sent successfully!';
        this.isSubmitting = false;

        setTimeout(() => {
          this.router.navigate(['/frontoffice/freelancer/evaluations']).catch(() => {
            this.router.navigate(['/frontoffice/freelancer/dashboard']);
          });
        }, 2000);
      },
      error: (error: HttpErrorResponse) => {
        if (error.status === 403) {
          this.errorMessage = 'You are not authorized to respond to this evaluation';
        } else {
          this.errorMessage = 'Error sending response';
        }
        this.isSubmitting = false;
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/frontoffice/freelancer/evaluations']).catch(() => {
      this.router.navigate(['/frontoffice/freelancer/dashboard']);
    });
  }
}