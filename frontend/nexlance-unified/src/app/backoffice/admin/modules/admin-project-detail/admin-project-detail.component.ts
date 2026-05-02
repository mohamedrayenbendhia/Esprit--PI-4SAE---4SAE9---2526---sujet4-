import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDividerModule } from '@angular/material/divider';
import { MatBadgeModule } from '@angular/material/badge';
import { TranslateModule } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';
import { ProjectService } from '../../../../core/services/project.service';
import { UserService } from '../../../../core/services/user.service';
import { Project, ProjectMilestone, MilestoneStatus, ProjectStatus } from '../../../../core/models/project.model';
import { User } from '../../../../shared/models/user.model';

@Component({
  selector: 'app-admin-project-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    TranslateModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTableModule,
    MatProgressBarModule,
    MatTooltipModule,
    MatTabsModule,
    MatDividerModule,
    MatBadgeModule
  ],
  templateUrl: './admin-project-detail.component.html',
  styleUrls: ['./admin-project-detail.component.scss']
})
export class AdminProjectDetailComponent implements OnInit {
  readonly ProjectStatus = ProjectStatus;
  project: Project | null = null;
  client: User | null = null;
  freelancer: User | null = null;
  loading = true;
  displayedColumns: string[] = ['order', 'title', 'status', 'dueDate', 'submittedAt', 'revisions', 'actions'];

  constructor(
    private route: ActivatedRoute,
    private projectService: ProjectService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    const projectId = this.route.snapshot.paramMap.get('id');
    if (projectId) {
      this.loadProject(projectId);
    }
  }

  loadProject(id: string): void {
    this.loading = true;
    this.projectService.getProjectById(id).subscribe({
      next: (project) => {
        this.project = project;
        this.loadUsersInfo(project);
      },
      error: (error) => {
        console.error('Error loading project:', error);
        this.loading = false;
      }
    });
  }

  loadUsersInfo(project: Project): void {
    if (!project.clientId || !project.freelanceId) {
      this.loading = false;
      return;
    }

    forkJoin({
      client: this.userService.getUserById(project.clientId),
      freelancer: this.userService.getUserById(project.freelanceId)
    }).subscribe({
      next: ({ client, freelancer }) => {
        this.client = client;
        this.freelancer = freelancer;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading user info:', error);
        this.loading = false;
      }
    });
  }

  getMilestoneCountByStatus(status: string): number {
    return this.project?.milestones?.filter(m => m.status === status).length || 0;
  }

  getApprovedCount(): number {
    return this.getMilestoneCountByStatus('APPROVED');
  }

  getPendingValidationCount(): number {
    return this.getMilestoneCountByStatus('SUBMITTED');
  }

  getMilestoneRevisionCount(milestone: ProjectMilestone): number {
    return milestone.status === 'REJECTED' ? 1 : 0;
  }

  getDaysSinceSubmission(milestone: ProjectMilestone): number | null {
    if (!milestone.submittedAt) return null;
    
    const submitted = new Date(milestone.submittedAt);
    const now = new Date();
    return Math.floor((now.getTime() - submitted.getTime()) / (1000 * 60 * 60 * 24));
  }

  getMilestoneStatusColor(status: string): string {
    const colors: any = {
      'PENDING': 'default',
      'IN_PROGRESS': 'primary',
      'SUBMITTED': 'accent',
      'APPROVED': 'success',
      'REJECTED': 'warn'
    };
    return colors[status] || 'default';
  }

  getMilestoneStatusIcon(status: string): string {
    const icons: any = {
      'PENDING': 'hourglass_empty',
      'IN_PROGRESS': 'autorenew',
      'SUBMITTED': 'upload_file',
      'APPROVED': 'check_circle',
      'REJECTED': 'cancel'
    };
    return icons[status] || 'help';
  }

  getProjectStatusIcon(status: string): string {
    const icons: any = {
      'ACTIVE': 'play_circle',
      'ON_HOLD': 'pause_circle',
      'COMPLETED': 'check_circle',
      'CANCELLED': 'cancel'
    };
    return icons[status] || 'help';
  }

  getProjectStatusClass(status: string): string {
    return 'status-' + (status || 'ACTIVE').toLowerCase();
  }

  isOverdue(milestone: ProjectMilestone): boolean {
    if (!milestone.dueDate || milestone.status === 'APPROVED') return false;
    return new Date(milestone.dueDate) < new Date();
  }

  getProjectDuration(): number {
    if (!this.project?.startDate || !this.project?.endDate) return 0;
    const start = new Date(this.project.startDate);
    const end = new Date(this.project.endDate);
    return Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
  }

  getElapsedDays(): number {
    if (!this.project?.startDate) return 0;
    const start = new Date(this.project.startDate);
    const now = new Date();
    const elapsed = Math.floor((now.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
    return Math.max(0, elapsed);
  }

  getRemainingDays(): number {
    if (!this.project?.endDate) return 0;
    const end = new Date(this.project.endDate);
    const now = new Date();
    const remaining = Math.ceil((end.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
    return Math.max(0, remaining);
  }

  getTimelinePercentage(): number {
    const total = this.getProjectDuration();
    if (total <= 0) return 0;
    const elapsed = this.getElapsedDays();
    return Math.min(100, Math.round((elapsed / total) * 100));
  }

  formatDate(date: Date | string | undefined): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  updateProjectStatus(status: ProjectStatus): void {
    if (!this.project?.id) return;
    this.projectService.updateProjectStatus(this.project.id, status).subscribe({
      next: (updated) => {
        this.project = updated;
      },
      error: (error) => {
        console.error('Error updating status:', error);
      }
    });
  }
}
