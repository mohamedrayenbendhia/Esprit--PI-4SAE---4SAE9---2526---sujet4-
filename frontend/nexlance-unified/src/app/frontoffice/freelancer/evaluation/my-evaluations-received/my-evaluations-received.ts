import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { switchMap, catchError } from 'rxjs/operators';
import { EvaluationService, Evaluation } from '@core/services/evaluation.service';
import { UserService } from '@core/services/user.service';

@Component({
  selector: 'app-my-evaluations-received',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './my-evaluations-received.html',
  styleUrls: ['./my-evaluations-received.css']
})
export class MyEvaluationsReceivedComponent implements OnInit {
  evaluations: Evaluation[] = [];
  freelancerEmail: string = '';
  isLoading: boolean = true;
  errorMessage: string = '';
  evaluatorEmails: Map<string, string> = new Map();

  constructor(
    private evaluationService: EvaluationService,
    private userService: UserService
  ) {}

  ngOnInit() {
    const userStr = localStorage.getItem('current_user');

    if (userStr) {
      try {
        const user = JSON.parse(userStr);
        this.freelancerEmail = user.email;
      } catch (e) {
        console.error('Error parsing user:', e);
      }
    }

    if (!this.freelancerEmail) {
      this.errorMessage = 'Email not found - Please check that the user has an email';
      this.isLoading = false;
      return;
    }

    this.loadEvaluations();
  }

  loadEvaluations() {
    this.isLoading = true;
    this.errorMessage = '';

    this.evaluationService.getFreelancerEvaluations(this.freelancerEmail).pipe(
      switchMap((data: Evaluation[]) => {
        this.evaluations = data;

        const uniqueIds = [...new Set(data.map(e => e.evaluatorId))];
        if (uniqueIds.length === 0) return of([]);

        return forkJoin(
          uniqueIds.map(id =>
            this.userService.getUserById(id).pipe(
              catchError(() => of({ id, email: `user-${id}` }))
            )
          )
        );
      })
    ).subscribe({
      next: (users: any[]) => {
        users.forEach(user => {
          this.evaluatorEmails.set(user.id, user.email ?? `user-${user.id}`);
        });
        this.isLoading = false;
      },
      error: (error: any) => {
        this.isLoading = false;
        console.error('Error loading evaluations:', error);
        this.errorMessage = 'Unable to load your evaluations';
      }
    });
  }

  getEvaluatorEmail(evaluatorId: string): string {
    return this.evaluatorEmails.get(evaluatorId) ?? `Client #${evaluatorId}`;
  }

  getStars(rating: number): number[] {
    return Array(5).fill(0).map((_, i) => i + 1);
  }

  getRatingPercent(rating: number): number {
    return (rating / 5) * 100;
  }
}