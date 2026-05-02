import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { ProjectService } from '../../../../core/services/project.service';
import { Project, ProjectStatus } from '../../../../core/models/project.model';

@Component({
  selector: 'app-admin-projects',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    TranslateModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressBarModule,
    MatTooltipModule,
    MatCardModule,
    MatCheckboxModule
  ],
  templateUrl: './admin-projects.component.html',
  styleUrls: ['./admin-projects.component.scss']
})
export class AdminProjectsComponent implements OnInit {
  projects: Project[] = [];
  filteredProjects: Project[] = [];
  displayedColumns: string[] = ['client', 'freelancer', 'milestones', 'progress', 'status', 'alerts', 'actions'];
  
  searchTerm: string = '';
  statusFilter: ProjectStatus | 'ALL' = 'ALL';
  alertFilter: boolean = false;
  
  ProjectStatus = ProjectStatus;
  loading = true;

  constructor(private projectService: ProjectService) {}

  ngOnInit(): void {
    this.loadProjects();
  }

  loadProjects(): void {
    this.loading = true;
    this.projectService.getAllProjects().subscribe({
      next: (projects) => {
        this.projects = projects;
        this.applyFilters();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading projects:', error);
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    this.filteredProjects = this.projects.filter(project => {
      const matchesSearch = !this.searchTerm || 
        project.id?.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        project.clientId?.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        project.freelanceId?.toLowerCase().includes(this.searchTerm.toLowerCase());
      
      const matchesStatus = this.statusFilter === 'ALL' || project.status === this.statusFilter;
      
      const matchesAlert = !this.alertFilter || this.hasAlerts(project);
      
      return matchesSearch && matchesStatus && matchesAlert;
    });
  }

  hasAlerts(project: Project): boolean {
    if (!project.milestones) return false;
    
    const now = new Date();
    return project.milestones.some(milestone => {
      if (milestone.status === 'SUBMITTED' && milestone.submittedAt) {
        const submittedDate = new Date(milestone.submittedAt);
        const daysSinceSubmission = Math.floor((now.getTime() - submittedDate.getTime()) / (1000 * 60 * 60 * 24));
        return daysSinceSubmission > 7;
      }
      return false;
    });
  }

  getMilestonesCounts(project: Project): string {
    if (!project.milestones || project.milestones.length === 0) {
      return '0 / 0';
    }
    const approved = project.milestones.filter(m => m.status === 'APPROVED').length;
    return `${approved} / ${project.milestones.length}`;
  }

  getStatusColor(status: ProjectStatus): string {
    switch (status) {
      case ProjectStatus.ACTIVE:
        return 'primary';
      case ProjectStatus.ON_HOLD:
        return 'warn';
      case ProjectStatus.COMPLETED:
        return 'accent';
      case ProjectStatus.CANCELLED:
        return '';
      default:
        return '';
    }
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.statusFilter = 'ALL';
    this.alertFilter = false;
    this.applyFilters();
  }
}
