import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { PortfolioService } from '../../../../core/services/portfolio.service';
import { Portfolio, PortfolioRequest, PortfolioUpdateRequest, Project, ProjectRequest } from '../../../../shared/models/portfolio.model';
import { AuthService } from '../../../../core/services/auth.service';


@Component({
  selector: 'app-freelancer-portfolio',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSnackBarModule, MatTooltipModule, MatDividerModule
  ],
  templateUrl: './freelancer-portfolio.component.html',
  styleUrls: ['./freelancer-portfolio.component.scss']
})
export class FreelancerPortfolioComponent implements OnInit {
  private portfolioService = inject(PortfolioService);
  private snackBar = inject(MatSnackBar);
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
userFullName = signal<string>('');

  portfolio = signal<Portfolio | null>(null);
  projects = signal<Project[]>([]);
  loading = signal(false);

  showPortfolioForm = signal(false);
  showProjectForm = signal(false);
  editingProject = signal<Project | null>(null);

  readonly urlPattern = 'https?://.+';

portfolioForm: FormGroup = this.fb.group({
  headline: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
  location: ['', Validators.maxLength(100)],
  githubUrl: ['', Validators.pattern(this.urlPattern)],
  linkedinUrl: ['', Validators.pattern(this.urlPattern)]
});

projectForm: FormGroup = this.fb.group({
  title: ['', [Validators.required, Validators.minLength(3)]],
  description: ['', Validators.maxLength(500)],
  techStack: [''],
  startDate: [''],
  endDate: [''],
  githubUrl: ['', Validators.pattern(this.urlPattern)],
  demoUrl: ['', Validators.pattern(this.urlPattern)],
  images: ['', Validators.pattern(this.urlPattern)]
});

  ngOnInit(): void {
    this.loadPortfolio();
  }

  loadPortfolio(): void {
  this.loading.set(true);
  this.portfolioService.getMyPortfolio().subscribe({
    next: (p) => {
  this.portfolio.set(p);
  const user = this.authService.getCurrentUser();
  if (user) {
    this.userFullName.set(`${user.firstName} ${user.lastName}`);
  }
  this.loading.set(false);
  this.loadProjects(p.id);
},
    error: (err) => {
      this.loading.set(false);
      if (err.status === 404) {
        this.portfolio.set(null); // pas de portfolio → affiche bouton Create
      } else {
        this.snackBar.open('Error loading portfolio', 'Close', { duration: 3000 });
      }
    }
  });
}

  loadProjects(portfolioId: number): void {
    this.portfolioService.getProjects(portfolioId).subscribe({
      next: (projects) => this.projects.set(projects),
      error: () => this.snackBar.open('Error loading projects', 'Close', { duration: 3000 })
    });
  }

  openCreatePortfolio(): void {
    this.portfolioForm.reset();
    this.showPortfolioForm.set(true);
  }

  openEditPortfolio(): void {
    const p = this.portfolio();
    if (p) this.portfolioForm.patchValue(p);
    this.showPortfolioForm.set(true);
  }

  closePortfolioForm(): void {
    this.showPortfolioForm.set(false);
    this.portfolioForm.reset();
  }

  savePortfolio(): void {
    if (this.portfolioForm.invalid) return;
    const p = this.portfolio();

    if (p) {
      const request: PortfolioUpdateRequest = this.portfolioForm.value;
      this.portfolioService.updatePortfolio(p.id, request).subscribe({
        next: (updated) => {
          this.portfolio.set(updated);
          this.snackBar.open('Portfolio updated!', 'Close', { duration: 3000 });
          this.closePortfolioForm();
        },
        error: () => this.snackBar.open('Error updating portfolio', 'Close', { duration: 3000 })
      });
    } else {
      const request: PortfolioRequest = this.portfolioForm.value;
      this.portfolioService.createPortfolio(request).subscribe({
        next: (created) => {
          this.portfolio.set(created);
          this.snackBar.open('Portfolio created!', 'Close', { duration: 3000 });
          this.closePortfolioForm();
        },
        error: () => this.snackBar.open('Error creating portfolio', 'Close', { duration: 3000 })
      });
    }
  }

  openAddProject(): void {
    this.editingProject.set(null);
    this.projectForm.reset();
    this.showProjectForm.set(true);
  }

  openEditProject(project: Project): void {
    this.editingProject.set(project);
    this.projectForm.patchValue(project);
    this.showProjectForm.set(true);
  }

  closeProjectForm(): void {
    this.showProjectForm.set(false);
    this.editingProject.set(null);
    this.projectForm.reset();
  }

  saveProject(): void {
    if (this.projectForm.invalid) return;
    const request: ProjectRequest = this.projectForm.value;
    const p = this.portfolio();
    if (!p) return;

    const editing = this.editingProject();
    if (editing) {
      this.portfolioService.updateProject(editing.id, request).subscribe({
        next: () => {
          this.snackBar.open('Project updated!', 'Close', { duration: 3000 });
          this.closeProjectForm();
          this.loadProjects(p.id);
        },
        error: () => this.snackBar.open('Error updating project', 'Close', { duration: 3000 })
      });
    } else {
      this.portfolioService.addProject(p.id, request).subscribe({
        next: () => {
          this.snackBar.open('Project added!', 'Close', { duration: 3000 });
          this.closeProjectForm();
          this.loadProjects(p.id);
        },
        error: () => this.snackBar.open('Error adding project', 'Close', { duration: 3000 })
      });
    }
  }

  deleteProject(projectId: number): void {
    if (!confirm('Delete this project?')) return;
    const p = this.portfolio();
    if (!p) return;
    this.portfolioService.deleteProject(projectId).subscribe({
      next: () => {
        this.snackBar.open('Project deleted!', 'Close', { duration: 3000 });
        this.loadProjects(p.id);
      },
      error: () => this.snackBar.open('Error deleting project', 'Close', { duration: 3000 })
    });
  }

  toggleVisibility(): void {
  const p = this.portfolio();
  if (!p) return;
  this.portfolioService.toggleVisibility(p.id).subscribe({
    next: (updated) => {
      this.portfolio.set(updated);
      this.snackBar.open(
        updated.isPublic ? 'Portfolio is now Public!' : 'Portfolio is now Private!',
        'Close', { duration: 3000 }
      );
    },
    error: () => this.snackBar.open('Error updating visibility', 'Close', { duration: 3000 })
  });
}
}