import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatListModule } from '@angular/material/list';
import { ProjectService } from '../../../../../core/services/project.service';
import { Project, ProjectMilestone } from '../../../../../core/models/project.model';
import { ProjectChatComponent } from '../../../../../shared/components/project-chat/project-chat.component';
import { AuthService } from '../../../../../core/services/auth.service';

@Component({
  selector: 'app-freelancer-project-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressBarModule,
    MatListModule,
    ProjectChatComponent
  ],
  templateUrl: './freelancer-project-dashboard.component.html',
  styleUrls: ['./freelancer-project-dashboard.component.scss']
})
export class FreelancerProjectDashboardComponent implements OnInit {
  project: Project | null = null;
  loading = true;
  currentUserId = '';
  currentUserName = '';

  constructor(
    private route: ActivatedRoute,
    private projectService: ProjectService,
    private authService: AuthService
  ) {
    const user = this.authService.getCurrentUser();
    this.currentUserId = user?.id || '';
    this.currentUserName = `${user?.firstName || ''} ${user?.lastName || ''}`.trim() || 'Freelancer';
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

  getMilestoneStatusColor(status: string): string {
    return status;
  }

  getDaysUntilDeadline(milestone: ProjectMilestone): number {
    const due = new Date(milestone.dueDate);
    const now = new Date();
    return Math.ceil((due.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
  }

  getCurrentMilestone(): ProjectMilestone | null {
    if (!this.project?.milestones) return null;
    
    const inProgress = this.project.milestones.find(m => m.status === 'IN_PROGRESS');
    if (inProgress) return inProgress;
    
    const pending = this.project.milestones.find(m => m.status === 'PENDING');
    return pending || null;
  }
}
