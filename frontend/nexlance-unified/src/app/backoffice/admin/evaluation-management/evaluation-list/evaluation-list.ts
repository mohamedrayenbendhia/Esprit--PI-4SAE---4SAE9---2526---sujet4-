import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AdminService, Evaluation } from '../../../../core/services/admin.service';
import { UserService } from '@core/services/user.service';

@Component({
  selector: 'app-evaluation-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './evaluation-list.html',
  styleUrls: ['./evaluation-list.css']
})
export class EvaluationListComponent implements OnInit {
  evaluations: Evaluation[] = [];
  filteredEvaluations: Evaluation[] = [];
  isLoading: boolean = true;
  errorMessage: string = '';
  searchTerm: string = '';

  currentPage: number = 0;
  pageSize: number = 10;
  totalPages: number = 0;

  userEmails: Map<string, string> = new Map();

  constructor(
    private adminService: AdminService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.loadEvaluations();
  }

  loadEvaluations(): void {
    this.isLoading = true;
    this.adminService.getAllEvaluations().subscribe({
      next: (data: Evaluation[]) => {
        this.evaluations = data;
        this.filteredEvaluations = data;
        this.totalPages = Math.ceil(data.length / this.pageSize);
        this.resolveUserEmails(data);
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.isLoading = false;
        this.errorMessage = 'Erreur chargement évaluations';
      }
    });
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
          catchError(() => of({ id, email: id }))
        )
      )
    ).subscribe((users: any[]) => {
      users.forEach(user => {
        this.userEmails.set(user.id, user.email ?? user.id);
      });
    });
  }

  getEmail(id: string): string {
    return this.userEmails.get(id) ?? id;
  }

  onSearchChange(): void {
    if (!this.searchTerm || this.searchTerm.trim() === '') {
      this.filteredEvaluations = this.evaluations;
    } else {
      const term = this.searchTerm.toLowerCase().trim();
      this.filteredEvaluations = this.evaluations.filter(e =>
        e.evaluatorId?.toLowerCase().includes(term) ||
        e.evaluatedId?.toLowerCase().includes(term) ||
        e.comment?.toLowerCase().includes(term) ||
        e.responseText?.toLowerCase().includes(term)
      );
    }
    this.totalPages = Math.ceil(this.filteredEvaluations.length / this.pageSize);
    this.currentPage = 0;
  }

  search(): void {
    this.onSearchChange();
  }

  deleteEvaluation(id: number): void {
    if (confirm('Supprimer cette évaluation ?')) {
      this.adminService.deleteAnyEvaluation(id).subscribe({
        next: () => {
          this.evaluations = this.evaluations.filter(e => e.id !== id);
          this.filteredEvaluations = this.filteredEvaluations.filter(e => e.id !== id);
        },
        error: (error: HttpErrorResponse) => {
          alert('Erreur lors de la suppression');
        }
      });
    }
  }

  get paginatedEvaluations(): Evaluation[] {
    const start = this.currentPage * this.pageSize;
    return this.filteredEvaluations.slice(start, start + this.pageSize);
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) this.currentPage++;
  }

  prevPage(): void {
    if (this.currentPage > 0) this.currentPage--;
  }
}