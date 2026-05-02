import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ProjectService } from '../../../../../core/services/project.service';
import { Project, ProjectMilestone } from '../../../../../core/models/project.model';
import { ProjectChatComponent } from '../../../../../shared/components/project-chat/project-chat.component';
import { AuthService } from '../../../../../core/services/auth.service';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-client-project-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    TranslateModule,
    ProjectChatComponent,
    MatIconModule
  ],
  templateUrl: './client-project-dashboard.component.html',
  styleUrls: ['./client-project-dashboard.component.scss']
})
export class ClientProjectDashboardComponent implements OnInit {
  project: Project | null = null;
  loading = true;
  currentUserId = '';
  currentUserName = '';

  constructor(
    private route: ActivatedRoute,
    private projectService: ProjectService,
    private translate: TranslateService,
    private authService: AuthService
  ) {
    const user = this.authService.getCurrentUser();
    this.currentUserId = user?.id || '';
    this.currentUserName = `${user?.firstName || ''} ${user?.lastName || ''}`.trim() || 'Client';
  }

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
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading project:', error);
        this.loading = false;
      }
    });
  }

  getPendingValidationCount(): number {
    return this.project?.milestones?.filter(m => m.status === 'SUBMITTED').length || 0;
  }

  getApprovedCount(): number {
    return this.project?.milestones?.filter(m => m.status === 'APPROVED').length || 0;
  }

  getMilestoneCountByStatus(status: string): number {
    return this.project?.milestones?.filter(m => m.status === status).length || 0;
  }

  getDaysUntilDue(milestone: ProjectMilestone): number {
    const due = new Date(milestone.dueDate);
    const now = new Date();
    return Math.ceil((due.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
  }

  getDeadlineClass(milestone: ProjectMilestone): string {
    if (milestone.status === 'APPROVED') return 'deadline-approved';
    const days = this.getDaysUntilDue(milestone);
    if (days < 0) return 'deadline-overdue';
    if (days <= 3) return 'deadline-soon';
    return 'deadline-ok';
  }

  getDaysLabel(milestone: ProjectMilestone): string {
    if (milestone.status === 'APPROVED') return '';
    const days = this.getDaysUntilDue(milestone);
    if (days < 0) return this.translate.instant('clientProjects.dashboard.daysLate', { days: Math.abs(days) });
    if (days === 0) return this.translate.instant('clientProjects.dashboard.today');
    if (days === 1) return this.translate.instant('clientProjects.dashboard.oneDayRemaining');
    return this.translate.instant('clientProjects.dashboard.daysRemainingCount', { days: days });
  }

  getMilestoneStatusIconEmoji(status: string): string {
    const icons: Record<string, string> = {
      'PENDING': 'schedule',
      'IN_PROGRESS': 'autorenew',
      'SUBMITTED': 'upload_file',
      'APPROVED': 'check_circle',
      'REJECTED': 'cancel'
    };
    return icons[status] || 'help_outline';
  }

  getMilestoneStatusIcon(status: string): string {
    const icons: any = {
      'PENDING': 'schedule',
      'IN_PROGRESS': 'work',
      'SUBMITTED': 'send',
      'APPROVED': 'check_circle',
      'REJECTED': 'cancel'
    };
    return icons[status] || 'help';
  }
}
