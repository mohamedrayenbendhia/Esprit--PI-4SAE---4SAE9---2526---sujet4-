import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { switchMap, catchError } from 'rxjs/operators';
import { AdminService, Evaluation, ReportStatus } from '../../../../core/services/admin.service';
import { UserService } from '@core/services/user.service';

@Component({
  selector: 'app-reported-evaluations',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './reported-evaluations.html',
  styleUrls: ['./reported-evaluations.css']
})
export class ReportedEvaluationsComponent implements OnInit, OnDestroy {
  evaluations: Evaluation[] = [];
  evaluationsWithResponses: Evaluation[] = [];
  isLoading: boolean = true;
  errorMessage: string = '';
  successMessage: string = '';

  selectedEvaluation: Evaluation | null = null;
  showModerateDialog: boolean = false;
  showResponseDialog: boolean = false;
  moderationDecision: ReportStatus = ReportStatus.PENDING;
  activeTab: 'reported' | 'responses' = 'reported';

  approvedValue = ReportStatus.APPROVED;
  rejectedValue = ReportStatus.REJECTED;

  currentPage: number = 0;
  pageSize: number = 10;
  totalPages: number = 0;

  userEmails: Map<string, string> = new Map();

  private refreshInterval: any;

  constructor(
    private adminService: AdminService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.loadData();
    this.refreshInterval = setInterval(() => this.loadData(), 30000);
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) clearInterval(this.refreshInterval);
  }

  loadData(): void {
    this.loadReportedEvaluations();
    this.loadEvaluationsWithResponses();
  }

  private resolveUserEmails(evaluations: Evaluation[]): void {
    const allIds = new Set<string>();
    evaluations.forEach(e => {
      if (e.evaluatorId) allIds.add(e.evaluatorId);
      if (e.evaluatedId) allIds.add(e.evaluatedId);
    });

    const newIds = [...allIds].filter(id => !this.userEmails.has(id));
    if (newIds.length === 0) return;

    forkJoin(
      newIds.map(id =>
        this.userService.getUserById(id).pipe(
          catchError(() => of({ id, email: id }))  // ← plus de #
        )
      )
    ).subscribe((users: any[]) => {
      users.forEach(user => {
        this.userEmails.set(user.id, user.email ?? user.id);  // ← plus de #
      });
    });
  }

  getEmail(id: string): string {
    return this.userEmails.get(id) ?? id;  // ← plus de #
  }

  loadReportedEvaluations(): void {
    this.adminService.getReportedEvaluations().subscribe({
      next: (data: Evaluation[]) => {
        this.evaluations = data;
        this.updatePagination();
        this.resolveUserEmails(data);
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = 'Erreur chargement des signalements';
        this.isLoading = false;
      }
    });
  }

  loadEvaluationsWithResponses(): void {
    this.adminService.getEvaluationsWithResponses().subscribe({
      next: (data: Evaluation[]) => {
        this.evaluationsWithResponses = data;
        this.resolveUserEmails(data);
      },
      error: (error: HttpErrorResponse) => {
        console.error('Erreur chargement réponses:', error);
      }
    });
  }

  updatePagination(): void {
    this.totalPages = Math.ceil(this.evaluations.length / this.pageSize);
  }

  switchTab(tab: 'reported' | 'responses'): void {
    this.activeTab = tab;
    this.currentPage = 0;
  }

  openModerateDialog(evaluation: Evaluation): void {
    this.selectedEvaluation = evaluation;
    this.moderationDecision = ReportStatus.PENDING;
    this.showModerateDialog = true;
  }

  closeModerateDialog(): void {
    this.showModerateDialog = false;
    this.selectedEvaluation = null;
  }

  viewResponse(evaluation: Evaluation): void {
    this.selectedEvaluation = evaluation;
    this.showResponseDialog = true;
  }

  closeResponseDialog(): void {
    this.showResponseDialog = false;
    this.selectedEvaluation = null;
  }

  moderateEvaluation(): void {
    if (!this.selectedEvaluation?.id) return;

    this.adminService.moderateEvaluation(this.selectedEvaluation.id, this.moderationDecision).subscribe({
      next: (updated: Evaluation) => {
        const index = this.evaluations.findIndex(e => e.id === updated.id);
        if (index !== -1) this.evaluations[index] = updated;
        this.successMessage = `Évaluation ${this.moderationDecision === ReportStatus.APPROVED ? 'approuvée' : 'rejetée'}`;
        this.closeModerateDialog();
        this.loadData();
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: () => {
        this.errorMessage = 'Erreur lors de la modération';
        setTimeout(() => this.errorMessage = '', 3000);
      }
    });
  }

  getReasonLabel(reason: string): string {
    const labels: {[key: string]: string} = {
      'INAPPROPRIATE_CONTENT': 'Contenu inapproprié',
      'SPAM': 'Spam',
      'HARASSMENT': 'Harcèlement',
      'FALSE_INFORMATION': 'Fausse information',
      'OTHER': 'Autre'
    };
    return labels[reason] || reason;
  }

  getStatusClass(status: string): string {
    const classes: {[key: string]: string} = {
      'PENDING': 'status-pending',
      'APPROVED': 'status-approved',
      'REJECTED': 'status-rejected'
    };
    return classes[status] || '';
  }

  get paginatedEvaluations(): Evaluation[] {
    const start = this.currentPage * this.pageSize;
    return this.evaluations.slice(start, start + this.pageSize);
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) this.currentPage++;
  }

  prevPage(): void {
    if (this.currentPage > 0) this.currentPage--;
  }
}