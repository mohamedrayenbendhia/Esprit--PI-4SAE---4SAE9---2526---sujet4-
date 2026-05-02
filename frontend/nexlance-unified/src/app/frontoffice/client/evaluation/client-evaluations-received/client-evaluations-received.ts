import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { switchMap, catchError } from 'rxjs/operators';
import { EvaluationService, Evaluation } from '@core/services/evaluation.service';
import { UserService } from '@core/services/user.service';

@Component({
  selector: 'app-client-evaluations-received',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './client-evaluations-received.html',
  styleUrls: ['./client-evaluations-received.css']
})
export class ClientEvaluationsReceivedComponent implements OnInit {
  evaluations: Evaluation[] = [];
  displayedEvaluations: Evaluation[] = [];
  showAllEvaluations = false;
  clientEmail: string = '';
  isLoading: boolean = true;
  errorMessage: string = '';
  evaluatorNames: Map<string, string> = new Map();

  constructor(
    private evaluationService: EvaluationService,
    private userService: UserService,
    private router: Router
  ) {}

  ngOnInit() {
    const userStr = localStorage.getItem('current_user');

    if (userStr) {
      try {
        const user = JSON.parse(userStr);
        this.clientEmail = user.email;
      } catch (e) {
        console.error('Error parsing user:', e);
      }
    }

    if (!this.clientEmail) {
      this.errorMessage = 'User not logged in';
      this.isLoading = false;
      return;
    }

    this.loadEvaluations();
  }

  loadEvaluations() {
    this.isLoading = true;
    this.errorMessage = '';

    this.evaluationService.getFreelancerEvaluations(this.clientEmail).pipe(
      switchMap((data: Evaluation[]) => {
        this.evaluations = data;

        const uniqueIds = [...new Set(data.map(e => e.evaluatorId))];
        if (uniqueIds.length === 0) return of([]);

        return forkJoin(
          uniqueIds.map(id =>
            this.userService.getUserById(id).pipe(
              catchError(() => of({ id, firstName: 'Utilisateur', lastName: `#${id}` }))
            )
          )
        );
      })
    ).subscribe({
      next: (users: any[]) => {
        users.forEach(user => {
          const name = `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim();
          this.evaluatorNames.set(user.id, name || `Utilisateur #${user.id}`);
        });
        this.updateDisplayedEvaluations();
        this.isLoading = false;
      },
      error: (error: any) => {
        this.isLoading = false;
        console.error('Error loading evaluations:', error);
        this.errorMessage = 'Unable to load your evaluations.';
      }
    });
  }

  getEvaluatorName(evaluatorId: string): string {
    return this.evaluatorNames.get(evaluatorId) ?? `Utilisateur #${evaluatorId}`;
  }

  updateDisplayedEvaluations(): void {
    this.displayedEvaluations = this.showAllEvaluations
      ? this.evaluations
      : this.evaluations.slice(0, 5);
  }

  toggleShowAll(): void {
    this.showAllEvaluations = !this.showAllEvaluations;
    this.updateDisplayedEvaluations();
  }

  respondToEvaluation(id: number): void {
    this.router.navigate(['/frontoffice/client/evaluations/received']);
  }

  getStars(rating: number): number[] {
    return Array(5).fill(0).map((_, i) => i + 1);
  }

  getRatingPercent(rating: number): number {
    return (rating / 5) * 100;
  }
}